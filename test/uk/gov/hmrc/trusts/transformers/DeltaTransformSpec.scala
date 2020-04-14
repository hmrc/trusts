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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.{BeneficiaryCharityType, BeneficiaryCompanyType, BeneficiaryTrustType, IndividualDetailsType, OtherType, UnidentifiedType}
import uk.gov.hmrc.trusts.models.{AddressType, IdentificationOrgType, NameType}

class DeltaTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "DeltaTransform" - {

    "serialise and deserialise json correctly" in {
      val genericOriginalData = Json.obj("originalKeys" -> "originalData")
      val genericAmendedData = Json.obj("newKeys" -> "newData")
      val amendedDate = LocalDate.of(2012, 3, 14)

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

      val newTrustBeneficiary = BeneficiaryTrustType(
        None,
        None,
        "Organisation Name",
        Some(false),
        Some("50"),
        Some(IdentificationOrgType(
          Some("company utr"),
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")))),
        DateTime.parse("1990-10-10"),
        None
      )

      val addTrustBeneficiaryTransform = AddTrustBeneficiaryTransform(newTrustBeneficiary)

      val amendLeadTrusteeIndTransform = AmendLeadTrusteeIndTransform(newLeadTrustee)

      val addTrusteeIndTransform = AddTrusteeIndTransform(newTrusteeInd)

      val addTrusteeOrgTransform = AddTrusteeOrgTransform(newTrusteeOrg)

      val removeTrusteeTransform = RemoveTrusteeTransform(
        endDate = LocalDate.parse("2010-01-01"),
        index = 0,
        Json.obj()
      )

      val promoteTrusteeIndTransform = PromoteTrusteeIndTransform(
        2,
        newLeadTrustee,
        LocalDate.parse("2012-02-06"),
        Json.obj()
      )

      val amendTrusteeIndTransform = AmendTrusteeIndTransform(0, newTrusteeInd, Json.obj())
      val amendTrusteeOrgTransform = AmendTrusteeOrgTransform(0, newTrusteeOrg, Json.obj())

      val amendCharityBeneficiaryTransform = AmendCharityBeneficiaryTransform(0, genericAmendedData, genericOriginalData, amendedDate)
      val amendOtherBeneficiaryTransform = AmendOtherBeneficiaryTransform(0, genericAmendedData, genericOriginalData, amendedDate)
      val amendUnidentifiedBeneficiaryTransform = AmendUnidentifiedBeneficiaryTransform(0, "New Description", genericOriginalData, amendedDate)

      val amendIndividualBenTransform = AmendIndividualBeneficiaryTransform(0, Json.toJson(individualBeneficiary), Json.obj(), LocalDate.parse("2020-03-25"))

      val addUnidentifiedBeneficiaryTransform = AddUnidentifiedBeneficiaryTransform(
        UnidentifiedType(None, None, "desc", None, None, DateTime.parse("2010-10-10"), None)
      )

      val addIndividualBeneficiaryTransform = AddIndividualBeneficiaryTransform(individualBeneficiary)

      val addCharityBeneficiaryTransform = AddCharityBeneficiaryTransform(
        BeneficiaryCharityType(
          None, None, "New Organisation Name", Some(true),
          None, None, DateTime.parse("2010-02-23"), None
        )
      )

      val addOtherBeneficiaryTransform = AddOtherBeneficiaryTransform(
        OtherType(
          None, None, "description", None, None, None, DateTime.parse("2010-02-23"), None
        )
      )

      val addCompanyBeneficiaryTransform = AddCompanyBeneficiaryTransform(
        BeneficiaryCompanyType(None, None, "Organisation", None, None, None, DateTime.parse("2010-02-23"), None)
      )

      val json = Json.parse(
        s"""{
          |        "deltaTransforms" : [
          |            {
          |                "AmendLeadTrusteeIndTransform": ${Json.toJson(amendLeadTrusteeIndTransform)}
          |            },
          |            {
          |                "AddTrusteeIndTransform": ${Json.toJson(addTrusteeIndTransform)}
          |            },
          |            {
          |                "AddTrusteeOrgTransform": ${Json.toJson(addTrusteeOrgTransform)}
          |            },
          |            {
          |               "RemoveTrusteeTransform": ${Json.toJson(removeTrusteeTransform)}
          |            },
          |            {
          |               "PromoteTrusteeIndTransform": ${Json.toJson(promoteTrusteeIndTransform)}
          |            },
          |            {
          |               "AmendTrusteeIndTransform": ${Json.toJson(amendTrusteeIndTransform)}
          |            },
          |            {
          |               "AmendTrusteeOrgTransform": ${Json.toJson(amendTrusteeOrgTransform)}
          |            },
          |            {
          |               "AmendIndividualBeneficiaryTransform": ${Json.toJson(amendIndividualBenTransform)}
          |            },
          |            {
          |               "AddTrustBeneficiaryTransform": ${Json.toJson(addTrustBeneficiaryTransform)}
          |            },
          |            {
          |               "AddUnidentifiedBeneficiaryTransform": ${Json.toJson(addUnidentifiedBeneficiaryTransform)}
          |            },
          |            {
          |               "AddIndividualBeneficiaryTransform": ${Json.toJson(addIndividualBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendCharityBeneficiaryTransform": ${Json.toJson(amendCharityBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendOtherBeneficiaryTransform": ${Json.toJson(amendOtherBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendUnidentifiedBeneficiaryTransform": ${Json.toJson(amendUnidentifiedBeneficiaryTransform)}
          |            },
          |            {
          |               "AddCharityBeneficiaryTransform": ${Json.toJson(addCharityBeneficiaryTransform)}
          |            },
          |            {
          |               "AddOtherBeneficiaryTransform": ${Json.toJson(addOtherBeneficiaryTransform)}
          |            },
          |            {
          |               "AddCompanyBeneficiaryTransform": ${Json.toJson(addCompanyBeneficiaryTransform)}
          |            }
          |        ]
          |    }
          |""".stripMargin)

      val data = ComposedDeltaTransform(Seq(
          amendLeadTrusteeIndTransform,
          addTrusteeIndTransform,
          addTrusteeOrgTransform,
          removeTrusteeTransform,
          promoteTrusteeIndTransform,
          amendTrusteeIndTransform,
          amendTrusteeOrgTransform,
          amendIndividualBenTransform,
          addTrustBeneficiaryTransform,
          addUnidentifiedBeneficiaryTransform,
          addIndividualBeneficiaryTransform,
          amendCharityBeneficiaryTransform,
          amendOtherBeneficiaryTransform,
          amendUnidentifiedBeneficiaryTransform,
          addCharityBeneficiaryTransform,
          addOtherBeneficiaryTransform,
          addCompanyBeneficiaryTransform
        )
      )

      Json.toJson(data) mustEqual json
      json.as[ComposedDeltaTransform] mustEqual data
    }
  }
}