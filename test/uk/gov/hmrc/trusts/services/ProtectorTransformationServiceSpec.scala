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
import uk.gov.hmrc.trusts.models.{NameType, RemoveProtector}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.ProtectorCompany
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProtectorTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {

  private implicit val pc: PatienceConfig =
    PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def protectorJson(value1: String, endDate: Option[LocalDate] = None) = {
    if (endDate.isDefined) {
      Json.obj("field1" -> value1, "field2" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> value1, "field2" -> "value20", "lineNo" -> 65)
    }
  }

  object LocalDateMock extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  def buildInputJson(protectorType: String, data: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "protectors" \ protectorType).json
      .put(JsArray(data))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "The protector transformation service" - {

    "must add a new remove protector transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new ProtectorTransformationService(transformationService, LocalDateMock)
      val protector = protectorJson("Blah Blah Blah")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))
      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("protector", Seq(protector)),
          ResponseHeader("status", "formBundlNo")
        )))

      val result = service.removeProtector("utr", "internalId", RemoveProtector(LocalDate.of(2013, 2, 20), 0, "protector"))
      whenReady(result) { _ =>
        verify(transformationService).addNewTransform("utr",
          "internalId", RemoveProtectorsTransform(0, protector, LocalDate.of(2013, 2, 20), "protector"))
      }
    }

    "must add a new add individual protector transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new ProtectorTransformationService(transformationService, LocalDateMock)
      val newProtector = DisplayTrustProtector(
        None,
        None,
        NameType("First", None, "Last"),
        None,
        None,
        LocalDate.parse("1990-10-10")
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addIndividualProtectorTransformer("utr", "internalId", newProtector)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddIndividualProtectorTransform(newProtector))
      }
    }

    "must add a new add company protector transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new ProtectorTransformationService(transformationService, LocalDateMock)
      val newCompanyProtector = DisplayTrustProtectorCompany(
        name = "TestCompany",
        identification = None,
        lineNo = None,
        bpMatchStatus = None,
        entityStart = LocalDate.parse("2010-05-03")
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addCompanyProtectorTransformer("utr", "internalId", newCompanyProtector)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddCompanyProtectorTransform(newCompanyProtector))
      }
    }

    "must add a new amend business protector transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new ProtectorTransformationService(transformationService, LocalDateMock)
      val newCompany = ProtectorCompany(
        None,
        None,
        "Company Name",
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
          buildInputJson("protectorCompany", Seq(original)),
          ResponseHeader("status", "formBundleNo")
        )))

      val result = service.amendBusinessProtectorTransformer("utr", index, "internalId", newCompany)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          Matchers.eq("utr"),
          Matchers.eq("internalId"),
          Matchers.eq(AmendBusinessProtectorTransform(index, Json.toJson(newCompany), original, LocalDateMock.now))
        )
      }
    }
  }
}
