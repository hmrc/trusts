/*
 * Copyright 2022 HM Revenue & Customs
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
import models.auditing.TrustAuditing
import models.get_trust.GetTrustResponse.CLOSED_REQUEST_STATUS
import models.get_trust.{BadRequestResponse, ResourceNotFoundResponse, _}
import models.requests.IdentifierRequest
import models.taxable_migration.MigrationStatus.MigrationStatus
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.auditing.AuditService
import services.{TaxYearService, TransformationService, TrustsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Constants._
import utils.{RequiredEntityDetailsForMigration, Session}
import java.time.LocalDate

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetTrustController @Inject()(identify: IdentifierAction,
                                   auditService: AuditService,
                                   trustsService: TrustsService,
                                   transformationService: TransformationService,
                                   taxYearService: TaxYearService,
                                   validateIdentifier: ValidateIdentifierActionProvider,
                                   requiredDetailsUtil: RequiredEntityDetailsForMigration,
                                   cc: ControllerComponents) extends BackendController(cc) with Logging {

  val errorAuditMessages: Map[GetTrustResponse, String] = Map(
    BadRequestResponse -> "Bad Request received from DES.",
    ResourceNotFoundResponse -> "Not Found received from DES.",
    InternalServerErrorResponse -> "Internal Server Error received from DES.",
    ServiceUnavailableResponse -> "Service Unavailable received from DES.",
    ClosedRequestResponse -> "Closed Request response received from DES."
  )

  val errorResponses: Map[GetTrustResponse, Result] = Map (
    ResourceNotFoundResponse -> NotFound,
    ClosedRequestResponse -> Status(CLOSED_REQUEST_STATUS),
    ServiceUnavailableResponse -> ServiceUnavailable
  )

  def getFromEtmp(identifier: String): Action[AnyContent] =
    doGet(identifier, applyTransformations = false, refreshEtmpData = true) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def get(identifier: String, applyTransformations: Boolean = false): Action[AnyContent] =
    doGet(identifier, applyTransformations) {
      result: GetTrustSuccessResponse => Ok(Json.toJson(result))
    }

  def getLeadTrustee(identifier: String): Action[AnyContent] = {
    getEtmpData(identifier) { processed =>
      val pick = (ENTITIES \ LEAD_TRUSTEE).json.pick
      processed.getTrust.transform(pick).fold(
        _ => InternalServerError,
        json => {
          json.validate[DisplayTrustLeadTrusteeType] match {
            case JsSuccess(DisplayTrustLeadTrusteeType(Some(leadTrusteeInd), None), _) =>
              Ok(Json.toJson(leadTrusteeInd))
            case JsSuccess(DisplayTrustLeadTrusteeType(None, Some(leadTrusteeOrg)), _) =>
              Ok(Json.toJson(leadTrusteeOrg))
            case _ =>
              logger.error(s"[GetTrustController][getLeadTrustee][UTR/URN: $identifier] something unexpected has happened. " +
                s"doGet has succeeded but picked lead trustee json has failed validation.")
              InternalServerError
          }
        }
      )
    }
  }

  def wasTrustRegisteredWithDeceasedSettlor(identifier: String): Action[AnyContent] =
    isPickSuccessfulAtPath(identifier, ENTITIES \ DECEASED_SETTLOR, applyTransformations = false)

  def getTrustDetails(identifier: String, applyTransformations: Boolean): Action[AnyContent] =
    getItemAtPath(identifier, TRUST \ DETAILS, applyTransformations)

  def getYearsReturns(identifier: String): Action[AnyContent] =
    getItemAtPath(identifier, YEARS_RETURNS)

  def getTrustees(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, ENTITIES \ TRUSTEES, TRUSTEES)

  def getBeneficiaries(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, ENTITIES \ BENEFICIARIES, BENEFICIARIES)

  def getAssets(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, TRUST \ ASSETS, ASSETS)

  def getSettlors(identifier: String): Action[AnyContent] =
    processEtmpData(identifier) {
      transformed =>
        val settlorsPath = ENTITIES \ SETTLORS
        val deceasedPath = ENTITIES \ DECEASED_SETTLOR

        val settlors = transformed.transform(settlorsPath.json.pick).getOrElse(Json.obj())
        val deceased = transformed.transform(deceasedPath.json.pick)
        val amendedSettlors = deceased.map {
          deceased => settlors.as[JsObject] + (DECEASED_SETTLOR -> deceased)
        }.getOrElse(settlors)

        Json.obj(SETTLORS -> amendedSettlors)
    }

  def getDeceasedSettlorDeathRecorded(identifier: String): Action[AnyContent] =
    isPickSuccessfulAtPath(identifier, ENTITIES \ DECEASED_SETTLOR \ DATE_OF_DEATH, applyTransformations = false)

  def getProtectorsAlreadyExist(identifier: String): Action[AnyContent] =
    processEtmpData(identifier) {
      trustData =>
        JsBoolean(!trustData.transform((ENTITIES \ PROTECTORS).json.pick).asOpt.contains(
          Json.obj(INDIVIDUAL_PROTECTOR -> JsArray(), BUSINESS_PROTECTOR -> JsArray()))
        )
    }

  def getProtectors(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, ENTITIES \ PROTECTORS, PROTECTORS)

  def getOtherIndividualsAlreadyExist(identifier: String): Action[AnyContent] = {
    isPickSuccessfulAtPath(identifier, ENTITIES \ OTHER_INDIVIDUALS \ 0)
  }

  def getOtherIndividuals(identifier: String): Action[AnyContent] =
    getArrayAtPath(identifier, ENTITIES \ OTHER_INDIVIDUALS, OTHER_INDIVIDUALS)

  def getNonEeaCompaniesAlreadyExist(identifier: String): Action[AnyContent] = {
    isPickSuccessfulAtPath(identifier, TRUST \ ASSETS \ NON_EEA_BUSINESS_ASSET \ 0)
  }

  def areBeneficiariesCompleteForMigration(identifier: String): Action[AnyContent] =
    areEntitiesCompleteForMigration(identifier)(requiredDetailsUtil.areBeneficiariesCompleteForMigration)

  def areSettlorsCompleteForMigration(identifier: String): Action[AnyContent] =
    areEntitiesCompleteForMigration(identifier)(requiredDetailsUtil.areSettlorsCompleteForMigration)

  private def areEntitiesCompleteForMigration(identifier: String)(f: JsValue => JsResult[MigrationStatus]): Action[AnyContent] = {
    getEtmpData(identifier) {
      processed =>
        f(processed.getTrust) match {
          case JsSuccess(value, _) => Ok(Json.toJson(value))
          case JsError(errors) =>
            logger.error(s"[GetTrustController][areEntitiesCompleteForMigration][Identifier: $identifier] Failed to check entities: $errors")
            InternalServerError
        }
    }
  }

  def isTrust5mld(identifier: String): Action[AnyContent] =
    isPickSuccessfulAtPath(identifier, TRUST \ DETAILS \ EXPRESS, applyTransformations = false)

  def getTrustName(identifier: String): Action[AnyContent] =
    getItemAtPath(identifier, TRUST_NAME)

  def getFirstTaxYearAvailable(identifier: String): Action[AnyContent] = {
    getEtmpData(identifier) { processed =>
      processed.getTrust.transform((TRUST \ DETAILS \ START_DATE).json.pick) match {
        case JsSuccess(value, _) =>
          Ok(Json.toJson(taxYearService.firstTaxYearAvailable(value.as[LocalDate])))
        case JsError(errors) =>
          logger.error(s"[Identifier: $identifier] Failed to pick trust start date: $errors")
          InternalServerError
      }
    }
  }

  private def isPickSuccessfulAtPath(identifier: String, path: JsPath, applyTransformations: Boolean = true): Action[AnyContent] = {
    processEtmpData(identifier, applyTransformations) {
      etmpData =>
        JsBoolean(etmpData.transform(path.json.pick).isSuccess)
    }
  }

  private def getArrayAtPath(identifier: String, path: JsPath, fieldName: String, applyTransformations: Boolean = true): Action[AnyContent] = {
    getElementAtPath(
      identifier,
      path,
      Json.obj(fieldName -> JsArray()),
      applyTransformations
    ) {
      json => Json.obj(fieldName -> json)
    }
  }

  private def getItemAtPath(identifier: String, path: JsPath, applyTransformations: Boolean = true): Action[AnyContent] = {
    getElementAtPath(
      identifier,
      path,
      Json.obj(),
      applyTransformations
    ) {
      json => json
    }
  }

  private def getElementAtPath(identifier: String, path: JsPath, defaultValue: JsValue, applyTransformations: Boolean)
                              (insertIntoObject: JsValue => JsValue): Action[AnyContent] = {
    processEtmpData(identifier, applyTransformations) {
      transformed => transformed
        .transform(path.json.pick)
        .map(insertIntoObject)
        .getOrElse(defaultValue)
    }
  }

  private def processEtmpData(identifier: String, applyTransformations: Boolean = true)
                             (processObject: JsValue => JsValue): Action[AnyContent] = {

    getEtmpData(identifier, applyTransformations) { processed =>
      processed.transform.map {
        case transformed: TrustProcessedResponse =>
          Ok(processObject(transformed.getTrust))
        case _ =>
          InternalServerError
      }.getOrElse(InternalServerError)
    }
  }

  private def getEtmpData(identifier: String, applyTransformations: Boolean = true)
                         (processObject: TrustProcessedResponse => Result): Action[AnyContent] = {
    doGet(identifier, applyTransformations) {
      case processed: TrustProcessedResponse =>
        processObject(processed)
      case _ =>
        Forbidden
    }
  }

  private def resetCacheIfRequested(identifier: String, internalId: String, sessionId: String, refreshEtmpData: Boolean): Future[Unit] = {
    if (refreshEtmpData) {
      val resetTransforms = transformationService.removeAllTransformations(identifier, internalId, sessionId)
      val resetCache = trustsService.resetCache(identifier, internalId, sessionId)
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
          _ <- resetCacheIfRequested(identifier, request.internalId, Session.id(hc), refreshEtmpData)
          data <- if (applyTransformations) {
            transformationService.getTransformedData(identifier, request.internalId, Session.id(hc))
          } else {
            trustsService.getTrustInfo(identifier, request.internalId, Session.id(hc))
          }
        } yield (
          successResponse(f, identifier) orElse
            notEnoughDataResponse(identifier) orElse
            errorResponse(identifier)
          ).apply(data)
      } recover {
        case e =>
          logger.error(s"[GetTrustcontroller][doGet][Session ID: ${request.sessionId}][UTR/URN: $identifier] Failed to get trust info ${e.getMessage}")
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
        internalId = request.internalId,
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
        internalId = request.internalId,
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
        request.internalId,
        errorAuditMessages.getOrElse(err, "UNKNOWN")
      )
      errorResponses.getOrElse(err, InternalServerError)
  }
}
