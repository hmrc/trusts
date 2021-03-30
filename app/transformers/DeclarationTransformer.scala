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

package transformers

import models._
import models.get_trust.TrustProcessedResponse
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.JsonOps.{doNothing, putNewValue}
import java.time.LocalDate

import models.variation.DeclarationForApi

class DeclarationTransformer {

  def transform(response: TrustProcessedResponse,
                originalJson: JsValue,
                declarationForApi: DeclarationForApi,
                submissionDate: LocalDate,
                is5mld: Boolean): JsResult[JsValue] = {

    val responseJson = response.getTrust
    val responseHeader = response.responseHeader

    responseJson.transform(
      (__ \ 'applicationType).json.prune andThen
        (__ \ 'declaration).json.prune andThen
        keepYearsReturnsIf5mld(is5mld) andThen
        updateCorrespondence(responseJson) andThen
        fixLeadTrusteeAddress(responseJson, pathToLeadTrustees) andThen
        convertLeadTrustee(responseJson) andThen
        addPreviousLeadTrustee(responseJson, originalJson, submissionDate) andThen
        pruneEmptyTrustees(responseJson) andThen
        putNewValue(__ \ 'reqHeader \ 'formBundleNo, JsString(responseHeader.formBundleNo)) andThen
        addDeclaration(declarationForApi, responseJson) andThen
        addAgentIfDefined(declarationForApi.agentDetails) andThen
        addEndDateIfDefined(declarationForApi.endDate) andThen
        addSubmissionDateIf5mld(submissionDate, is5mld)
    )
  }

  private val pathToEntities: JsPath = __ \ 'details \ 'trust \ 'entities
  private val pathToLeadTrustees: JsPath =  pathToEntities \ 'leadTrustees
  private val pathToTrustees: JsPath = pathToEntities \ 'trustees
  private val pathToLeadTrusteeAddress = pathToLeadTrustees \ 'identification \ 'address
  private val pathToLeadTrusteePhoneNumber = pathToLeadTrustees \ 'phoneNumber
  private val pathToLeadTrusteeCountry = pathToLeadTrusteeAddress \ 'country
  private val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
  private val pathToCorrespondencePhoneNumber = __ \ 'correspondence \ 'phoneNumber
  private val pickLeadTrustee = pathToLeadTrustees.json.pick

  private def trusteeField(json: JsValue): String = determineTrusteeField(pathToLeadTrustees, json)

  private def updateCorrespondence(responseJson: JsValue): Reads[JsObject] = {
    val leadTrusteeCountry = responseJson.transform(pathToLeadTrusteeCountry.json.pick)
    val inUk = leadTrusteeCountry.isError || leadTrusteeCountry.get == JsString("GB")
    pathToCorrespondenceAddress.json.prune andThen
      pathToCorrespondencePhoneNumber.json.prune andThen
      putNewValue(__ \ 'correspondence \ 'abroadIndicator, JsBoolean(!inUk)) andThen
      __.json.update(pathToCorrespondenceAddress.json.copyFrom(pathToLeadTrusteeAddress.json.pick)) andThen
      __.json.update(pathToCorrespondencePhoneNumber.json.copyFrom(pathToLeadTrusteePhoneNumber.json.pick))
  }

  private def fixLeadTrusteeAddress(leadTrusteeJson: JsValue, leadTrusteePath: JsPath): Reads[JsObject] = {
    if (leadTrusteeJson.transform((leadTrusteePath \ 'identification \ 'utr).json.pick).isSuccess ||
        leadTrusteeJson.transform((leadTrusteePath \ 'identification \ 'nino).json.pick).isSuccess) {
      (leadTrusteePath \ 'identification \ 'address).json.prune
    } else {
      doNothing()
    }
  }

  private def determineTrusteeField(rootPath: JsPath, json: JsValue): String = {
    val namePath = (rootPath \ 'name).json.pick[JsObject]

    json.transform(namePath).flatMap(_.validate[NameType]) match {
      case JsSuccess(_, _) => "leadTrusteeInd"
      case _ => "leadTrusteeOrg"
    }
  }

  private def addPreviousLeadTrusteeAsExpiredStep(previousLeadTrusteeJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val trusteeField = determineTrusteeField(__, previousLeadTrusteeJson)
    previousLeadTrusteeJson.transform(__.json.update(
      (__ \ 'entityEnd).json.put(Json.toJson(date))
    )).fold(
      errors => Reads(_ => JsError(errors)),
      endedJson => {
        pathToLeadTrustees.json.update(of[JsArray]
          .map { a => a :+ Json.obj(trusteeField -> endedJson) })
      })
  }

  private def addPreviousLeadTrustee(newJson: JsValue, originalJson: JsValue, date: LocalDate): Reads[JsObject] = {
    val newLeadTrustee = newJson.transform(pickLeadTrustee)
    val originalLeadTrustee = originalJson.transform(pickLeadTrustee)

    (newLeadTrustee, originalLeadTrustee) match {
      case (JsSuccess(newLeadTrusteeJson, _), JsSuccess(originalLeadTrusteeJson, _))
        if newLeadTrusteeJson != originalLeadTrusteeJson =>
          val reads = fixLeadTrusteeAddress(originalLeadTrusteeJson, __)
          originalLeadTrusteeJson.transform(reads) match {
            case JsSuccess(value, _) => addPreviousLeadTrusteeAsExpiredStep(value, date)
            case e: JsError => Reads(_ => e)
          }
      case _ => doNothing()
    }
  }

  private def convertLeadTrustee(json: JsValue): Reads[JsObject] = pathToLeadTrustees.json.update( of[JsObject]
    .map{ a => Json.arr(Json.obj(trusteeField(json) -> a )) })

  private def declarationAddress(agentDetails: Option[AgentDetails], responseJson: JsValue): JsResult[AddressType] = {
    if (agentDetails.isDefined) {
      JsSuccess.apply(agentDetails.get.agentAddress)
    } else {
      responseJson.transform((pathToLeadTrustees \ 'identification \ 'address).json.pick).map(_.as[AddressType])
    }
  }

  private def pruneEmptyTrustees(responseJson: JsValue): Reads[JsObject] = {
    val pickTrusteesArray = pathToTrustees.json.pick[JsArray]

    responseJson.transform(pickTrusteesArray) match {
      case JsSuccess(JsArray(e), _) if e.isEmpty =>
        pathToTrustees.json.prune
      case _ =>
        doNothing()
    }
  }

  private def addDeclaration(declarationForApi: DeclarationForApi, responseJson: JsValue): Reads[JsObject] = {
    declarationAddress(declarationForApi.agentDetails, responseJson) match {
      case JsSuccess(address, _) =>
        val declarationToSend = Declaration(declarationForApi.declaration.name, address)
        putNewValue(__ \ 'declaration, Json.toJson(declarationToSend))
      case JsError(errors) =>
        Reads.failed(s"Unable to transform declaration due to ${JsError.toJson(errors)}")
    }
  }

  private def addAgentIfDefined(agentDetails: Option[AgentDetails]): Reads[JsObject] = {
    if (agentDetails.isDefined) {
      __.json.update(
        (__ \ 'agentDetails).json.put(Json.toJson(agentDetails.get))
      )
    } else {
      doNothing()
    }
  }

  private def addEndDateIfDefined(endDate: Option[LocalDate]): Reads[JsObject] = {
    endDate match {
      case Some(date) =>
        __.json.update(
          (__ \ 'trustEndDate).json.put(Json.toJson(date))
        )
      case _ =>
        doNothing()
    }
  }

  private def addSubmissionDateIf5mld(submissionDate: LocalDate, is5mld: Boolean): Reads[JsObject] = {
    if (is5mld) {
      __.json.update(
        (__ \ 'submissionDate).json.put(Json.toJson(submissionDate))
      )
    } else {
      doNothing()
    }
  }

  private def keepYearsReturnsIf5mld(is5mld: Boolean): Reads[JsObject] = {
    if (!is5mld) {
      (__ \ 'yearsReturns).json.prune
    } else {
      doNothing()
    }
  }
}
