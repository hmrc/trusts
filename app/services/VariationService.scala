/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import models.DeclarationForApi
import models.auditing.TrustAuditing
import models.get_trust.TrustProcessedResponse
import models.variation.VariationResponse
import play.api.Logging
import play.api.libs.json._
import transformers.DeclarationTransformer
import uk.gov.hmrc.http.HeaderCarrier
import utils.JsonOps._
import utils.Session

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationService @Inject()(desService: DesService,
                                 transformationService: TransformationService,
                                 declarationTransformer: DeclarationTransformer,
                                 auditService: AuditService,
                                 localDateService: LocalDateService,
                                 trustsStoreService: TrustsStoreService) extends Logging {

  private case class LoggingContext(utr: String)(implicit hc: HeaderCarrier) {
    def info(content: String): Unit = logger.info(format(content))
    def error(content: String): Unit = logger.error(format(content))
    def warn(content: String): Unit = logger.warn(format(content))

    def format(content: String): String = s"[submitDeclaration][Session ID: ${Session.id(hc)}][UTR: $utr] $content"
  }

  def submitDeclaration(utr: String, internalId: String, declaration: DeclarationForApi)
                       (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    implicit val logging: LoggingContext = LoggingContext(utr)

    getCachedTrustData(utr, internalId).flatMap { originalResponse: TrustProcessedResponse =>
      transformationService.populateLeadTrusteeAddress(originalResponse.getTrust) match {
        case JsSuccess(originalJson, _) =>
          transformationService.applyDeclarationTransformations(utr, internalId, originalJson).flatMap {
            case JsSuccess(transformedJson, _) =>
              val response = TrustProcessedResponse(transformedJson, originalResponse.responseHeader)
              transformAndSubmit(utr, internalId, declaration, originalJson, response)
            case JsError(errors) =>
              logging.error(s"Failed to transform trust info $errors")
              Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
          }
        case JsError(errors) =>
          logging.error(s"Failed to populate lead trustee address $errors")
          Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
      }
   }
  }

  private def transformAndSubmit(utr: String,
                                 internalId: String,
                                 declaration: DeclarationForApi,
                                 originalJson: JsValue,
                                 response: TrustProcessedResponse)
                                (implicit hc: HeaderCarrier, logging: LoggingContext)= {
    trustsStoreService.is5mldEnabled() flatMap {
      is5mld =>
        declarationTransformer.transform(response, originalJson, declaration, localDateService.now, is5mld) match {
          case JsSuccess(value, _) =>
            logging.info("successfully transformed json for declaration")
            doSubmit(utr, value, internalId)
          case JsError(errors) =>
            logging.error(s"Problem transforming data for ETMP submission ${errors.toString()}")
            Future.failed(InternalServerErrorException(s"There was a problem transforming data for submission to ETMP: ${errors.toString()}"))
        }
    } recoverWith {
      case e =>
        logging.error(s"Exception transforming and submitting ${e.getMessage}")
        Future.failed(InternalServerErrorException(s"Exception transforming and submitting ${e.getMessage}"))
    }
  }

  private def getCachedTrustData(utr: String, internalId: String)(implicit logging: LoggingContext): Future[TrustProcessedResponse] = {
    for {
      response <- desService.getTrustInfo(utr, internalId)
      fbn <- desService.getTrustInfoFormBundleNo(utr)
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

  private def doSubmit(utr: String, value: JsValue, internalId: String)
                      (implicit hc: HeaderCarrier, logging: LoggingContext): Future[VariationResponse] = {

    val payload = value.applyRules

    auditService.audit(
      TrustAuditing.TRUST_VARIATION_ATTEMPT,
      payload,
      internalId,
      Json.toJson(Json.obj())
    )

    desService.trustVariation(payload) map { response =>

      logging.info("variation submitted")

      auditService.auditVariationSubmitted(
        internalId,
        payload,
        response
      )

      response
    }
  }

}
