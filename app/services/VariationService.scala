/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import exceptions.{EtmpCacheDataStaleException, InternalServerErrorException}
import models.variation.{DeclarationForApi, VariationContext}
import models.auditing.TrustAuditing
import models.get_trust.TrustProcessedResponse
import play.api.Logging
import play.api.libs.json._
import transformers.DeclarationTransformer
import uk.gov.hmrc.http.HeaderCarrier
import utils.JsonOps._
import utils.Session

import javax.inject.Inject
import models.tax_enrolments.{TaxEnrolmentNotProcessed, TaxEnrolmentSubscriberResponse}
import services.auditing.VariationAuditService
import services.dates.LocalDateService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationService @Inject()(
                                  trustsService: TrustsService,
                                  transformationService: TransformationService,
                                  declarationTransformer: DeclarationTransformer,
                                  auditService: VariationAuditService,
                                  localDateService: LocalDateService,
                                  taxableMigrationService: TaxableMigrationService
                                ) extends Logging {

  private case class LoggingContext(identifier: String)(implicit hc: HeaderCarrier) {
    def info(content: String): Unit = logger.info(format(content))
    def error(content: String): Unit = logger.error(format(content))
    def warn(content: String): Unit = logger.warn(format(content))

    def format(content: String): String = s"[submitDeclaration][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] $content"
  }

  def submitDeclaration(identifier: String, internalId: String, declaration: DeclarationForApi)
                       (implicit hc: HeaderCarrier): Future[VariationContext] = {

    implicit val logging: LoggingContext = LoggingContext(identifier)

    getCachedTrustData(identifier, internalId).flatMap { originalResponse: TrustProcessedResponse =>
      transformationService.populateLeadTrusteeAddress(originalResponse.getTrust) match {
        case JsSuccess(originalJson, _) =>
          transformationService.applyDeclarationTransformations(identifier, internalId, originalJson).flatMap {
            case JsSuccess(transformedJson, _) =>
              val response = TrustProcessedResponse(transformedJson, originalResponse.responseHeader)
              transformAndSubmit(identifier, internalId, declaration, originalJson, response)
            case e@JsError(errors) =>

              auditService.auditTransformationError(
                internalId,
                identifier,
                originalJson,
                JsString("Declaration Transforms"),
                "Failed to apply declaration transformations.",
                JsError.toJson(e)
              )

              logging.error(s"Failed to transform trust info ${JsError.toJson(errors)}")
              Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
          }
        case e@JsError(errors) =>

          auditService.auditTransformationError(
            internalId,
            identifier,
            originalResponse.getTrust,
            JsString("Populate lead trustees transforms"),
            "Failed to populate lead trustee address",
            JsError.toJson(e)
          )

          logging.error(s"Failed to populate lead trustee address ${JsError.toJson(errors)}")
          Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
      }
    }
  }

  private def transformAndSubmit(identifier: String,
                                 internalId: String,
                                 declaration: DeclarationForApi,
                                 originalJson: JsValue,
                                 response: TrustProcessedResponse)
                                (implicit hc: HeaderCarrier, logging: LoggingContext): Future[VariationContext] = {
        logger.debug(s"[Session ID: ${Session.id(hc)}]" +
          s" transformation to final submission, applying declaration transform to shape data into variations payload")

        declarationTransformer.transform(
          response,
          originalJson,
          declaration,
          localDateService.now
        ) match {
          case JsSuccess(value, _) =>
            logging.info("successfully transformed json for declaration")
            submitVariationAndCheckForMigration(identifier, value, internalId)
          case JsError(errors) =>

            auditService.auditTransformationError(
              internalId = internalId,
              utr = identifier,
              transforms = JsString("Declaration transforms"),
              data = originalJson,
              errorReason = "Problem transforming data for ETMP submission"
            )

            logging.error(s"Problem transforming data for ETMP submission: ${JsError.toJson(errors)}")
            Future.failed(InternalServerErrorException(s"There was a problem transforming data for submission to ETMP: ${JsError.toJson(errors)}"))
    }
  }

  private def getCachedTrustData(identifier: String, internalId: String)(implicit logging: LoggingContext): Future[TrustProcessedResponse] = {
    for {
      response <- trustsService.getTrustInfo(identifier, internalId)
      fbn <- trustsService.getTrustInfoFormBundleNo(identifier)
    } yield response match {
      case tpr: TrustProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
        logging.info("returning TrustProcessedResponse")
        response.asInstanceOf[TrustProcessedResponse]
      case _: TrustProcessedResponse =>
        logging.info("ETMP cached data in mongo has become stale, rejecting submission")
        throw EtmpCacheDataStaleException
      case _ =>
        logging.warn("Trust was not in a processed state")
        throw InternalServerErrorException("Submission could not proceed, Trust data was not in a processed state")
    }
  }

  private def submitVariationAndCheckForMigration(identifier: String, value: JsValue, internalId: String)
                                                 (implicit hc: HeaderCarrier): Future[VariationContext] = {

    def migrateNonTaxableToTaxable(migrateToTaxable: Boolean, subscriptionId: String, identifier: String): Future[TaxEnrolmentSubscriberResponse] = {
      if (migrateToTaxable) {
        logger.info(s"[Session ID: ${Session.id(hc)}][UTR/URN: $identifier] trust has migrated to taxable, preparing tax-enrolments to be allocated the UTR")
        taxableMigrationService.migrateSubscriberToTaxable(subscriptionId, identifier)
      } else {
        logger.info(s"[Session ID: ${Session.id(hc)}][UTR/URN: $identifier] trust did not require a migration to taxable")
        Future.successful(TaxEnrolmentNotProcessed)
      }
    }

    for {
      migrateToTaxable <- taxableMigrationService.migratingFromNonTaxableToTaxable(identifier, internalId)
      variationResponse <- submitVariation(identifier, value, internalId, migrateToTaxable)
      _ <- migrateNonTaxableToTaxable(migrateToTaxable, variationResponse.result.tvn, identifier)
    } yield {
      variationResponse
    }
  }

  private def submitVariation(
                               identifier: String,
                               value: JsValue,
                               internalId: String,
                               migrateToTaxable: Boolean
                             )
                             (implicit hc: HeaderCarrier): Future[VariationContext] = {

    val payload = value.applyRules

    auditService.audit(
      TrustAuditing.TRUST_VARIATION_ATTEMPT,
      payload,
      internalId,
      Json.toJson(Json.obj())
    )

    trustsService.trustVariation(payload) map { response =>

      logger.info(s"[Session ID: ${Session.id(hc)}][UTR/URN: $identifier] trust submitted a variation")

      auditService.auditVariationSuccess(
        internalId,
        migrateToTaxable,
        payload,
        response
      )

      VariationContext(payload, response)
    }
  }

}
