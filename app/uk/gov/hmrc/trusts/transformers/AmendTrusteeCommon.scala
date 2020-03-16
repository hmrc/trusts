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

package uk.gov.hmrc.trusts.transformers

import java.time.LocalDate

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType, DisplayTrustTrusteeType}

trait AmendTrusteeCommon {

  def transform(index: Int, originalJson: JsValue, newTrusteeDetails: JsValue): JsResult[JsValue] = {

    val trusteeToRemove = getOriginalTrustee(originalJson, index)

    val entityStartDate = getEntityStartDate(trusteeToRemove)

    for {
      trusteeRemovedJson <- RemoveTrusteeTransform(LocalDate.now(), index, trusteeToRemove).applyTransform(originalJson)
      trusteeAddedJson <- addTrustee(newTrusteeDetails, trusteeRemovedJson, entityStartDate)
    } yield {
      trusteeAddedJson
    }
  }

  private def addTrustee(newTrustee: JsValue, updatedJson: JsValue, entityStart: DateTime): JsResult[JsValue] = {
    val indTrustee = newTrustee.validate[DisplayTrustTrusteeIndividualType].asOpt
    val orgTrustee = newTrustee.validate[DisplayTrustTrusteeOrgType].asOpt

    (indTrustee, orgTrustee) match {
      case (Some(ind), None) =>
        AddTrusteeIndTransform(
          ind.copy(entityStart = entityStart)
        ).applyTransform(updatedJson)
      case (None, Some(org)) =>
        AddTrusteeOrgTransform(
          org.copy(entityStart = entityStart)
        ).applyTransform(updatedJson)
      case _ => throw new Exception("Existing trustee could not be identified")
    }
  }

  private def getEntityStartDate(trusteeToRemove: JsValue): DateTime = {

    trusteeToRemove.validate[DisplayTrustTrusteeType].asOpt match {
      case Some(DisplayTrustTrusteeType(Some(ind), None)) => ind.entityStart
      case Some(DisplayTrustTrusteeType(None, Some(org))) => org.entityStart
      case _ => throw new Exception("Existing trustee could not be identified")
    }
  }

  private def getOriginalTrustee(input: JsValue, index: Int): JsValue = {
    val trusteePath = __ \ 'details \ 'trust \ 'entities \ 'trustees

    input.transform(trusteePath.json.pick) match {

      case JsSuccess(json, _) =>

        val list = json.as[JsArray].value.toList
        list(index).validate[JsValue] match {
          case JsSuccess(value, _) =>
            value

          case JsError(errors) =>
            throw JsResultException(errors)
        }
      case JsError(errors) =>
        throw JsResultException(errors)
    }
  }

  def declarationTransform(input: JsValue, endDate: LocalDate, index: Int, originalTrusteeJson: JsValue): JsResult[JsValue] = {
    RemoveTrusteeTransform(endDate, index, originalTrusteeJson).applyDeclarationTransform(input)
  }
}