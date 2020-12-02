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

package controllers

import controllers.actions.{IdentifierAction, ValidateIdentifierActionProvider}
import javax.inject.{Inject, Singleton}
import models.auditing.TrustAuditing
import models.get_trust.{BadRequestResponse, _}
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.{AuditService, TrustService, TransformationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class GetTrustController @Inject()(identify: IdentifierAction,
                                   auditService: AuditService,
                                   desService: TrustService,
                                   transformationService: TransformationService,
                                   validateIdentifier : ValidateIdentifierActionProvider,
                                   cc: ControllerComponents) extends BackendController(cc) with Logging {


  val errorAuditMessages: Map[GetTrustResponse, String] = Map(
    BadRequestResponse -> "Bad Request received from DES.",
    ResourceNotFoundResponse -> "Not Found received from DES.",
    InternalServerErrorResponse -> "Internal Server Error received from DES.",
    ServiceUnavailableResponse -> "Service Unavailable received from DES."
  )

  val errorResponses: Map[GetTrustResponse, Result] = Map (
    ResourceNotFoundResponse -> NotFound
  )

  def getFromEtmp(identifier: String): Action[AnyContent] =
    doGet(identifier, applyTransformations = false, refreshEtmpData = true) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def get(identifier: String, applyTransformations: Boolean = false): Action[AnyContent] =
    doGet(identifier, applyTransformations) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def getLeadTrustee(identifier: String): Action[AnyContent] =
    doGet(identifier, applyTransformations = true) {
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

  def getTrustDetails(identifier: String): Action[AnyContent] =
    getItemAtPath(identifier, JsPath \ 'details \ 'trust \ 'details)

  def getTrustees(identifier: String) : Action[AnyContent] =
    getArrayAtPath(identifier, JsPath \ 'details \ 'trust \ 'entities \ 'trustees, "trustees")

  def getBeneficiaries(identifier: String) : Action[AnyContent] =
    getArrayAtPath(identifier, JsPath \ 'details \ 'trust \ 'entities \ 'beneficiary, "beneficiary")

  def getSettlors(identifier: String) : Action[AnyContent] =
    processEtmpData(identifier) {
      transformed =>
        val settlorsPath = JsPath \ 'details \ 'trust \ 'entities \ 'settlors
        val deceasedPath = JsPath \ 'details \ 'trust \ 'entities \ 'deceased

        val settlors = transformed.transform(settlorsPath.json.pick).getOrElse(Json.obj())
        val deceased = transformed.transform(deceasedPath.json.pick)
        val amendedSettlors = deceased.map {
          deceased => settlors.as[JsObject] + ("deceased" -> deceased)
        }.getOrElse(settlors)

        Json.obj("settlors" -> amendedSettlors)
    }

  def getDeceasedSettlorDeathRecorded(identifier: String) : Action[AnyContent] =
    processEtmpData(identifier, applyTransformations = false) {
      etmpData =>
        val deceasedDeathDatePath = JsPath \ 'details \ 'trust \ 'entities \ 'deceased \ 'dateOfDeath
        JsBoolean(etmpData.transform(deceasedDeathDatePath.json.pick).isSuccess)
    }

  private val protectorsPath = JsPath \ 'details \ 'trust \ 'entities \ 'protectors

  def getProtectorsAlreadyExist(identifier: String) : Action[AnyContent] =
    processEtmpData(identifier) {
      trustData =>
        JsBoolean(!trustData.transform(protectorsPath.json.pick).asOpt.contains(
          Json.obj("protector" -> JsArray(), "protectorCompany" -> JsArray()))
        )
    }

  def getProtectors(identifier: String) : Action[AnyContent] =
    getArrayAtPath(identifier, protectorsPath, "protectors")

  private val otherIndividualsPath = JsPath \ 'details \ 'trust \ 'entities \ 'naturalPerson

  def getOtherIndividualsAlreadyExist(identifier: String): Action[AnyContent] =
    processEtmpData(identifier) {
      trustData => JsBoolean(trustData.transform((otherIndividualsPath \ 0).json.pick).isSuccess)
    }

  def getOtherIndividuals(identifier: String) : Action[AnyContent] =
    getArrayAtPath(identifier, otherIndividualsPath, "naturalPerson")

  private def getArrayAtPath(identifier: String, path: JsPath, fieldName: String): Action[AnyContent] = {
    getElementAtPath(identifier,
      path,
      Json.obj(fieldName -> JsArray())) {
        json => Json.obj(fieldName -> json)
      }
  }

  private def getItemAtPath(identifier: String, path: JsPath): Action[AnyContent] = {
    getElementAtPath(identifier,
      path,
      Json.obj()) {
        json => json
      }
  }

  private def getElementAtPath(identifier: String,
                             path: JsPath,
                             defaultValue: JsValue)
                              (insertIntoObject: JsValue => JsValue): Action[AnyContent] = {
    processEtmpData(identifier) {
      transformed => transformed
        .transform(path.json.pick)
        .map(insertIntoObject)
        .getOrElse(defaultValue)
    }
  }

  private def processEtmpData(identifier: String, applyTransformations: Boolean = true)
                              (processObject: JsValue => JsValue): Action[AnyContent] = {
    doGet(identifier, applyTransformations) {
      case processed: TrustProcessedResponse =>
        processed.transform.map {
          case transformed: TrustProcessedResponse =>
            Ok(processObject(transformed.getTrust))
          case _ =>
            InternalServerError
        }.getOrElse(InternalServerError)
      case _ =>
        Forbidden
    }
  }

  private def resetCacheIfRequested(identifier: String, internalId: String, refreshEtmpData: Boolean) = {
    if (refreshEtmpData) {
      val resetTransforms = transformationService.removeAllTransformations(identifier, internalId)
      val resetCache = desService.resetCache(identifier, internalId)
      for {
        _ <- resetTransforms
        cache <- resetCache
      } yield cache
    } else {
      Future.successful(())
    }
  }

  private def doGet(identifier: String, applyTransformations: Boolean, refreshEtmpData: Boolean = false)
                   (f: GetTrustSuccessResponse => Result): Action[AnyContent] = (validateIdentifier(identifier) andThen identify).async {
      implicit request =>
        {
          for {
            _ <- resetCacheIfRequested(identifier, request.identifier, refreshEtmpData)
            data <- if (applyTransformations) {
              transformationService.getTransformedData(identifier, request.identifier)
            } else {
              desService.getTrustInfo(identifier, request.identifier)
            }
          } yield (
            successResponse(f, identifier) orElse
            notEnoughDataResponse(identifier) orElse
            errorResponse(identifier)
          ).apply(data)
        } recover {
          case e =>
            logger.error(s"[Session ID: ${request.sessionId}][UTR/URN: $identifier] Failed to get trust info ${e.getMessage}")
            InternalServerError
        }
    }

  private def successResponse(f: GetTrustSuccessResponse => Result,
                              identifier: String)
                             (implicit request: IdentifierRequest[AnyContent]): PartialFunction[GetTrustResponse, Result] = {
    case response: GetTrustSuccessResponse =>
      auditService.audit(
        event = TrustAuditing.GET_TRUST,
        request = Json.obj("utr" -> identifier),
        internalId = request.identifier,
        response = Json.toJson(response)
      )

      f(response)
  }

  private def notEnoughDataResponse(identifier: String)
                           (implicit request: IdentifierRequest[AnyContent]): PartialFunction[GetTrustResponse, Result] = {
    case NotEnoughDataResponse(json, errors) =>
      val reason = Json.obj(
        "response" -> json,
        "reason" -> "Missing mandatory fields in response received from DES",
        "errors" -> errors
      )

      auditService.audit(
        event = TrustAuditing.GET_TRUST,
        request = Json.obj("utr" -> identifier),
        internalId = request.identifier,
        response = reason
      )

      NoContent
  }

  private def errorResponse(identifier: String)
                           (implicit request: IdentifierRequest[AnyContent]): PartialFunction[GetTrustResponse, Result] = {
    case err =>
      auditService.auditErrorResponse(
        TrustAuditing.GET_TRUST,
        Json.obj("utr" -> identifier),
        request.identifier,
        errorAuditMessages.getOrElse(err, "UNKNOWN")
      )
      errorResponses.getOrElse(err, InternalServerError)
  }
}
