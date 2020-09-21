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
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{DisplayTrustIdentificationOrgType, DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustLeadTrusteeOrgType, DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType}

trait PromoteTrusteeCommon {
  private val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)

  val currentDate: LocalDate

  def transform(input: JsValue, index: Int, newLeadTrustee: JsValue, originalTrusteeJson: JsValue): JsResult[JsValue] = {

    val removeTrusteeTransform = RemoveTrusteeTransform(currentDate, index, originalTrusteeJson)

    trusteeEntityStart(originalTrusteeJson) match {
      case JsSuccess(entityStart, _) =>
        for {
          promotedTrusteeJson <- input.transform(promoteTrustee(newLeadTrustee, entityStart))
          removedTrusteeJson <- removeTrusteeTransform.applyTransform(promotedTrusteeJson)
          demotedTrusteeJson <- demoteLeadTrusteeTransform(input).applyTransform(removedTrusteeJson)
        } yield demotedTrusteeJson
      case e: JsError => e
    }
  }

  private def trusteeEntityStart(trusteeJson: JsValue) = {
    trusteeJson.transform((__ \ 'trusteeInd \ 'entityStart).json.pick) orElse
      trusteeJson.transform((__ \ 'trusteeOrg \ 'entityStart).json.pick)
  }

  private def promoteTrustee(newLeadTrustee: JsValue, entityStart: JsValue) = {
    leadTrusteesPath.json.prune andThen
      (__).json.update(leadTrusteesPath.json.put(newLeadTrustee)) andThen
      (__).json.update((leadTrusteesPath \ 'entityStart).json.put(entityStart)) andThen
      (leadTrusteesPath \ 'lineNo).json.prune andThen
      (leadTrusteesPath \ 'bpMatchStatus).json.prune
  }

  private def demoteLeadTrusteeTransform(input: JsValue): DeltaTransform = {
    val oldLeadIndTrustee = input.transform(leadTrusteesPath.json.pick).flatMap(_.validate[DisplayTrustLeadTrusteeIndType]).asOpt
    val oldLeadOrgTrustee = input.transform(leadTrusteesPath.json.pick).flatMap(_.validate[DisplayTrustLeadTrusteeOrgType]).asOpt

    (oldLeadIndTrustee, oldLeadOrgTrustee) match {
      case (Some(indLead), None) =>
        val demotedTrustee = DisplayTrustTrusteeIndividualType(
          None,
          None,
          indLead.name,
          Some(indLead.dateOfBirth),
          Some(indLead.phoneNumber),
          Some(getIdentification(indLead.identification)),
          None,
          None,
          None,
          indLead.entityStart.get
        )

        AddTrusteeIndTransform(demotedTrustee)

      case (None, Some(orgLead)) =>

        val demotedTrustee = DisplayTrustTrusteeOrgType(
          None,
          None,
          orgLead.name,
          Some(orgLead.phoneNumber),
          orgLead.email,
          Some(getIdentification(orgLead.identification)),
          None,
          orgLead.entityStart.get
        )

        AddTrusteeOrgTransform(demotedTrustee)

      case _ => throw new Exception("Existing Lead trustee could not be identified")
    }
  }

  private def getIdentification(identification: DisplayTrustIdentificationType): DisplayTrustIdentificationType = {
    if (identification.nino.isDefined) {
      DisplayTrustIdentificationType(identification.safeId, identification.nino, identification.passport, None)
    }
    else {
      identification
    }
  }

  private def getIdentification(identification: DisplayTrustIdentificationOrgType): DisplayTrustIdentificationOrgType = {
    if (identification.utr.isDefined) {
      DisplayTrustIdentificationOrgType(identification.safeId, identification.utr, None)
    }
    else {
      identification
    }
  }

  def declarationTransform(input: JsValue, endDate: LocalDate, index: Int, originalTrusteeJson: JsValue): JsResult[JsValue] = {
    RemoveTrusteeTransform(endDate, index, originalTrusteeJson).applyDeclarationTransform(input)
  }
}
