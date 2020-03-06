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

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{
  DisplayTrustLeadTrusteeIndType,
  DisplayTrustLeadTrusteeOrgType,
  DisplayTrustTrusteeIndividualType,
  DisplayTrustTrusteeOrgType
}

trait PromoteTrusteeCommon {

  def transform(input: JsValue, index: Int, newLeadTrustee: JsValue): JsResult[JsValue] = {

    val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)

    val oldLeadIndTrustee = input.transform(leadTrusteesPath.json.pick).flatMap(_.validate[DisplayTrustLeadTrusteeIndType]).asOpt
    val oldLeadOrgTrustee = input.transform(leadTrusteesPath.json.pick).flatMap(_.validate[DisplayTrustLeadTrusteeOrgType]).asOpt

    val demotedTrusteeJson = (oldLeadIndTrustee, oldLeadOrgTrustee) match {
      case (Some(indLead), None) =>
        val demotedTrustee = DisplayTrustTrusteeIndividualType(
          None,
          None,
          indLead.name,
          Some(indLead.dateOfBirth),
          Some(indLead.phoneNumber),
          Some(indLead.identification),
          indLead.entityStart.get
        )

        AddTrusteeIndTransform(demotedTrustee).applyTransform(input)

      case (None, Some(orgLead)) =>

        val demotedTrustee = DisplayTrustTrusteeOrgType(
          None,
          None,
          orgLead.name,
          Some(orgLead.phoneNumber),
          orgLead.email,
          Some(orgLead.identification),
          orgLead.entityStart.get,
          None
        )

        AddTrusteeOrgTransform(demotedTrustee).applyTransform(input)

      case _ => throw new Exception("Existing Lead trustee could not be identified")
    }

    demotedTrusteeJson match {
      case JsSuccess(value, _) =>

        val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)

            val promoteTrustee = value.transform(
              leadTrusteesPath.json.prune andThen
                (__).json.update(leadTrusteesPath.json.put(newLeadTrustee)) andThen
                (leadTrusteesPath \ 'lineNo).json.prune andThen
                (leadTrusteesPath \ 'bpMatchStatus).json.prune
            )

        for {
          promotedTrusteeJson <- promoteTrustee
          removedTrusteeJson <- RemoveTrusteeTransform(DateTime.now, index, Json.obj()).applyTransform(promotedTrusteeJson)
        } yield removedTrusteeJson

      case error: JsError => error
    }
  }
}
