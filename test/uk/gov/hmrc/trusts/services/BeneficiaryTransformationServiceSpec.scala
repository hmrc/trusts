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

package uk.gov.hmrc.trusts.services

import java.time.LocalDate

import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.{BeneficiaryCharityType, BeneficiaryCompanyType, BeneficiaryTrustType, IdentificationType, IndividualDetailsType, OtherType, UnidentifiedType}
import uk.gov.hmrc.trusts.models.{AddressType, IdentificationOrgType, NameType, RemoveBeneficiary}
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BeneficiaryTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {
   private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private implicit val hc : HeaderCarrier = HeaderCarrier()

  private def beneficiaryJson(value1 : String, endDate: Option[LocalDate] = None) = {
    if (endDate.isDefined) {
      Json.obj("field1" -> value1, "field2" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> value1, "field2" -> "value20", "lineNo" -> 65)
    }
  }

  object LocalDateMock extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json
      .put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }


  "The beneficiary transformation service" - {

    "must add a new remove beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val beneficiary = beneficiaryJson("Blah Blah Blah")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))
      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("individualDetails", Seq(beneficiary)),
          ResponseHeader("status", "formBundlNo")
        )))

      val result = service.removeBeneficiary("utr", "internalId", RemoveBeneficiary(LocalDate.of(2013, 2, 20), 0, "individualDetails"))
      whenReady(result) { _ =>
        verify(transformationService).addNewTransform("utr",
          "internalId", RemoveBeneficiariesTransform(0, beneficiary, LocalDate.of(2013, 2, 20), "individualDetails"))
      }
    }

    "must add a new amend unidentified beneficiary transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newDescription = "Some Description"
      val originalBeneficiaryJson = Json.toJson(UnidentifiedType(
        None,
        None,
        "Some description",
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      ))

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("unidentified", Seq(originalBeneficiaryJson)),
          ResponseHeader("status", "formBundlNo")
        )))

      val result = service.amendUnidentifiedBeneficiaryTransformer("utr", index, "internalId", newDescription)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendUnidentifiedBeneficiaryTransform(index, newDescription, originalBeneficiaryJson, LocalDateMock.now))
      }
    }

    "must add a new add unidentified beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newBeneficiary = UnidentifiedType(
        None,
        None,
        "Some description",
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addUnidentifiedBeneficiaryTransformer("utr", "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddUnidentifiedBeneficiaryTransform(newBeneficiary))
      }
    }

    "must add a new amend individual beneficiary transform using the transformation service" in {
        val index = 0
        val transformationService = mock[TransformationService]
        val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
        val newIndividual = IndividualDetailsType(
          None,
          None,
          NameType("First", None, "Last"),
          None,
          vulnerableBeneficiary = false,
          None,
          None,
          None,
          None,
          LocalDate.parse("2010-01-01"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo": "1",
            |  "bpMatchStatus": "01",
            |  "name": {
            |    "firstName": "First 2",
            |    "lastName": "Last 2"
            |  },
            |  "vulnerableBeneficiary": true,
            |  "identification": {
            |    "nino": "JP1212122A"
            |  },
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any())(any()))
          .thenReturn(Future.successful(TrustProcessedResponse(
            buildInputJson("individualDetails", Seq(original)),
            ResponseHeader("status", "formBundlNo")
          )))

        val result = service.amendIndividualBeneficiaryTransformer("utr", index, "internalId", newIndividual)
        whenReady(result) { _ =>

          verify(transformationService).addNewTransform(
            Matchers.eq("utr"),
            Matchers.eq("internalId"),
            Matchers.eq(AmendIndividualBeneficiaryTransform(index, Json.toJson(newIndividual), original, LocalDateMock.now))
          )
      }
    }

    "must add a new add individual beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newBeneficiary = IndividualDetailsType(None,
        None,
        NameType("First", None, "Last"),
        Some(LocalDate.parse("2000-01-01")),
        vulnerableBeneficiary = false,
        None,
        None,
        None,
        Some(IdentificationType(Some("nino"), None, None, None)),
        LocalDate.parse("1990-10-10"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addIndividualBeneficiaryTransformer("utr", "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddIndividualBeneficiaryTransform(newBeneficiary))
      }
    }

    "must add a new add charity beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newBeneficiary = BeneficiaryCharityType(
        None,
        None,
        "Charity",
        Some(false),
        Some("50"),
        Some(IdentificationOrgType(None, Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")))),
        LocalDate.parse("1990-10-10"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addCharityBeneficiaryTransformer("utr", "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddCharityBeneficiaryTransform(newBeneficiary))
      }
    }

    "must add a new amend charity beneficiary transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newCharity = BeneficiaryCharityType(
        None,
        None,
        "Charity Name",
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val original: JsValue = Json.parse(
        """
          |{
          |  "lineNo": "1",
          |  "bpMatchStatus": "01",
          |  "name": "Original name",
          |  "identification": {
          |    "utr": "1234567890"
          |  },
          |  "entityStart": "2018-02-28"
          |}
          |""".stripMargin)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("charity", Seq(original)),
          ResponseHeader("status", "formBundleNo")
        )))

      val result = service.amendCharityBeneficiaryTransformer("utr", index, "internalId", newCharity)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          Matchers.eq("utr"),
          Matchers.eq("internalId"),
          Matchers.eq(AmendCharityBeneficiaryTransform(index, Json.toJson(newCharity), original, LocalDateMock.now))
        )
      }
    }

    "must add a new add other beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newBeneficiary = OtherType(
        None,
        None,
        "Other",
        Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
        Some(false),
        None,
        LocalDate.parse("1990-10-10"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addOtherBeneficiaryTransformer("utr", "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddOtherBeneficiaryTransform(newBeneficiary))
      }
    }
    "must add a new add company beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newBeneficiary = BeneficiaryCompanyType(
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

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addCompanyBeneficiaryTransformer("utr", "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddCompanyBeneficiaryTransform(newBeneficiary))
      }
    }

    "must add a new amend company beneficiary transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
      val newCompany = BeneficiaryCompanyType(
        None,
        None,
        "Company Name",
        None,
        None,
        None,
        LocalDate.parse("2010-01-01"),
        None
      )

      val original: JsValue = Json.parse(
        """
          |{
          |  "lineNo": "1",
          |  "bpMatchStatus": "01",
          |  "name": "Original name",
          |  "identification": {
          |    "utr": "1234567890"
          |  },
          |  "entityStart": "2018-02-28"
          |}
          |""".stripMargin)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("company", Seq(original)),
          ResponseHeader("status", "formBundleNo")
        )))

      val result = service.amendCompanyBeneficiaryTransformer("utr", index, "internalId", newCompany)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          Matchers.eq("utr"),
          Matchers.eq("internalId"),
          Matchers.eq(AmendCompanyBeneficiaryTransform(index, Json.toJson(newCompany), original, LocalDateMock.now))
        )
      }
    }

  "must add a new add trust beneficiary transform using the transformation service" in {
    val transformationService = mock[TransformationService]
    val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
    val newBeneficiary = BeneficiaryTrustType(
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

    when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

    val result = service.addTrustBeneficiaryTransformer("utr", "internalId", newBeneficiary)
    whenReady(result) { _ =>

      verify(transformationService).addNewTransform("utr",
        "internalId", AddTrustBeneficiaryTransform(newBeneficiary))
    }
  }

  "must add a new amend other beneficiary transform using the transformation service" in {
    val index = 0
    val transformationService = mock[TransformationService]
    val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
    val newBeneficiary = OtherType(
      None,
      None,
      "Other",
      Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
      Some(false),
      None,
      LocalDate.parse("1990-10-10"),
      None
    )

      val original: JsValue = Json.parse(
        """
          |{
          |  "lineNo": "1",
          |  "bpMatchStatus": "01",
          |  "description":"Other 1",
          |  "address":{
          |    "line1":"House 1",
          |    "line2":"Street 2",
          |    "postCode":"NE1 1NE",
          |    "country":"GB"
          |  },
          |  "entityStart":"2018-02-12"
          |}
          |""".stripMargin)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("other", Seq(original)),
          ResponseHeader("status", "formBundleNo")
        )))

      val result = service.amendOtherBeneficiaryTransformer("utr", index, "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          Matchers.eq("utr"),
          Matchers.eq("internalId"),
          Matchers.eq(AmendOtherBeneficiaryTransform(index, Json.toJson(newBeneficiary), original, LocalDateMock.now))
        )
      }
    }
  }

  "must add a new amend trust beneficiary transform using the transformation service" in {
    val index = 0
    val transformationService = mock[TransformationService]
    val service = new BeneficiaryTransformationService(transformationService, LocalDateMock)
    val newTrust = BeneficiaryTrustType(
      None,
      None,
      "Trust Name",
      None,
      None,
      None,
      LocalDate.parse("2010-01-01"),
      None
    )

    val original: JsValue = Json.parse(
      """
        |{
        |  "lineNo": "1",
        |  "bpMatchStatus": "01",
        |  "name": "Original name",
        |  "identification": {
        |    "utr": "1234567890"
        |  },
        |  "entityStart": "2018-02-28"
        |}
        |""".stripMargin)

    when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

    when(transformationService.getTransformedData(any(), any())(any()))
      .thenReturn(Future.successful(TrustProcessedResponse(
        buildInputJson("trust", Seq(original)),
        ResponseHeader("status", "formBundleNo")
      )))

    val result = service.amendTrustBeneficiaryTransformer("utr", index, "internalId", newTrust)
    whenReady(result) { _ =>

      verify(transformationService).addNewTransform(
        Matchers.eq("utr"),
        Matchers.eq("internalId"),
        Matchers.eq(AmendTrustBeneficiaryTransform(index, Json.toJson(newTrust), original, LocalDateMock.now))
      )
    }
  }
}
