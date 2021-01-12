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

import models.variation._
import models.{AddressType, NameType}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsBoolean, JsString, Json}
import transformers.beneficiaries._
import transformers.otherindividuals._
import transformers.protectors._
import transformers.settlors._
import transformers.trustdetails._
import transformers.trustees._

import java.time.LocalDate

class DeltaTransformSpec extends FreeSpec with MustMatchers {

  "DeltaTransform" - {

    "serialise and deserialise json correctly" in {
      val genericOriginalData = Json.obj("originalKeys" -> "originalData")
      val genericAmendedData = Json.obj("newKeys" -> "newData")
      val amendedDate = LocalDate.of(2012, 3, 14)
      val currentDate = LocalDate.of(2020, 4, 1)

      val newLeadTrustee = AmendedLeadTrusteeIndType(
        NameType("New", Some("lead"), "Trustee"),
        LocalDate.parse("2000-01-01"),
        "",
        None,
        IdentificationType(None, None, None, None),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None
      )

      val newLeadTrusteeOrg = AmendedLeadTrusteeOrgType(
        "Organisation",
        "phoneNumber",
        None,
        IdentificationOrgType(
          Some("utr"), None, None
        ),
        countryOfResidence = None
      )

      val newTrusteeInd = TrusteeIndividualType(
        Some("lineNo"),
        Some("bpMatchStatus"),
        NameType("New", None, "Trustee"),
        Some(LocalDate.parse("2000-01-01")),
        Some("phoneNumber"),
        Some(IdentificationType(Some("nino"), None, None, None)),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        LocalDate.parse("2000-01-01"),
        None
      )

      val newTrusteeOrg = TrusteeOrgType(
        Some("lineNo"),
        Some("bpMatchStatus"),
        "New Trustee",
        Some("phoneNumber"),
        Some("email"),
        Some(IdentificationOrgType(Some("utr"), None, None)),
        countryOfResidence = None,
        LocalDate.parse("2000-01-01"),
        None
      )

      val individualBeneficiary = IndividualDetailsType(
        lineNo = None,
        bpMatchStatus = None,
        NameType("Amended New First 3", None, "Amended New Last 3"),
        dateOfBirth = None,
        vulnerableBeneficiary = Some(true),
        None,
        None,
        None,
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
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None)),
        None,
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
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None
        )),
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val settlor = Settlor(
        None,
        None,
        NameType("Individual", None, "Settlor"),
        Some(LocalDate.parse("2000-01-01")),
        Some(IdentificationType(
          Some("nino"),
          None,
          None,
          None
        )),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val protector = Protector(
        None,
        None,
        NameType("Individual", None, "Settlor"),
        Some(LocalDate.parse("2000-01-01")),
        Some(IdentificationType(
          Some("nino"),
          None,
          None,
          None
        )),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val newCompanyProtector = ProtectorCompany(
        name = "TestCompany",
        identification = None,
        lineNo = None,
        bpMatchStatus = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      val otherIndividual = NaturalPersonType(
        None,
        None,
        NameType("Individual", None, "Settlor"),
        Some(LocalDate.parse("2000-01-01")),
        Some(IdentificationType(
          Some("nino"),
          None,
          None,
          None
        )),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val addTrustBeneficiaryTransform = AddBeneficiaryTransform(Json.toJson(newTrustBeneficiary), "trust")

      val amendLeadTrusteeIndTransform = AmendTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), LocalDate.now(), "leadTrusteeInd")

      val amendLeadTrusteeOrgTransform = AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeOrg), Json.obj(), LocalDate.now(), "leadTrusteeOrg")

      val addTrusteeIndTransform = AddTrusteeTransform(Json.toJson(newTrusteeInd), "trusteeInd")

      val addTrusteeOrgTransform = AddTrusteeTransform(Json.toJson(newTrusteeOrg), "trusteeOrg")

      val removeTrusteeTransform = RemoveTrusteeTransform(index = Some(0), Json.obj(), endDate = LocalDate.parse("2010-01-01"), "trusteeInd")

      val promoteTrusteeIndTransform = PromoteTrusteeTransform(Some(2), Json.toJson(newLeadTrustee), Json.obj(), LocalDate.parse("2012-02-06"), "trusteeInd")

      val promoteTrusteeOrgTransform = PromoteTrusteeTransform(Some(2), Json.toJson(newLeadTrusteeOrg), Json.obj(), LocalDate.parse("2012-02-06"), "trusteeOrg")

      val amendTrusteeIndTransform = AmendTrusteeTransform(Some(0), Json.toJson(newTrusteeInd), Json.obj(), currentDate, "trusteeInd")
      val amendTrusteeOrgTransform = AmendTrusteeTransform(Some(0), Json.toJson(newTrusteeOrg), Json.obj(), currentDate, "trusteeOrg")

      val amendCharityBeneficiaryTransform = AmendBeneficiaryTransform(Some(0), genericAmendedData, genericOriginalData, amendedDate, "charity")
      val amendCompanyBeneficiaryTransform = AmendBeneficiaryTransform(Some(0), genericAmendedData, genericOriginalData, amendedDate, "company")
      val amendOtherBeneficiaryTransform = AmendBeneficiaryTransform(Some(0), genericAmendedData, genericOriginalData, amendedDate, "other")
      val amendTrustBeneficiaryTransform = AmendBeneficiaryTransform(Some(0), genericAmendedData, genericOriginalData, amendedDate, "trust")
      val amendUnidentifiedBeneficiaryTransform = AmendBeneficiaryTransform(Some(0), JsString("New Description"), genericOriginalData, amendedDate, "unidentified")

      val amendIndividualBenTransform = AmendBeneficiaryTransform(Some(0), Json.toJson(individualBeneficiary), Json.obj(), LocalDate.parse("2020-03-25"), "individualDetails")

      val addUnidentifiedBeneficiaryTransform = AddBeneficiaryTransform(
        Json.toJson(UnidentifiedType(None, None, "desc", None, None, LocalDate.parse("2010-10-10"), None)),
        "unidentified"
      )

      val addIndividualBeneficiaryTransform = AddBeneficiaryTransform(Json.toJson(individualBeneficiary), "individualDetails")

      val addCharityBeneficiaryTransform = AddBeneficiaryTransform(
        Json.toJson(BeneficiaryCharityType(
          None, None, "New Organisation Name", Some(true),
          None, None, None, LocalDate.parse("2010-02-23"), None
        )),
        "charity"
      )

      val addOtherBeneficiaryTransform = AddBeneficiaryTransform(
        Json.toJson(OtherType(
          None, None, "description", None, None, None, None, LocalDate.parse("2010-02-23"), None
        )),
        "other"
      )

      val addCompanyBeneficiaryTransform = AddBeneficiaryTransform(
        Json.toJson(BeneficiaryCompanyType(None, None, "Organisation", None, None, None, None, LocalDate.parse("2010-02-23"), None)),
        "company"
      )

      val addLargeBeneficiaryTransform = AddBeneficiaryTransform(Json.toJson(newLargeBeneficiary), "large")

      val amendLargeBeneficiaryTransform = AmendBeneficiaryTransform(Some(0), Json.toJson(newLargeBeneficiary), Json.obj(), currentDate, "large")

      val removeBeneficiariesTransform = RemoveBeneficiaryTransform(Some(3), Json.toJson(individualBeneficiary), LocalDate.parse("2012-02-06"), "unidentified")

      val amendIndividualSettlorTransform = AmendSettlorTransform(Some(0), Json.obj(), Json.obj(), LocalDate.parse("2020-03-25"), "settlor")

      val amendBusinessSettlorTransform = AmendSettlorTransform(Some(0), Json.obj(), Json.obj(), LocalDate.parse("2020-03-25"), "settlorCompany")

      val removeSettlorsTransform = RemoveSettlorTransform(Some(3), Json.toJson(settlor), LocalDate.parse("2012-02-06"), "settlor")

      val amendDeceasedSettlorTransform = AmendSettlorTransform(None, Json.obj(), Json.obj(), LocalDate.now(), "deceased")

      val addIndividualSettlorTransform = AddSettlorTransform(Json.toJson(settlor), "settlor")

      val addIndividualProtectorTransform = AddProtectorTransform(Json.toJson(protector), "protector")

      val removeProtectorsTransform = RemoveProtectorTransform(Some(3), Json.toJson(protector), LocalDate.parse("2012-02-06"), "protector")
      val addCompanyProtectorTransform = AddProtectorTransform(Json.toJson(newCompanyProtector), "protectorCompany")

      val amendBusinessProtectorTransform = AmendProtectorTransform(Some(0), Json.toJson(newCompanyProtector), Json.obj(), LocalDate.parse("2020-03-25"), "protectorCompany")

      val removeOtherIndividualsTransform = RemoveOtherIndividualTransform(Some(3), Json.toJson(otherIndividual), LocalDate.parse("2012-02-06"))

      val amendOtherIndividualTransform = AmendOtherIndividualTransform(Some(0), Json.toJson(otherIndividual), Json.obj(), LocalDate.parse("2020-03-25"))

      val addOtherIndividualTransform = AddOtherIndividualTransform(Json.toJson(otherIndividual))

      val setExpressTransform = SetTrustDetailTransform(JsBoolean(true), "expressTrust")

      val setPropertyTransform = SetTrustDetailTransform(JsBoolean(true), "trustUKProperty")

      val setRecordedTransform = SetTrustDetailTransform(JsBoolean(true), "trustRecorded")

      val setResidentTransform = SetTrustDetailTransform(JsBoolean(true), "trustUKResident")

      val setTaxableTransform = SetTrustDetailTransform(JsBoolean(true), "trustTaxable")

      val setUKRelationTransform = SetTrustDetailTransform(JsBoolean(true), "trustUKRelation")

      val json = Json.parse(
        s"""{
          |        "deltaTransforms" : [
          |            {
          |               "AmendTrusteeTransform": ${Json.toJson(amendLeadTrusteeIndTransform)}
          |            },
          |            {
          |               "AmendTrusteeTransform": ${Json.toJson(amendLeadTrusteeOrgTransform)}
          |            },
          |            {
          |               "AddTrusteeTransform": ${Json.toJson(addTrusteeIndTransform)}
          |            },
          |            {
          |               "AddTrusteeTransform": ${Json.toJson(addTrusteeOrgTransform)}
          |            },
          |            {
          |               "RemoveTrusteeTransform": ${Json.toJson(removeTrusteeTransform)}
          |            },
          |            {
          |               "PromoteTrusteeTransform": ${Json.toJson(promoteTrusteeIndTransform)}
          |            },
          |            {
          |               "PromoteTrusteeTransform": ${Json.toJson(promoteTrusteeOrgTransform)}
          |            },
          |            {
          |               "AmendTrusteeTransform": ${Json.toJson(amendTrusteeIndTransform)}
          |            },
          |            {
          |               "AmendTrusteeTransform": ${Json.toJson(amendTrusteeOrgTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendIndividualBenTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addTrustBeneficiaryTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addUnidentifiedBeneficiaryTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addIndividualBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendCharityBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendCompanyBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendOtherBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendTrustBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendUnidentifiedBeneficiaryTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addCharityBeneficiaryTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addOtherBeneficiaryTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addCompanyBeneficiaryTransform)}
          |            },
          |            {
          |               "AddBeneficiaryTransform": ${Json.toJson(addLargeBeneficiaryTransform)}
          |            },
          |            {
          |               "AmendBeneficiaryTransform": ${Json.toJson(amendLargeBeneficiaryTransform)}
          |            },
          |            {
          |               "RemoveBeneficiaryTransform": ${Json.toJson(removeBeneficiariesTransform)}
          |            },
          |            {
          |               "AmendSettlorTransform": ${Json.toJson(amendIndividualSettlorTransform)}
          |            },
          |            {
          |               "AmendSettlorTransform": ${Json.toJson(amendBusinessSettlorTransform)}
          |            },
          |            {
          |               "RemoveSettlorTransform": ${Json.toJson(removeSettlorsTransform)}
          |            },
          |            {
          |               "AmendSettlorTransform": ${Json.toJson(amendDeceasedSettlorTransform)}
          |            },
          |            {
          |               "AddSettlorTransform": ${Json.toJson(addIndividualSettlorTransform)}
          |            },
          |            {
          |               "AddProtectorTransform": ${Json.toJson(addIndividualProtectorTransform)}
          |            },
          |            {
          |               "RemoveProtectorTransform": ${Json.toJson(removeProtectorsTransform)}
          |            },
          |            {
          |               "AddProtectorTransform": ${Json.toJson(addCompanyProtectorTransform)}
          |            },
          |            {
          |               "AmendProtectorTransform": ${Json.toJson(amendBusinessProtectorTransform)}
          |            },
          |            {
          |               "RemoveOtherIndividualTransform": ${Json.toJson(removeOtherIndividualsTransform)}
          |            },
          |            {
          |               "AmendOtherIndividualTransform": ${Json.toJson(amendOtherIndividualTransform)}
          |            },
          |            {
          |               "AddOtherIndividualTransform": ${Json.toJson(addOtherIndividualTransform)}
          |            },
          |            {
          |               "SetTrustDetailTransform": ${Json.toJson(setExpressTransform)}
          |            },
          |            {
          |               "SetTrustDetailTransform": ${Json.toJson(setPropertyTransform)}
          |            },
          |            {
          |               "SetTrustDetailTransform": ${Json.toJson(setRecordedTransform)}
          |            },
          |            {
          |               "SetTrustDetailTransform": ${Json.toJson(setResidentTransform)}
          |            },
          |            {
          |               "SetTrustDetailTransform": ${Json.toJson(setTaxableTransform)}
          |            },
          |            {
          |               "SetTrustDetailTransform": ${Json.toJson(setUKRelationTransform)}
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
          amendBusinessProtectorTransform,
          removeOtherIndividualsTransform,
          amendOtherIndividualTransform,
          addOtherIndividualTransform,
          setExpressTransform,
          setPropertyTransform,
          setRecordedTransform,
          setResidentTransform,
          setTaxableTransform,
          setUKRelationTransform
        )
      )

      Json.toJson(data) mustEqual json
      json.as[ComposedDeltaTransform] mustEqual data
    }
  }
}