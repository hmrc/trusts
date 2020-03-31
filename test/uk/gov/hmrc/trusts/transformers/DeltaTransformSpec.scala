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
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationOrgType, DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType}
import uk.gov.hmrc.trusts.models.variation.IndividualDetailsType

class DeltaTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "DeltaTransform" - {

    "serialise and deserialise json correctly" in {

      val newLeadTrustee = DisplayTrustLeadTrusteeIndType(
        None,
        None,
        NameType("New", Some("lead"), "Trustee"),
        DateTime.parse("2000-01-01"),
        "",
        None,
        DisplayTrustIdentificationType(None, None, None, None),
        None
      )

      val newTrusteeInd = DisplayTrustTrusteeIndividualType(
        Some("lineNo"),
        Some("bpMatchStatus"),
        NameType("New", None, "Trustee"),
        Some(DateTime.parse("2000-01-01")),
        Some("phoneNumber"),
        Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
        DateTime.parse("2000-01-01")
      )

      val newTrusteeOrg = DisplayTrustTrusteeOrgType(
        Some("lineNo"),
        Some("bpMatchStatus"),
        "New Trustee",
        Some("phoneNumber"),
        Some("email"),
        Some(DisplayTrustIdentificationOrgType(None, Some("utr"), None)),
        DateTime.parse("2000-01-01")
      )

      val individualBeneficiary = IndividualDetailsType(
        lineNo = None,
        bpMatchStatus = None,
        NameType("Amended New First 3", None, "Amended New Last 3"),
        dateOfBirth = None,
        vulnerableBeneficiary = true,
        None,
        None,
        None,
        None,
        DateTime.parse("2018-02-28"),
        None
      )

      val amendLeadTrusteeTransform = AmendLeadTrusteeIndTransform(newLeadTrustee)

      val addTrusteeTransform = AddTrusteeIndTransform(newTrusteeInd)

      val removeTrusteeTransform = RemoveTrusteeTransform(
        endDate = LocalDate.parse("2010-01-01"),
        index = 0,
        Json.obj()
      )

      val promoteTrusteeTransform = PromoteTrusteeIndTransform(
        2,
        newLeadTrustee,
        LocalDate.parse("2012-02-06"),
        Json.obj()
      )

      val amendTrusteeIndTransform = AmendTrusteeIndTransform(0, newTrusteeInd, Json.obj())
      val amendTrusteeOrgTransform = AmendTrusteeOrgTransform(0, newTrusteeOrg, Json.obj())

      val amendIndividualBenTransform = AmendIndividualBeneficiaryTransform(0, Json.toJson(individualBeneficiary), Json.obj(), LocalDate.parse("2020-03-25"))

      val json = Json.parse(
        s"""{
          |        "deltaTransforms" : [
          |            {
          |                "AmendLeadTrusteeIndTransform": ${Json.toJson(amendLeadTrusteeTransform)}
          |            },
          |            {
          |                "AddTrusteeIndTransform": ${Json.toJson(addTrusteeTransform)}
          |            },
          |            {
          |               "RemoveTrusteeTransform": ${Json.toJson(removeTrusteeTransform)}
          |            },
          |            {
          |               "PromoteTrusteeIndTransform": ${Json.toJson(promoteTrusteeTransform)}
          |            },
          |            {
          |               "AmendTrusteeIndTransform": ${Json.toJson(amendTrusteeIndTransform)}
          |            },
          |            {
          |               "AmendTrusteeOrgTransform": ${Json.toJson(amendTrusteeOrgTransform)}
          |            },
          |            {
          |               "AmendIndividualBeneficiaryTransform": ${Json.toJson(amendIndividualBenTransform)}
          |            }
          |        ]
          |    }
          |""".stripMargin)

      val data = ComposedDeltaTransform(Seq(
          amendLeadTrusteeTransform,
          addTrusteeTransform,
          removeTrusteeTransform,
          promoteTrusteeTransform,
          amendTrusteeIndTransform,
          amendTrusteeOrgTransform,
          amendIndividualBenTransform
        )
      )

      Json.toJson(data) mustEqual json
      json.as[ComposedDeltaTransform] mustEqual data
    }
  }
}