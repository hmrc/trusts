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
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.{NameType, variation}
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SettlorTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {

   private implicit val pc: PatienceConfig =
     PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private implicit val hc : HeaderCarrier = HeaderCarrier()

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

    "must add a new amend individual beneficiary transform" in {
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

      when(transformationService.getTransformedData(any(), any())(any()))
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
}