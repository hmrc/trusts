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

package services

import models.AddressType
import models.variation._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import transformers._
import transformers.remove.RemoveAsset
import uk.gov.hmrc.http.HeaderCarrier
import utils.{JsonFixtures, JsonUtils}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssetsTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def nonEeaBusinessAssetJson(value1: String, endDate: Option[LocalDate] = None) = {
    if (endDate.isDefined) {
      Json.obj("field1" -> value1, "field2" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> value1, "field2" -> "value20", "lineNo" -> 65)
    }
  }

  object LocalDateMock extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  def buildInputJson(nonEeaBusinessAssetType: String, nonEeaBusinessAssetData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "assets" \ nonEeaBusinessAssetType).json
      .put(JsArray(nonEeaBusinessAssetData))

    baseJson.as[JsObject](__.json.update(adder))
  }


  "The NonEeaBusinessAsset transformation service" - {

    "must add a new remove NonEeaBusinessAsset transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new AssetsTransformationService(transformationService, LocalDateMock)
      val nonEeaBusinessAsset = nonEeaBusinessAssetJson("Blah Blah Blah")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))

      when(transformationService.getTransformedTrustJson(any(), any())(any()))
        .thenReturn(Future.successful(buildInputJson("nonEEABusiness", Seq(nonEeaBusinessAsset))))

      val result = service.removeAsset("utr", "internalId", RemoveAsset(LocalDate.of(2013, 2, 20), 0, "nonEEABusiness"))
      whenReady(result) { _ =>
        verify(transformationService).addNewTransform("utr",
          "internalId", RemoveAssetTransform(0, nonEeaBusinessAsset, LocalDate.of(2013, 2, 20), "nonEEABusiness"))
      }
    }

    "must add a new amend NonEeaBusinessAsset transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new AssetsTransformationService(transformationService, LocalDateMock)
      val address = AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK")
      val originalNonEeaBusinessAssetJson = Json.toJson(NonEEABusinessType("1", "TestOrg", address, "UK", LocalDate.parse("2000-01-01"), None))
      val amendedNonEeaBusinessAssetJson = NonEEABusinessType("1", "TestOrg2", address, "UK", LocalDate.parse("2000-01-01"), None)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      when(transformationService.getTransformedTrustJson(any(), any())(any()))
        .thenReturn(Future.successful(buildInputJson("nonEEABusiness", Seq(originalNonEeaBusinessAssetJson))))

      val result = service.amendAsset("utr", index, "internalId", amendedNonEeaBusinessAssetJson)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",
          AmendAssetTransform(index, Json.toJson(amendedNonEeaBusinessAssetJson), originalNonEeaBusinessAssetJson, LocalDateMock.now, amendedNonEeaBusinessAssetJson.toString))
      }
    }

    "must add a new add NonEeaBusinessAsset transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new AssetsTransformationService(transformationService, LocalDateMock)
      val newNonEeaBusinessAsset = NonEEABusinessType(
        "1",
        "TestOrg",
        AddressType(
          "Line 1",
          "Line 2",
          None,
          None,
          Some("NE11NE"), "UK"),
        "UK",
        LocalDate.parse("2000-01-01"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAsset("utr", "internalId", newNonEeaBusinessAsset)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddAssetTransform(Json.toJson(newNonEeaBusinessAsset), newNonEeaBusinessAsset.toString))
      }
    }
  }
}
