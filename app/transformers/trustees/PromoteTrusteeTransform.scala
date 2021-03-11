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

package transformers.trustees

import models.variation._
import play.api.libs.json._
import transformers.{AmendEntityTransform, DeltaTransform}
import utils.Constants._

import java.time.LocalDate

case class PromoteTrusteeTransform(index: Option[Int],
                                   amended: JsValue,
                                   original: JsValue,
                                   endDate: LocalDate,
                                   `type`: String,
                                   isTaxable: Boolean) extends TrusteeTransform with AmendEntityTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    transform(input)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    removeTrusteeTransform.applyDeclarationTransform(input)
  }

  private val removeTrusteeTransform = RemoveTrusteeTransform(index, original, endDate, `type`)

  private def transform(input: JsValue): JsResult[JsValue] = {
    for {
      entityStart <- original.transform((__ \ `type` \ ENTITY_START).json.pick)
      trusteePromotedJson <- input.transform(promoteTrustee(entityStart))
      trusteeRemovedJson <- removeTrusteeTransform.applyTransform(trusteePromotedJson)
      leadTrusteeDemotedJson <- demoteLeadTrusteeTransform(input).applyTransform(trusteeRemovedJson)
    } yield leadTrusteeDemotedJson
  }

  private def promoteTrustee(entityStart: JsValue): Reads[JsObject] = {
    leadTrusteePath.json.prune andThen
      __.json.update(leadTrusteePath.json.put(amended)) andThen
      __.json.update((leadTrusteePath \ ENTITY_START).json.put(entityStart)) andThen
      (leadTrusteePath \ LINE_NUMBER).json.prune andThen
      (leadTrusteePath \ BP_MATCH_STATUS).json.prune
  }

  private def demoteLeadTrusteeTransform(input: JsValue): DeltaTransform = {

    def validate[T](implicit rds: Reads[T]): Option[T] = input.transform(leadTrusteePath.json.pick).flatMap(_.validate[T]).asOpt

    (validate[LeadTrusteeIndType], validate[LeadTrusteeOrgType]) match {

      case (Some(leadTrustee), None) =>
        val trustee = TrusteeIndividualType(
          lineNo = None,
          bpMatchStatus = None,
          name = leadTrustee.name,
          dateOfBirth = Some(leadTrustee.dateOfBirth),
          phoneNumber = None,
          identification = adjustIdentification(leadTrustee.identification),
          countryOfResidence = leadTrustee.countryOfResidence,
          legallyIncapable = leadTrustee.legallyIncapable,
          nationality = leadTrustee.nationality,
          entityStart = leadTrustee.entityStart,
          entityEnd = leadTrustee.entityEnd
        )
        AddTrusteeTransform(Json.toJson(trustee), INDIVIDUAL_TRUSTEE)

      case (None, Some(leadTrustee)) =>
        val trustee = TrusteeOrgType(
          lineNo = None,
          bpMatchStatus = None,
          name = leadTrustee.name,
          phoneNumber = None,
          email = None,
          identification = adjustIdentification(leadTrustee.identification),
          countryOfResidence = leadTrustee.countryOfResidence,
          entityStart = leadTrustee.entityStart,
          entityEnd = leadTrustee.entityEnd
        )
        AddTrusteeTransform(Json.toJson(trustee), BUSINESS_TRUSTEE)

      case _ => throw new Exception("Failed to validate existing lead trustee.")
    }
  }

  private def adjustIdentification(identification: IdentificationType): Option[IdentificationType] = {
    (isTaxable, identification.nino.isDefined) match {
      case (true, true) => Some(identification.copy(address = None))
      case (false, _) => None
      case _ => Some(identification)
    }
  }

  private def adjustIdentification(identification: IdentificationOrgType): Option[IdentificationOrgType] = {
    (isTaxable, identification.utr.isDefined) match {
      case (true, true) => Some(identification.copy(address = None))
      case (false, _) => None
      case _ => Some(identification)
    }
  }
}

object PromoteTrusteeTransform {

  val key = "PromoteTrusteeTransform"

  implicit val format: Format[PromoteTrusteeTransform] = Json.format[PromoteTrusteeTransform]
}
