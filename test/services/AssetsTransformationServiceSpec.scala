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
import transformers.assets.{AddAssetTransform, AmendAssetTransform, RemoveAssetTransform}
import transformers.remove.RemoveAsset
import uk.gov.hmrc.http.HeaderCarrier
import utils.{JsonFixtures, JsonUtils}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssetsTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {
  
  private implicit val pc: PatienceConfig = PatienceConfig(
    timeout = Span(1000, Millis),
    interval = Span(15, Millis)
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val index: Int = 0
  private val utr: String = "utr"
  private val internalId: String = "internalId"
  private val arbitraryAssetType: String = "nonEEABusiness"
  private val date: LocalDate = LocalDate.parse("2000-01-01")

  private def assetJson(value1: String, endDate: Option[LocalDate] = None): JsObject = {
    if (endDate.isDefined) {
      Json.obj("field1" -> value1, "field2" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> value1, "field2" -> "value20", "lineNo" -> 65)
    }
  }

  object LocalDateMock extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  def buildInputJson(assetType: String, assetData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "assets" \ assetType).json.put(JsArray(assetData))

    baseJson.as[JsObject](__.json.update(adder))
  }
  
  private def buildAsset(orgName: String): NonEEABusinessType = NonEEABusinessType(
    lineNo = "1",
    orgName = orgName,
    address = AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
    govLawCountry = "UK",
    startDate = date,
    endDate = None
  )

  "Assets transformation service" - {

    "must add a new add asset transform using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new AssetsTransformationService(transformationService, LocalDateMock)

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))
      
      val asset = buildAsset("TestOrg")

      val result = service.addAsset(utr, internalId, asset)

      whenReady(result) { _ => verify(transformationService).addNewTransform(
        utr,
        internalId,
        AddAssetTransform(
          Json.toJson(asset),
          asset.toString
        )
      )}
    }

    "must add a new amend asset transform using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new AssetsTransformationService(transformationService, LocalDateMock)

      val originalAssetJson = Json.toJson(buildAsset("TestOrg"))
      val amendedAsset = buildAsset("TestOrg2")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))

      when(transformationService.getTransformedTrustJson(any(), any())(any()))
        .thenReturn(Future.successful(buildInputJson(arbitraryAssetType, Seq(originalAssetJson))))

      val result = service.amendAsset(utr, index, internalId, amendedAsset)
      
      whenReady(result) { _ => verify(transformationService).addNewTransform(
        utr,
        internalId,
        AmendAssetTransform(
          index,
          Json.toJson(amendedAsset),
          originalAssetJson,
          LocalDateMock.now,
          amendedAsset.toString
        )
      )}
    }

    "must add a new remove asset transform using the transformation service" in {
      
      val transformationService = mock[TransformationService]
      val service = new AssetsTransformationService(transformationService, LocalDateMock)

      val asset = assetJson("TestOrg")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(true))

      when(transformationService.getTransformedTrustJson(any(), any())(any()))
        .thenReturn(Future.successful(buildInputJson(arbitraryAssetType, Seq(asset))))

      val result = service.removeAsset(
        utr,
        internalId,
        RemoveAsset(
          date,
          index,
          arbitraryAssetType
        )
      )

      whenReady(result) { _ =>
        verify(transformationService).addNewTransform(
          utr,
          internalId,
          RemoveAssetTransform(
            index,
            asset,
            date,
            arbitraryAssetType
          )
        )
      }
    }
  }
}
