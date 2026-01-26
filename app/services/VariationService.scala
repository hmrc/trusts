/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import errors._
import models.auditing.TrustAuditing
import models.get_trust.{GetTrustResponse, TrustProcessedResponse}
import models.tax_enrolments.{TaxEnrolmentNotProcessed, TaxEnrolmentSubscriberResponse}
import models.variation.{DeclarationForApi, VariationContext}
import play.api.Logging
import play.api.libs.json._
import services.auditing.VariationAuditService
import services.dates.LocalDateService
import transformers.DeclarationTransformer
import uk.gov.hmrc.http.HeaderCarrier
import utils.JsonOps._
import utils.TrustEnvelope.TrustEnvelope
import utils.{Session, TrustEnvelope}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VariationService @Inject() (
  trustsService: TrustsService,
  transformationService: TransformationService,
  declarationTransformer: DeclarationTransformer,
  auditService: VariationAuditService,
  localDateService: LocalDateService,
  taxableMigrationService: TaxableMigrationService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val className = this.getClass.getSimpleName

  def submitDeclaration(identifier: String, internalId: String, sessionId: String, declaration: DeclarationForApi)(
    implicit hc: HeaderCarrier
  ): TrustEnvelope[VariationContext] = EitherT {

    getCachedTrustData(identifier, internalId, sessionId).value.flatMap {
      case Left(trustErrors)                               => Future.successful(Left(trustErrors))
      case Right(originalResponse: TrustProcessedResponse) =>
        transformationService.populateLeadTrusteeAddress(originalResponse.getTrust) match {
          case JsSuccess(originalJson, _) =>
            transformationService.applyDeclarationTransformations(identifier, internalId, originalJson).value.flatMap {
              case Right(JsSuccess(transformedJson, _)) =>
                val response = TrustProcessedResponse(transformedJson, originalResponse.responseHeader)
                transformAndSubmit(identifier, internalId, sessionId, declaration, originalJson, response).value
              case Right(e @ JsError(errors))           =>

                auditService.auditTransformationError(
                  internalId,
                  identifier,
                  originalJson,
                  JsString("Declaration Transforms"),
                  "Failed to apply declaration transformations.",
                  JsError.toJson(e)
                )

                logger.error(
                  s"[$className][submitDeclaration][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] " +
                    s"Failed to transform trust info ${JsError.toJson(errors)}"
                )

                Future.successful(
                  Left(
                    VariationFailureForAudit(
                      InternalServerErrorResponse,
                      "There was a problem transforming data for submission to ETMP"
                    )
                  )
                )

              case Left(trustErrors) => Future.successful(Left(trustErrors))
            }
          case e @ JsError(errors)        =>

            auditService.auditTransformationError(
              internalId,
              identifier,
              originalResponse.getTrust,
              JsString("Populate lead trustees transforms"),
              "Failed to populate lead trustee address",
              JsError.toJson(e)
            )

            logger.error(
              s"[$className][submitDeclaration][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] " +
                s"Failed to populate lead trustee address ${JsError.toJson(errors)}"
            )

            Future.successful(
              Left(
                VariationFailureForAudit(
                  InternalServerErrorResponse,
                  "There was a problem transforming data for submission to ETMP"
                )
              )
            )

        }
    }
  }

  private def transformAndSubmit(
    identifier: String,
    internalId: String,
    sessionId: String,
    declaration: DeclarationForApi,
    originalJson: JsValue,
    response: TrustProcessedResponse
  )(implicit hc: HeaderCarrier): TrustEnvelope[VariationContext] = EitherT {

    declarationTransformer.transform(response, originalJson, declaration, localDateService.now) match {
      case JsSuccess(value, _) =>
        logger.info(
          s"[$className][transformAndSubmit][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] " +
            s"successfully transformed json for declaration"
        )
        submitVariationAndCheckForMigration(identifier, value, internalId, sessionId).value
      case JsError(errors)     =>

        auditService.auditTransformationError(
          internalId = internalId,
          utr = identifier,
          transforms = JsString("Declaration transforms"),
          data = originalJson,
          errorReason = "Problem transforming data for ETMP submission"
        )

        logger.error(
          s"[$className][transformAndSubmit][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] " +
            s"Problem transforming data for ETMP submission: ${JsError.toJson(errors)}"
        )

        Future.successful(
          Left(
            VariationFailureForAudit(
              InternalServerErrorResponse,
              s"There was a problem transforming data for submission to ETMP: ${JsError.toJson(errors)}"
            )
          )
        )
    }
  }

  private def getCachedTrustData(
    identifier: String,
    internalId: String,
    sessionId: String
  ): TrustEnvelope[TrustProcessedResponse] = EitherT {

    def processGetTrustResponse(response: GetTrustResponse, fbn: String): Either[TrustErrors, TrustProcessedResponse] =
      response match {
        case tpr: TrustProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
          logger.info(
            s"[$className][processGetTrustResponse][Session ID: $sessionId][UTR/URN: $identifier] " +
              s"returning TrustProcessedResponse"
          )
          Right(response.asInstanceOf[TrustProcessedResponse])

        case _: TrustProcessedResponse =>
          logger.warn(
            s"[$className][processGetTrustResponse][Session ID: $sessionId][UTR/URN: $identifier] " +
              s"ETMP cached data in mongo has become stale, rejecting submission"
          )
          Left(VariationFailureForAudit(EtmpCacheDataStaleErrorResponse, "Etmp data is stale"))

        case _ =>
          logger.warn(
            s"[$className][processGetTrustResponse][Session ID: $sessionId][UTR/URN: $identifier] " +
              s"Trust was not in a processed state"
          )
          Left(
            VariationFailureForAudit(
              InternalServerErrorResponse,
              "Submission could not proceed, Trust data was not in a processed state"
            )
          )
      }

    val result = for {
      response          <- trustsService.getTrustInfo(identifier, internalId, sessionId)
      formBundleNo      <- trustsService.getTrustInfoFormBundleNo(identifier)
      processedResponse <- TrustEnvelope(processGetTrustResponse(response, formBundleNo))
    } yield processedResponse

    result.value.map {
      case Right(processedResponse)                       => Right(processedResponse)
      case Left(ServerError(message)) if message.nonEmpty =>
        Left(VariationFailureForAudit(InternalServerErrorResponse, message))
      case Left(error)                                    => Left(error)
    }

  }

  private def submitVariationAndCheckForMigration(
    identifier: String,
    value: JsValue,
    internalId: String,
    sessionId: String
  )(implicit hc: HeaderCarrier): TrustEnvelope[VariationContext] = {

    def migrateNonTaxableToTaxable(
      migrateToTaxable: Boolean,
      subscriptionId: String,
      identifier: String
    ): TrustEnvelope[TaxEnrolmentSubscriberResponse] = EitherT {
      if (migrateToTaxable) {
        logger.info(
          s"[$className][migrateNonTaxableToTaxable][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] " +
            s"trust has migrated to taxable, preparing tax-enrolments to be allocated the UTR"
        )
        taxableMigrationService.migrateSubscriberToTaxable(subscriptionId, identifier).value
      } else {
        logger.info(
          s"[$className][migrateNonTaxableToTaxable][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] " +
            s"trust did not require a migration to taxable"
        )
        Future.successful(Right(TaxEnrolmentNotProcessed))
      }
    }

    for {
      migrateToTaxable  <- taxableMigrationService.migratingFromNonTaxableToTaxable(identifier, internalId, sessionId)
      variationResponse <- submitVariation(identifier, value, internalId, migrateToTaxable)
      _                 <- migrateNonTaxableToTaxable(migrateToTaxable, variationResponse.result.tvn, identifier)
    } yield variationResponse
  }

  private def submitVariation(
    identifier: String,
    value: JsValue,
    internalId: String,
    migrateToTaxable: Boolean
  )(implicit hc: HeaderCarrier): TrustEnvelope[VariationContext] = EitherT {

    val payload = value.applyRules

    auditService.audit(TrustAuditing.TRUST_VARIATION_ATTEMPT, payload, internalId, Json.toJson(Json.obj()))

    trustsService.trustVariation(payload).value.map {
      case Right(response) =>
        logger.info(
          s"[$className][submitVariation][Session ID: ${Session.id(hc)}][UTR/URN: $identifier] trust submitted a variation"
        )

        auditService.auditVariationSuccess(internalId, migrateToTaxable, payload, response)
        Right(VariationContext(payload, response))

      case Left(trustErrors) => Left(trustErrors)
    }
  }

}
