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

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustTrusteeType
import uk.gov.hmrc.trusts.models.variation.{TrusteeIndividualType, TrusteeOrgType}

trait AmendTrusteeCommon {

  val currentDate: LocalDate

  def transform(index: Int, originalJson: JsValue, originalTrusteeJson: JsValue, newTrusteeJson: JsValue): JsResult[JsValue] = {

    val entityStartDate = getEntityStartDate(originalTrusteeJson)

    for {
      trusteeRemovedJson <- RemoveTrusteeTransform(currentDate, index, originalTrusteeJson).applyTransform(originalJson)
      trusteeAddedJson <- addTrustee(newTrusteeJson, trusteeRemovedJson, entityStartDate)
    } yield {
      trusteeAddedJson
    }
  }

  private def addTrustee(newTrustee: JsValue, updatedJson: JsValue, entityStart: LocalDate): JsResult[JsValue] = {
    val indTrustee = newTrustee.validate[TrusteeIndividualType].asOpt
    val orgTrustee = newTrustee.validate[TrusteeOrgType].asOpt

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

  private def getEntityStartDate(trusteeToRemove: JsValue): LocalDate = {

    trusteeToRemove.validate[DisplayTrustTrusteeType].asOpt match {
      case Some(DisplayTrustTrusteeType(Some(ind), None)) => ind.entityStart
      case Some(DisplayTrustTrusteeType(None, Some(org))) => org.entityStart
      case _ => throw new Exception("Existing trustee could not be identified")
    }
  }

  def declarationTransform(input: JsValue, endDate: LocalDate, index: Int, originalTrusteeJson: JsValue): JsResult[JsValue] = {
    RemoveTrusteeTransform(endDate, index, originalTrusteeJson).applyDeclarationTransform(input)
  }
}