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

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import controllers.actions.IdentifierAction
import models.variation._
import services.BeneficiaryTransformationService
import transformers.remove.RemoveBeneficiary
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class BeneficiaryTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          beneficiaryTransformationService: BeneficiaryTransformationService
                                        )(implicit val executionContext: ExecutionContext,
                                          cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def amendUnidentifiedBeneficiary(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[JsString] match {
        case JsSuccess(description, _) =>
          beneficiaryTransformationService.amendUnidentifiedBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            description.value
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendUnidentifiedBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied description could not be read as a JsString - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addUnidentifiedBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[UnidentifiedType] match {
        case JsSuccess(newBeneficiary, _) =>

          beneficiaryTransformationService.addUnidentifiedBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addUnidentifiedBeneficiary][Session ID: ${request.sessionId}] " +
            s"Supplied json could not be read as an Unidentified Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addIndividualBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[IndividualDetailsType] match {
        case JsSuccess(newBeneficiary, _) =>
          beneficiaryTransformationService.addIndividualBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addIndividualBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as an Individual Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveBeneficiary] match {
        case JsSuccess(beneficiary, _) =>
          beneficiaryTransformationService.removeBeneficiary(utr, request.identifier, beneficiary) map { _ =>
          Ok
        }
        case JsError(_) => Future.successful(BadRequest)
      }
    }
  }

  def amendIndividualBeneficiary(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[IndividualDetailsType] match {
        case JsSuccess(individual, _) =>
          beneficiaryTransformationService.amendIndividualBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            individual
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendIndividualBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied payload could not be read as a IndividualDetailsType - $errors")
          Future.successful(BadRequest)
      }
  }

  def addCharityBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[BeneficiaryCharityType] match {
        case JsSuccess(newBeneficiary, _) =>
          beneficiaryTransformationService.addCharityBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addCharityBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as a Charity Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendCharityBeneficiary(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[BeneficiaryCharityType] match {
        case JsSuccess(charity, _) =>
          beneficiaryTransformationService.amendCharityBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            charity
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendCharityBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied payload could not be read as a CharityType - $errors")
          Future.successful(BadRequest)
      }
  }

  def addOtherBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[OtherType] match {
        case JsSuccess(newBeneficiary, _) =>
          beneficiaryTransformationService.addOtherBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addOtherBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as an Other Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendOtherBeneficiary(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[OtherType] match {
        case JsSuccess(otherBeneficiary, _) =>
          beneficiaryTransformationService.amendOtherBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            otherBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendOtherBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied payload could not be read as a OtherType - $errors")
          Future.successful(BadRequest)
      }
  }

  def addCompanyBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[BeneficiaryCompanyType] match {
        case JsSuccess(newBeneficiary, _) =>
          beneficiaryTransformationService.addCompanyBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addCompanyBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as a Company Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addTrustBeneficiary(utr: String) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[BeneficiaryTrustType] match {
        case JsSuccess(newBeneficiary, _) =>
          beneficiaryTransformationService.addTrustBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addTrustBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as a Trust Beneficiary - $errors")
          Future.successful(BadRequest)
      }
  }

  def amendCompanyBeneficiary(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[BeneficiaryCompanyType] match {
        case JsSuccess(companyBeneficiary, _) =>
          beneficiaryTransformationService.amendCompanyBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            companyBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendCompanyBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied payload could not be read as a BeneficiaryCompanyType - $errors")
          Future.successful(BadRequest)
      }
  }

  def amendTrustBeneficiary(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[BeneficiaryTrustType] match {
        case JsSuccess(charity, _) =>
          beneficiaryTransformationService.amendTrustBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            charity
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendTrustBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied payload could not be read as a BeneficiaryTrustType - $errors")
          Future.successful(BadRequest)
      }
  }

  def addLargeBeneficiary(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[LargeType] match {
        case JsSuccess(newBeneficiary, _) =>
          beneficiaryTransformationService.addLargeBeneficiaryTransformer(
            utr,
            request.identifier,
            newBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addLargeBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied json could not be read as a Large Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendLargeBeneficiary(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[LargeType] match {
        case JsSuccess(largeBeneficiary, _) =>
          beneficiaryTransformationService.amendLargeBeneficiaryTransformer(
            utr,
            index,
            request.identifier,
            largeBeneficiary
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendLargeBeneficiary][Session ID: ${request.sessionId}]" +
            s" Supplied payload could not be read as a LargeBeneficiary - $errors")
          Future.successful(BadRequest)
      }
  }
}
