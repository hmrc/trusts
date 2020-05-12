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

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation._
import uk.gov.hmrc.trusts.models.{AddressType, IdentificationOrgType, NameType}

class DeltaTransformSpec extends FreeSpec with MustMatchers {

  "DeltaTransform" - {

    "serialise and deserialise json correctly" in {
      val genericOriginalData = Json.obj("originalKeys" -> "originalData")
      val genericAmendedData = Json.obj("newKeys" -> "newData")
      val amendedDate = LocalDate.of(2012, 3, 14)
      val currentDate = LocalDate.of(2020, 4, 1)

      val newLeadTrustee = DisplayTrustLeadTrusteeIndType(
        None,
        None,
        NameType("New", Some("lead"), "Trustee"),
        LocalDate.parse("2000-01-01"),
        "",
        None,
        DisplayTrustIdentificationType(None, None, None, None),
        None
      )

      val newLeadTrusteeOrg = DisplayTrustLeadTrusteeOrgType(
        None,
        None,
        "Organisation",
        "phoneNumber",
        None,
        DisplayTrustIdentificationOrgType(
          None, Some("utr"), None
        ),
        None
      )

      val newTrusteeInd = DisplayTrustTrusteeIndividualType(
        Some("lineNo"),
        Some("bpMatchStatus"),
        NameType("New", None, "Trustee"),
        Some(LocalDate.parse("2000-01-01")),
        Some("phoneNumber"),
        Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
        LocalDate.parse("2000-01-01")
      )

      val newTrusteeOrg = DisplayTrustTrusteeOrgType(
        Some("lineNo"),
        Some("bpMatchStatus"),
        "New Trustee",
        Some("phoneNumber"),
        Some("email"),
        Some(DisplayTrustIdentificationOrgType(None, Some("utr"), None)),
        LocalDate.parse("2000-01-01")
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
        LocalDate.parse("2018-02-28"),
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
        LocalDate.parse("1990-10-10"),
        None
      )

      val newLargeBeneficiary = LargeType(
        None,
        None,
        "Name",
        "Description",
        None,
        None,
        None,
        None,
        "501",
        Some(IdentificationOrgType(
          None,
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB"))
        )),
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val settlor = DisplayTrustSettlor(
        None,
        None,
        NameType("Individual", None, "Settlor"),
        Some(LocalDate.parse("2000-01-01")),
        Some(DisplayTrustIdentificationType(
          None,
          Some("nino"),
          None,
          None
        )),
        LocalDate.parse("2010-01-01")
      )

      val protector = DisplayTrustProtector(
        None,
        None,
        NameType("Individual", None, "Settlor"),
        Some(LocalDate.parse("2000-01-01")),
        Some(DisplayTrustIdentificationType(
          None,
          Some("nino"),
          None,
          None
        )),
        LocalDate.parse("2010-01-01")
      )

      val newCompanyProtector = DisplayTrustProtectorCompany(
        name = "TestCompany",
        identification = None,
        lineNo = None,
        bpMatchStatus = None,
        entityStart = LocalDate.parse("2010-05-03")
      )

      val addTrustBeneficiaryTransform = AddTrustBeneficiaryTransform(newTrustBeneficiary)

      val amendLeadTrusteeIndTransform = AmendLeadTrusteeIndTransform(newLeadTrustee)

      val amendLeadTrusteeOrgTransform = AmendLeadTrusteeOrgTransform(
        newLeadTrusteeOrg
      )

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
        Json.obj(),
        currentDate
      )

      val promoteTrusteeOrgTransform = PromoteTrusteeOrgTransform(
        2,
        newLeadTrusteeOrg,
        LocalDate.parse("2012-02-06"),
        Json.obj(),
        currentDate
      )

      val amendTrusteeIndTransform = AmendTrusteeIndTransform(0, newTrusteeInd, Json.obj(), currentDate)
      val amendTrusteeOrgTransform = AmendTrusteeOrgTransform(0, newTrusteeOrg, Json.obj(), currentDate)

      val amendCharityBeneficiaryTransform = AmendCharityBeneficiaryTransform(0, genericAmendedData, genericOriginalData, amendedDate)
      val amendCompanyBeneficiaryTransform = AmendCompanyBeneficiaryTransform(0, genericAmendedData, genericOriginalData, amendedDate)
      val amendOtherBeneficiaryTransform = AmendOtherBeneficiaryTransform(0, genericAmendedData, genericOriginalData, amendedDate)
      val amendTrustBeneficiaryTransform = AmendTrustBeneficiaryTransform(0, genericAmendedData, genericOriginalData, amendedDate)
      val amendUnidentifiedBeneficiaryTransform = AmendUnidentifiedBeneficiaryTransform(0, "New Description", genericOriginalData, amendedDate)

      val amendIndividualBenTransform = AmendIndividualBeneficiaryTransform(0, Json.toJson(individualBeneficiary), Json.obj(), LocalDate.parse("2020-03-25"))

      val addUnidentifiedBeneficiaryTransform = AddUnidentifiedBeneficiaryTransform(
        UnidentifiedType(None, None, "desc", None, None, LocalDate.parse("2010-10-10"), None)
      )

      val addIndividualBeneficiaryTransform = AddIndividualBeneficiaryTransform(individualBeneficiary)

      val addCharityBeneficiaryTransform = AddCharityBeneficiaryTransform(
        BeneficiaryCharityType(
          None, None, "New Organisation Name", Some(true),
          None, None, LocalDate.parse("2010-02-23"), None
        )
      )

      val addOtherBeneficiaryTransform = AddOtherBeneficiaryTransform(
        OtherType(
          None, None, "description", None, None, None, LocalDate.parse("2010-02-23"), None
        )
      )

      val addCompanyBeneficiaryTransform = AddCompanyBeneficiaryTransform(
        BeneficiaryCompanyType(None, None, "Organisation", None, None, None, LocalDate.parse("2010-02-23"), None)
      )

      val addLargeBeneficiaryTransform = AddLargeBeneficiaryTransform(newLargeBeneficiary)

      val amendLargeBeneficiaryTransform = AmendLargeBeneficiaryTransform(0, Json.toJson(newLargeBeneficiary), Json.obj(), currentDate)

      val removeBeneficiariesTransform = RemoveBeneficiariesTransform(3, Json.toJson(individualBeneficiary), LocalDate.parse("2012-02-06"), "BeneficiaryType")

      val amendIndividualSettlorTransform = AmendIndividualSettlorTransform(0, Json.obj(), Json.obj(), LocalDate.parse("2020-03-25"))

      val amendBusinessSettlorTransform = AmendBusinessSettlorTransform(0, Json.obj(), Json.obj(), LocalDate.parse("2020-03-25"))

      val removeSettlorsTransform = RemoveSettlorsTransform(3, Json.toJson(settlor), LocalDate.parse("2012-02-06"), "settlor")

      val amendDeceasedSettlorTransform = AmendDeceasedSettlorTransform(Json.obj(), Json.obj())

      val addIndividualSettlorTransform = AddIndividualSettlorTransform(settlor)

      val addIndividualProtectorTransform = AddIndividualProtectorTransform(protector)

      val removeProtectorsTransform = RemoveProtectorsTransform(3, Json.toJson(protector), LocalDate.parse("2012-02-06"), "protector")

      val addCompanyProtectorTransform = AddCompanyProtectorTransform(newCompanyProtector)

      val amendBusinessProtectorTransform = AmendBusinessProtectorTransform(0, Json.toJson(newCompanyProtector), Json.obj(), LocalDate.parse("2020-03-25"))

      val json = Json.parse(
        s"""{
          |        "deltaTransforms" : [
          |            {
          |               "AmendLeadTrusteeIndTransform": ${Json.toJson(amendLeadTrusteeIndTransform)}
          |            },
          |            {
          |               "AmendLeadTrusteeOrgTransform": ${Json.toJson(amendLeadTrusteeOrgTransform)}
          |            },
          |            {
          |               "AddTrusteeIndTransform": ${Json.toJson(addTrusteeIndTransform)}
          |            },
          |            {
          |               "AddTrusteeOrgTransform": ${Json.toJson(addTrusteeOrgTransform)}
          |            },
          |            {
          |               "RemoveTrusteeTransform": ${Json.toJson(removeTrusteeTransform)}
          |            },
          |            {
          |               "PromoteTrusteeIndTransform": ${Json.toJson(promoteTrusteeIndTransform)}
          |            },
          |            {
          |               "PromoteTrusteeOrgTransform": ${Json.toJson(promoteTrusteeOrgTransform)}
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
          |               "AmendCompanyBeneficiaryTransform": ${Json.toJson(amendCompanyBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendOtherBeneficiaryTransform": ${Json.toJson(amendOtherBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendTrustBeneficiaryTransform": ${Json.toJson(amendTrustBeneficiaryTransform)}
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
          |            },
          |            {
          |               "AddLargeBeneficiaryTransform": ${Json.toJson(addLargeBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendLargeBeneficiaryTransform": ${Json.toJson(amendLargeBeneficiaryTransform)}
          |            },
          |            {
          |               "RemoveBeneficiariesTransform": ${Json.toJson(removeBeneficiariesTransform)}
          |            },
          |            {
          |               "AmendIndividualSettlorTransform": ${Json.toJson(amendIndividualSettlorTransform)}
          |            },
          |            {
          |               "AmendBusinessSettlorTransform": ${Json.toJson(amendBusinessSettlorTransform)}
          |            },
          |            {
          |               "RemoveSettlorsTransform": ${Json.toJson(removeSettlorsTransform)}
          |            },
          |            {
          |               "AmendDeceasedSettlorTransform": ${Json.toJson(amendDeceasedSettlorTransform)}
          |            },
          |            {
          |               "AddIndividualSettlorTransform": ${Json.toJson(addIndividualSettlorTransform)}
          |            },
          |            {
          |               "AddIndividualProtectorTransform": ${Json.toJson(addIndividualProtectorTransform)}
          |            },
          |            {
          |               "RemoveProtectorsTransform": ${Json.toJson(removeProtectorsTransform)}
          |            },
          |            {
          |               "AddCompanyProtectorTransform": ${Json.toJson(addCompanyProtectorTransform)}
          |            },
          |            {
          |               "AmendBusinessProtectorTransform": ${Json.toJson(amendBusinessProtectorTransform)}
          |            }
          |        ]
          |    }
          |""".stripMargin)

      val data = ComposedDeltaTransform(Seq(
          amendLeadTrusteeIndTransform,
          amendLeadTrusteeOrgTransform,
          addTrusteeIndTransform,
          addTrusteeOrgTransform,
          removeTrusteeTransform,
          promoteTrusteeIndTransform,
          promoteTrusteeOrgTransform,
          amendTrusteeIndTransform,
          amendTrusteeOrgTransform,
          amendIndividualBenTransform,
          addTrustBeneficiaryTransform,
          addUnidentifiedBeneficiaryTransform,
          addIndividualBeneficiaryTransform,
          amendCharityBeneficiaryTransform,
          amendCompanyBeneficiaryTransform,
          amendOtherBeneficiaryTransform,
          amendTrustBeneficiaryTransform,
          amendUnidentifiedBeneficiaryTransform,
          addCharityBeneficiaryTransform,
          addOtherBeneficiaryTransform,
          addCompanyBeneficiaryTransform,
          addLargeBeneficiaryTransform,
          amendLargeBeneficiaryTransform,
          removeBeneficiariesTransform,
          amendIndividualSettlorTransform,
          amendBusinessSettlorTransform,
          removeSettlorsTransform,
          amendDeceasedSettlorTransform,
          addIndividualSettlorTransform,
          addIndividualProtectorTransform,
          removeProtectorsTransform,
          addCompanyProtectorTransform,
          amendBusinessProtectorTransform
        )
      )

      Json.toJson(data) mustEqual json
      json.as[ComposedDeltaTransform] mustEqual data
    }
  }
}