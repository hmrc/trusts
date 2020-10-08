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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.{AmendDeceasedSettlor, SettlorCompany, IdentificationOrgType}
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SettlorTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {

  private implicit val pc: PatienceConfig =
    PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private def settlorJson(value1: String, endDate: Option[LocalDate] = None) = {
    if (endDate.isDefined) {
      Json.obj("field1" -> value1, "field2" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> value1, "field2" -> "value20", "lineNo" -> 65)
    }
  }

  object LocalDateMock extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  def buildInputJson(settlorType: String, data: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "settlors" \ settlorType).json
      .put(JsArray(data))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "The settlor transformation service" - {

    "must add a new remove settlor transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new SettlorTransformationService(transformationService, LocalDateMock)
      val settlor = settlorJson("Blah Blah Blah")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))
      when(transformationService.getTransformedData(any(), any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("settlor", Seq(settlor)),
          ResponseHeader("status", "formBundlNo")
        )))

      val result = service.removeSettlor("utr", "internalId", RemoveSettlor(LocalDate.of(2013, 2, 20), 0, "settlor"))
      whenReady(result) { _ =>
        verify(transformationService).addNewTransform("utr",
          "internalId", RemoveSettlorsTransform(0, settlor, LocalDate.of(2013, 2, 20), "settlor"))
      }
    }

    "must add a new add individual settlor transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new SettlorTransformationService(transformationService, LocalDateMock)
      val newSettlor = DisplayTrustSettlor(
        None,
        None,
        NameType("First", None, "Last"),
        None,
        None,
        LocalDate.parse("1990-10-10")
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addIndividualSettlorTransformer("utr", "internalId", newSettlor)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddIndividualSettlorTransform(newSettlor))
      }
    }

    "must add a new add business settlor transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new SettlorTransformationService(transformationService, LocalDateMock)
      val newCompanySettlor = SettlorCompany(
        None,
        None,
        "Organisation Name",
        None,
        Some(false),
        Some(IdentificationOrgType(
          None,
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")), None)),
        LocalDate.parse("1990-10-10"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addBusinessSettlorTransformer("utr", "internalId", newCompanySettlor)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddBusinessSettlorTransform(newCompanySettlor))
      }
    }

      "must add a new amend individual settlor transform" in {
        val index = 0
        val transformationService = mock[TransformationService]
        val service = new SettlorTransformationService(transformationService, LocalDateMock)

        val newSettlor = variation.Settlor(
          lineNo = None,
          bpMatchStatus = None,
          name = NameType("First", None, "Last"),
          dateOfBirth = None,
          identification = None,
          entityStart = LocalDate.parse("2010-05-03"),
          entityEnd = None
        )

        val originalSettlorJson = Json.toJson(
          variation.Settlor(
            lineNo = None,
            bpMatchStatus = None,
            name = NameType("Old", None, "Last"),
            dateOfBirth = Some(LocalDate.parse("1990-02-01")),
            identification = None,
            entityStart = LocalDate.parse("2010-05-03"),
            entityEnd = None
          )
        )

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustProcessedResponse(
            buildInputJson("settlor", Seq(originalSettlorJson)),
            ResponseHeader("status", "formBundlNo")
          )))

        val result = service.amendIndividualSettlorTransformer("utr", index, "internalId", newSettlor)
        whenReady(result) { _ =>

          verify(transformationService).addNewTransform(
            "utr",
            "internalId",
            AmendIndividualSettlorTransform(index, Json.toJson(newSettlor), originalSettlorJson, LocalDateMock.now)
          )
        }
      }
    }

    "must add a new amend business settlor transform" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new SettlorTransformationService(transformationService, LocalDateMock)

      val newSettlor = variation.SettlorCompany(
        lineNo = None,
        bpMatchStatus = None,
        name = "Company Ltd",
        companyType = None,
        companyTime = None,
        identification = None,
        entityStart = LocalDate.parse("2010-05-03"),
        entityEnd = None
      )

      val originalSettlorJson = Json.toJson(
        variation.SettlorCompany(
          lineNo = None,
          bpMatchStatus = None,
          name = "Company",
          companyTime = None,
          companyType = None,
          identification = None,
          entityStart = LocalDate.parse("2010-05-03"),
          entityEnd = None
        )
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      when(transformationService.getTransformedData(any(), any()))
        .thenReturn(
          Future.successful(TrustProcessedResponse(buildInputJson("settlorCompany", Seq(originalSettlorJson)),
          ResponseHeader("status", "formBundlNo")
        )))

      val result = service.amendBusinessSettlorTransformer("utr", index, "internalId", newSettlor)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          "utr",
          "internalId",
          AmendBusinessSettlorTransform(index, Json.toJson(newSettlor), originalSettlorJson, LocalDateMock.now)
        )
      }
    }

    "must add a new amend deceased settlor transform" in {
      val transformationService = mock[TransformationService]
      val service = new SettlorTransformationService(transformationService, LocalDateMock)

      val amendedSettlor = AmendDeceasedSettlor(
        name = NameType("First", None, "Last"),
        dateOfBirth = None,
        dateOfDeath = None,
        identification = None
      )

      val originalDeceased = Json.parse(
        """
          |{
          |  "lineNo":"1",
          |  "bpMatchStatus": "01",
          |  "name":{
          |    "firstName":"John",
          |    "middleName":"William",
          |    "lastName":"O'Connor"
          |  },
          |  "dateOfBirth":"1956-02-12",
          |  "dateOfDeath":"2016-01-01",
          |  "identification":{
          |    "nino":"KC456736"
          |  },
          |  "entityStart":"1998-02-12"
          |}
          |""".stripMargin)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      when(transformationService.getTransformedData(any(), any()))
        .thenReturn(
          Future.successful(
            TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
          ))

      val result = service.amendDeceasedSettlor("utr", "internalId", amendedSettlor)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          "utr",
          "internalId",
          AmendDeceasedSettlorTransform(Json.toJson(amendedSettlor), originalDeceased)
        )
      }
    }
  }

