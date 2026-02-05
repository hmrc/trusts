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

package transformers

import models._
import models.get_trust.TrustProcessedResponse
import models.variation.DeclarationForApi
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.Constants._
import utils.JsonOps.{doNothing, prunePathAndPutNewValue, putNewValue}
import utils.{DeedOfVariation, TypeOfTrust}

import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class DeclarationTransformer {

  def transform(
    response: TrustProcessedResponse,
    originalJson: JsValue,
    declarationForApi: DeclarationForApi,
    submissionDate: LocalDate
  ): JsResult[JsValue] = {

    val responseJson   = response.getTrust
    val responseHeader = response.responseHeader

    responseJson.transform(
      (__ \ Symbol("applicationType")).json.prune                                                         andThen
        DECLARATION.json.prune                                                                            andThen
        removeDetailsTypeFields(responseJson)                                                             andThen
        updateCorrespondence(responseJson)                                                                andThen
        fixLeadTrusteeAddress(responseJson, pathToLeadTrustees)                                           andThen
        convertLeadTrustee(responseJson)                                                                  andThen
        addPreviousLeadTrustee(responseJson, originalJson, submissionDate)                                andThen
        pruneEmptyTrustees(responseJson)                                                                  andThen
        putNewValue(__ \ Symbol("reqHeader") \ FORM_BUNDLE_NUMBER, JsString(responseHeader.formBundleNo)) andThen
        addDeclaration(declarationForApi, responseJson)                                                   andThen
        addAgentIfDefined(declarationForApi.agentDetails)                                                 andThen
        addEndDateIfDefined(declarationForApi.endDate)                                                    andThen
        addSubmissionDate(submissionDate)                                                                 andThen
        removeAdditionalShareAssetFields(responseJson)                                                    andThen
        removeAdditionalTrustDetailsField(responseJson)                                                   andThen
        fixInvalidTrustDetails(responseJson)
    )
  }

  private val pathToLeadTrustees: JsPath      = ENTITIES \ LEAD_TRUSTEE
  private val pathToTrustees: JsPath          = ENTITIES \ TRUSTEES
  private val pathToLeadTrusteeAddress        = pathToLeadTrustees \ IDENTIFICATION \ Symbol("address")
  private val pathToLeadTrusteePhoneNumber    = pathToLeadTrustees \ Symbol("phoneNumber")
  private val pathToLeadTrusteeCountry        = pathToLeadTrusteeAddress \ Symbol("country")
  private val pathToCorrespondenceAddress     = CORRESPONDENCE \ Symbol("address")
  private val pathToCorrespondencePhoneNumber = CORRESPONDENCE \ Symbol("phoneNumber")
  private val pathToShareAssets: JsPath       = TRUST \ ASSETS \ SHARES_ASSET
  private val pathToTrustDetails: JsPath      = TRUST \ DETAILS
  private val pickLeadTrustee                 = pathToLeadTrustees.json.pick

  private def trusteeField(json: JsValue): String = determineTrusteeField(pathToLeadTrustees, json)

  private def updateCorrespondence(responseJson: JsValue): Reads[JsObject] = {
    val leadTrusteeCountry = responseJson.transform(pathToLeadTrusteeCountry.json.pick)
    val inUk               = leadTrusteeCountry.isError || leadTrusteeCountry.get == JsString(GB)
    pathToCorrespondenceAddress.json.prune                                                          andThen
      pathToCorrespondencePhoneNumber.json.prune                                                    andThen
      putNewValue(CORRESPONDENCE \ Symbol("abroadIndicator"), JsBoolean(!inUk))                     andThen
      __.json.update(pathToCorrespondenceAddress.json.copyFrom(pathToLeadTrusteeAddress.json.pick)) andThen
      __.json.update(pathToCorrespondencePhoneNumber.json.copyFrom(pathToLeadTrusteePhoneNumber.json.pick))
  }

  private def fixLeadTrusteeAddress(leadTrusteeJson: JsValue, leadTrusteePath: JsPath): Reads[JsObject] =
    if (
      leadTrusteeJson.transform((leadTrusteePath \ IDENTIFICATION \ Symbol("utr")).json.pick).isSuccess ||
      leadTrusteeJson.transform((leadTrusteePath \ IDENTIFICATION \ Symbol("nino")).json.pick).isSuccess
    ) {
      (leadTrusteePath \ IDENTIFICATION \ Symbol("address")).json.prune
    } else {
      doNothing()
    }

  private def determineTrusteeField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ Symbol("name")).json.pick[JsObject]

    json.transform(namePath).flatMap(_.validate[NameType]) match {
      case JsSuccess(_, _) => INDIVIDUAL_LEAD_TRUSTEE
      case _               => BUSINESS_LEAD_TRUSTEE
    }
  }

  private def addPreviousLeadTrusteeAsExpiredStep(
    previousLeadTrusteeJson: JsValue,
    date: LocalDate
  ): Reads[JsObject] = {
    val trusteeField = determineTrusteeField(__, previousLeadTrusteeJson)
    previousLeadTrusteeJson
      .transform(
        __.json.update(
          (__ \ ENTITY_END).json.put(Json.toJson(date))
        )
      )
      .fold(
        errors => Reads(_ => JsError(errors)),
        endedJson => pathToLeadTrustees.json.update(of[JsArray].map(_ :+ Json.obj(trusteeField -> endedJson)))
      )
  }

  private def addPreviousLeadTrustee(newJson: JsValue, originalJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val newLeadTrustee      = newJson.transform(pickLeadTrustee)
    val originalLeadTrustee = originalJson.transform(pickLeadTrustee)

    (newLeadTrustee, originalLeadTrustee) match {
      case (JsSuccess(newLeadTrusteeJson, _), JsSuccess(originalLeadTrusteeJson, _))
          if newLeadTrusteeJson != originalLeadTrusteeJson =>
        val reads = fixLeadTrusteeAddress(originalLeadTrusteeJson, __)
        originalLeadTrusteeJson.transform(reads) match {
          case JsSuccess(value, _) => addPreviousLeadTrusteeAsExpiredStep(value, date)
          case e: JsError          => Reads(_ => e)
        }
      case _ => doNothing()
    }
  }

  private def convertLeadTrustee(json: JsValue): Reads[JsObject] = pathToLeadTrustees.json.update(
    of[JsObject]
      .map(a => Json.arr(Json.obj(trusteeField(json) -> a)))
  )

  private def declarationAddress(agentDetails: Option[AgentDetails], responseJson: JsValue): JsResult[AddressType] =
    if (agentDetails.isDefined) {
      JsSuccess.apply(agentDetails.get.agentAddress)
    } else {
      responseJson.transform((pathToLeadTrustees \ IDENTIFICATION \ Symbol("address")).json.pick).map(_.as[AddressType])
    }

  private def pruneEmptyTrustees(responseJson: JsValue): Reads[JsObject] = {
    val pickTrusteesArray = pathToTrustees.json.pick[JsArray]

    responseJson.transform(pickTrusteesArray) match {
      case JsSuccess(JsArray(e), _) if e.isEmpty =>
        pathToTrustees.json.prune
      case _                                     =>
        doNothing()
    }
  }

  private def removeAdditionalShareAssetFields(json: JsValue): Reads[JsObject] =
    json.transform(pathToShareAssets.json.pick[JsArray]) match {
      case JsSuccess(array, _) =>
        val updatedArray: JsArray = JsArray(array.value.map(_.as[JsObject] - IS_PORTFOLIO - SHARE_CLASS_DISPLAY))
        prunePathAndPutNewValue(pathToShareAssets, updatedArray)
      case _                   =>
        doNothing()
    }

  private def removeAdditionalTrustDetailsField(json: JsValue): Reads[JsObject] =
    json.transform(pathToTrustDetails.json.pick) match {
      case JsSuccess(value, _) =>
        prunePathAndPutNewValue(pathToTrustDetails, value.as[JsObject] - "settlorsUkBased")
      case _                   =>
        doNothing()
    }

  /**
   *
   * @param json Trust JSON with delta transforms applied to it
   * @return a Reads to fix the trust details if they are invalid. This is due to a bug (TRUS-4539) spotted in trusts registration
   *         where trust details was being mapped incorrectly for a deed of variation in addition to a will. This method
   *         checks to see if the trust details contains these two key-value pairs:
   *{{{
   *{
   *  "typeOfTrust": "Will Trust or Intestacy Trust",
   *  "deedOfVariation": "Addition to the will trust"
   *}
   *}}}
   * and if so applies an update such that it becomes:
   * {{{
   *{
   *  "typeOfTrust": "Deed of Variation Trust or Family Arrangement",
   *  "deedOfVariation": "Addition to the will trust"
   *}
   *}}}
   */
  private def fixInvalidTrustDetails(json: JsValue): Reads[JsObject] = {
    val typeOfTrustPath = pathToTrustDetails \ "typeOfTrust"
    (for {
      typeOfTrust     <- json.transform(typeOfTrustPath.json.pick[JsString])
      deedOfVariation <- json.transform((pathToTrustDetails \ "deedOfVariation").json.pick[JsString])
    } yield
      if (
        typeOfTrust.value == TypeOfTrust.Will.toString && deedOfVariation.value == DeedOfVariation.AdditionToWill.toString
      ) {
        putNewValue(typeOfTrustPath, JsString(TypeOfTrust.DeedOfVariationOrFamilyAgreement.toString))
      } else {
        doNothing()
      }) getOrElse
      doNothing()
  }

  private def addDeclaration(declarationForApi: DeclarationForApi, responseJson: JsValue): Reads[JsObject] =
    declarationAddress(declarationForApi.agentDetails, responseJson) match {
      case JsSuccess(address, _) =>
        val declarationToSend = Declaration(declarationForApi.declaration.name, address)
        putNewValue(DECLARATION, Json.toJson(declarationToSend))
      case JsError(errors)       =>
        Reads.failed(s"Unable to transform declaration due to ${JsError.toJson(errors)}")
    }

  private def addAgentIfDefined(agentDetails: Option[AgentDetails]): Reads[JsObject] =
    agentDetails match {
      case Some(value) => putNewValue(__ \ Symbol("agentDetails"), Json.toJson(value))
      case None        => doNothing()
    }

  private def addEndDateIfDefined(endDate: Option[LocalDate]): Reads[JsObject] =
    endDate match {
      case Some(date) =>
        putNewValue(__ \ Symbol("trustEndDate"), Json.toJson(date))
      case _          =>
        doNothing()
    }

  private def removeDetailsTypeFields(json: JsValue): Reads[JsObject] = {
    case class EntityPath(mainPath: JsPath, subPath: JsPath = __)

    val paths = Seq(
      EntityPath(ENTITIES \ SETTLORS \ INDIVIDUAL_SETTLOR),
      EntityPath(ENTITIES \ BENEFICIARIES \ INDIVIDUAL_BENEFICIARY),
      EntityPath(ENTITIES \ PROTECTORS \ INDIVIDUAL_PROTECTOR),
      EntityPath(ENTITIES \ OTHER_INDIVIDUALS),
      EntityPath(ENTITIES \ TRUSTEES, __ \ INDIVIDUAL_TRUSTEE),
      EntityPath(ENTITIES \ LEAD_TRUSTEE)
    )

    paths.foldLeft(doNothing()) { (acc, path) =>
      def transformEntity(entity: JsValue): JsValue =
        entity.transform((path.subPath \ IDENTIFICATION \ PASSPORT \ DETAILS_TYPE).json.prune) match {
          case JsSuccess(entityWithDetailsTypeRemoved, _) => entityWithDetailsTypeRemoved
          case _                                          => entity
        }

      val reads = json.transform(path.mainPath.json.pick) match {
        case JsSuccess(entities: JsArray, _) =>
          prunePathAndPutNewValue(path.mainPath, JsArray(entities.value.map(transformEntity)))
        case JsSuccess(entity: JsObject, _)  =>
          prunePathAndPutNewValue(path.mainPath, transformEntity(entity))
        case _                               =>
          doNothing()
      }

      acc andThen reads
    }
  }

  private def addSubmissionDate(submissionDate: LocalDate): Reads[JsObject] =
    putNewValue(SUBMISSION_DATE, Json.toJson(submissionDate))

}
