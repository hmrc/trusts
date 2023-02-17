/*
 * Copyright 2023 HM Revenue & Customs
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

package transformers.assets

import models.AddressType
import models.variation.NonEEABusinessType
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import repositories.TransformationRepository
import services.auditing.AuditService
import services.{TransformationService, TrustsService}
import transformers.ComposedDeltaTransform
import uk.gov.hmrc.http.HeaderCarrier
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class RemoveAssetTransformSpec extends AnyFreeSpec with ScalaFutures with MockitoSugar {

  private def assetJson(value1: String, endDate: Option[LocalDate] = None, withLineNo: Boolean = true) = {
    val a = Json.obj("field1" -> value1, "field2" -> "value20")

    val b = if (endDate.isDefined) {
      a.deepMerge(Json.obj("endDate" -> endDate.get))
    } else a

    if (withLineNo) {
      b.deepMerge(Json.obj("lineNo" -> 12))
    } else b
  }

  private def buildInputJson(nonEeaBusinessAssetData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "assets" \ "nonEEABusiness").json
      .put(JsArray(nonEeaBusinessAssetData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  private val inputJson = buildInputJson(Seq(
    assetJson("One"),
    assetJson("Two"),
    assetJson("Three")
  ))

  "Remove asset transforms should round trip through JSON as part of Composed Transform" in {
    val OUT = ComposedDeltaTransform(Seq(
      RemoveAssetTransform(Some(56), assetJson("Blah Blah Blah"), LocalDate.of(1563, 10, 23), "nonEEABusiness"),
      RemoveAssetTransform(Some(12), assetJson("Foo"), LocalDate.of(2317, 12, 21), "nonEEABusiness")
    ))

    Json.toJson(OUT).validate[ComposedDeltaTransform] match {
      case JsSuccess(result, _) => result mustBe OUT
      case _ => fail("Transform failed")
    }
  }

  "the remove asset normal transform must" - {

    "remove an asset from the list that is returned to the frontend" in {

      val expectedOutput = buildInputJson(Seq(
        assetJson("One"),
        assetJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveAssetTransform(Some(1), Json.obj(), LocalDate.of(2018, 4, 21), "nonEEABusiness")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case JsError(errors) => fail(s"Transform failed: $errors")
      }
    }

    "not affect the document if the index is too high" in {

      val OUT = ComposedDeltaTransform(Seq(RemoveAssetTransform(Some(10), Json.obj(), LocalDate.of(2018, 4, 21), "nonEEABusiness")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {

      val OUT = ComposedDeltaTransform(Seq(RemoveAssetTransform(Some(-1), Json.obj(), LocalDate.of(2018, 4, 21), "nonEEABusiness")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "remove the section if the last asset in that section is removed" in {

      val inputJson = buildInputJson(Seq(assetJson("One")))

      val transforms = Seq(
        RemoveAssetTransform(Some(0), assetJson("One", None, withLineNo = false), LocalDate.of(2018, 4, 21), "nonEEABusiness")
      )

      val OUT = ComposedDeltaTransform(transforms)

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value.transform(
          (JsPath() \ "details" \ "trust" \ "assets" \ "nonEEABusiness").json.pick).isError mustBe true
        case _ => fail("Transform failed")
      }
    }
  }

  "the remove asset declaration transform must" - {

    "when non-EEA business asset" - {
      "must set an end date" in {

        val expectedOutput = buildInputJson(Seq(
          assetJson("One"),
          assetJson("Three"),
          assetJson("Two", Some(LocalDate.of(2018, 4, 21)))
        ))

        val repo = mock[TransformationRepository]
        val trustsService = mock[TrustsService]
        val auditService = mock[AuditService]
        val transforms = Seq(RemoveAssetTransform(Some(1), assetJson("Two"), LocalDate.of(2018, 4, 21), "nonEEABusiness"))
        when(repo.get(any(), any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

        val SUT = new TransformationService(repo, trustsService, auditService)

        SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
          case JsSuccess(value, _) => value mustBe expectedOutput
          case _ => fail("Transform failed")
        }
      }
    }

    "when not non-EEA business asset" - {
      "must not set an end date" in {

        val expectedOutput = buildInputJson(Seq(
          assetJson("One"),
          assetJson("Two"),
          assetJson("Three")
        ))

        val repo = mock[TransformationRepository]
        val trustsService = mock[TrustsService]
        val auditService = mock[AuditService]
        val transforms = Seq(RemoveAssetTransform(Some(1), assetJson("Two"), LocalDate.of(2018, 4, 21), "other"))
        when(repo.get(any(), any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

        val SUT = new TransformationService(repo, trustsService, auditService)

        SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
          case JsSuccess(value, _) => value mustBe expectedOutput
          case _ => fail("Transform failed")
        }
      }
    }

    "ignore an asset that was added then removed" in {

      val repo = mock[TransformationRepository]
      val trustsService = mock[TrustsService]
      val auditService = mock[AuditService]
      val transforms = Seq(
        AddAssetTransform(Json.toJson(NonEEABusinessType(None, "TestOrg", AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"), "UK", LocalDate.parse("1967-12-30"), None)), "nonEEABusiness"),
        RemoveAssetTransform(Some(3), assetJson("Two", None, withLineNo = false), LocalDate.of(2018, 4, 21), "nonEEABusiness")
      )

      when(repo.get(any(), any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, trustsService, auditService)

      SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too high" in {

      val OUT = ComposedDeltaTransform(Seq(RemoveAssetTransform(Some(10), Json.obj(), LocalDate.of(2018, 4, 21), "nonEEABusiness")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {

      val OUT = ComposedDeltaTransform(Seq(RemoveAssetTransform(Some(-1), Json.obj(), LocalDate.of(2018, 4, 21), "nonEEABusiness")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
  }
}
