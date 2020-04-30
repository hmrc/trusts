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

package uk.gov.hmrc.trusts.controllers

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsPath, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.trusts.controllers.actions.{IdentifierAction, ValidateUTRAction}
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{BadRequestResponse, _}
import uk.gov.hmrc.trusts.services.{AuditService, DesService, TransformationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class GetTrustController @Inject()(identify: IdentifierAction,
                                   auditService: AuditService,
                                   desService: DesService,
                                   transformationService: TransformationService) extends BaseController {
  private val logger = LoggerFactory.getLogger("application." + classOf[GetTrustController].getCanonicalName)


  val errorAuditMessages: Map[GetTrustResponse, String] = Map (
    InvalidUTRResponse -> "The UTR provided is invalid.",
    InvalidRegimeResponse -> "Invalid regime received from DES.",
    BadRequestResponse -> "Bad Request received from DES.",
    NotEnoughDataResponse -> "Missing mandatory field received from DES.",
    ResourceNotFoundResponse -> "Not Found received from DES.",
    InternalServerErrorResponse -> "Internal Server Error received from DES.",
    ServiceUnavailableResponse -> "Service Unavailable received from DES."
  )

  val errorResponses: Map[GetTrustResponse, Result] = Map (
    NotEnoughDataResponse -> NoContent,
    ResourceNotFoundResponse -> NotFound
  )

  def getFromEtmp(utr: String): Action[AnyContent] =
    doGet(utr, applyTransformations = false, refreshEtmpData = true) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def get(utr: String, applyTransformations: Boolean = false): Action[AnyContent] =
    doGet(utr, applyTransformations) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def getLeadTrustee(utr: String): Action[AnyContent] =
    doGet(utr, applyTransformations = true) {
      case processed: TrustProcessedResponse =>
        val pick = (JsPath \ 'details \ 'trust \ 'entities \ 'leadTrustees).json.pick
        processed.getTrust.transform(pick).fold(
          _ => InternalServerError,
          json => {
            Ok(json.as[DisplayTrustLeadTrusteeType] match {
              case DisplayTrustLeadTrusteeType(Some(leadTrusteeInd), None) => Json.toJson(leadTrusteeInd)
              case DisplayTrustLeadTrusteeType(None, Some(leadTrusteeOrg)) => Json.toJson(leadTrusteeOrg)
            })
          }
        )
      case _ => Forbidden
    }

  def getTrustDetails(utr: String): Action[AnyContent] =
    getItemAtPath(utr, JsPath \ 'details \ 'trust \ 'details)

  def getTrustees(utr: String) : Action[AnyContent] =
    getItemAtPath(utr, JsPath \ 'details \ 'trust \ 'entities \ 'trustees, "trustees")

  def getBeneficiaries(utr: String) : Action[AnyContent] =
    getItemAtPath(utr, JsPath \ 'details \ 'trust \ 'entities \ 'beneficiary, "beneficiary")

  def getSettlors(utr: String) : Action[AnyContent] =
    getItemAtPath(utr, JsPath \ 'details \ 'trust \ 'entities \ 'settlors, "settlors")

  def getDeceasedSettlor(utr: String) : Action[AnyContent] =
    getItemAtPath(utr, JsPath \ 'details \ 'trust \ 'entities \ 'deceased)

  private def getItemAtPath(utr: String, path: JsPath, fieldName: String): Action[AnyContent] = {
    getItemAtPath(utr, path, json => Json.obj(fieldName -> json))
  }

  private def getItemAtPath(utr: String, path: JsPath): Action[AnyContent] = {
    getItemAtPath(utr, path, json => json)
  }

  private def getItemAtPath(utr: String, path: JsPath, insertIntoObject: JsValue => JsValue): Action[AnyContent] = {
    doGet(utr, applyTransformations = true) {
      case processed: TrustProcessedResponse =>
        processed.transform.map {
          case transformed: TrustProcessedResponse =>

            Ok(transformed
              .getTrust.transform(path.json.pick)
              .map(insertIntoObject)
              .getOrElse(insertIntoObject(JsArray())))
          case _ =>
            InternalServerError
        }.getOrElse(InternalServerError)
      case _ =>
        Forbidden
    }
  }

  private def resetCacheIfRequested(utr: String, internalId: String, refreshEtmpData: Boolean) = {
    if (refreshEtmpData) {
      val resetTransforms = transformationService.removeAllTransformations(utr, internalId)
      val resetCache = desService.resetCache(utr, internalId)
      for {
        _ <- resetTransforms
        cache <- resetCache
      } yield cache
    } else {
      Future.successful(())
    }
  }

  private def doGet(utr: String, applyTransformations: Boolean, refreshEtmpData: Boolean = false)
                   (handleResult: GetTrustSuccessResponse => Result): Action[AnyContent] = {

    (ValidateUTRAction(utr) andThen identify).async {
      implicit request =>

        resetCacheIfRequested(utr, request.identifier, refreshEtmpData).flatMap { _ =>

          val data = if (applyTransformations) {
            transformationService.getTransformedData(utr, request.identifier)
          } else {
            desService.getTrustInfo(utr, request.identifier)
          }

          data.flatMap {
            case response: GetTrustSuccessResponse =>
              auditService.audit(
                event = TrustAuditing.GET_TRUST,
                request = Json.obj("utr" -> utr),
                internalId = request.identifier,
                response = Json.toJson(response)
              )

              Future.successful(handleResult(response))

            case err =>
              auditService.auditErrorResponse(
                TrustAuditing.GET_TRUST,
                Json.obj("utr" -> utr),
                request.identifier,
                errorAuditMessages.getOrElse(err, "UNKNOWN")
              )
              Future.successful(errorResponses.getOrElse(err, InternalServerError))
          }.recover {
            case ex =>
              logger.error("Failed to get trust info", ex)
              InternalServerError
          }
        }
    }
  }
}
