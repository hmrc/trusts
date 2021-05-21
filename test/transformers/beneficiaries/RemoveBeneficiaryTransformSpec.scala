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

package transformers.beneficiaries

import models.variation.UnidentifiedType
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import repositories.TransformationRepository
import services.{AuditService, TransformationService, TrustsService}
import transformers.ComposedDeltaTransform
import uk.gov.hmrc.http.HeaderCarrier
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class RemoveBeneficiaryTransformSpec extends AnyFreeSpec with ScalaFutures with MockitoSugar {

  private def beneficiaryJson(value1 : String, endDate: Option[LocalDate] = None, withLineNo: Boolean = true) = {
    val a = Json.obj("field1" -> value1, "field2" -> "value20")

    val b = if (endDate.isDefined) {a.deepMerge(Json.obj("entityEnd" -> endDate.get))
    } else a

    if (withLineNo) {b.deepMerge(Json.obj("lineNo" -> 12))}
    else b
  }

  private def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json
      .put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove Beneficiary Transforms should round trip through JSON as part of Composed Transform" in {
    val OUT = ComposedDeltaTransform(Seq(
      RemoveBeneficiaryTransform(Some(56), beneficiaryJson("Blah Blah Blah"), LocalDate.of(1563, 10, 23), "unidentified"),
      RemoveBeneficiaryTransform(Some(12), beneficiaryJson("Foo"), LocalDate.of(2317, 12, 21),  "individualDetails")
    ))

    Json.toJson(OUT).validate[ComposedDeltaTransform] match {
      case JsSuccess(result, _) => result mustBe OUT
      case _ => fail("Transform failed")
    }
  }

  "the remove unidentified beneficiary normal transform must" - {
    "remove an unidentified beneficiary from the list that is returned to the frontend" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val expectedOutput = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiaryTransform(Some(1), Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case JsError(errors) => fail(s"Transform failed: $errors")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiaryTransform(Some(10), Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiaryTransform(Some(-1), Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
    "remove the section if the last beneficiary in that section is removed" in {
      val inputJson = buildInputJson("charity", Seq(
        beneficiaryJson("One")
      ))

      val transforms = Seq(
        RemoveBeneficiaryTransform(Some(0), beneficiaryJson("One", None, withLineNo = false), LocalDate.of(2018, 4, 21), "charity")
      )

      val OUT = ComposedDeltaTransform(transforms)

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value.transform(
          (JsPath()  \ "details" \ "trust" \ "entities" \ "beneficiary" \ "charity"  ).json.pick).isError mustBe true
        case _ => fail("Transform failed")
      }
    }

  }

  "the remove unidentified beneficiary declaration transform must" - {
    "set an end date on  an unidentified beneficiary from the list that is sent to ETMP" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val expectedOutput = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Three"),
        beneficiaryJson("Two", Some(LocalDate.of(2018, 4, 21)))

      ))

      val repo = mock[TransformationRepository]
      val trustsService = mock[TrustsService]
      val auditService = mock[AuditService]
      val transforms = Seq(RemoveBeneficiaryTransform(Some(1), beneficiaryJson("Two"), LocalDate.of(2018, 4, 21), "unidentified"))
      when(repo.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, trustsService, auditService)

      SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case _ => fail("Transform failed")
      }
    }

    "ignore a beneficiary that was added then removed" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val repo = mock[TransformationRepository]
      val trustsService = mock[TrustsService]
      val auditService = mock[AuditService]
      val transforms = Seq(
        AddBeneficiaryTransform(Json.toJson(UnidentifiedType(None, None, "Description", None, None, LocalDate.parse("1967-12-30"), None)), "unidentified"),
        RemoveBeneficiaryTransform(Some(3), beneficiaryJson("Two", None, withLineNo = false), LocalDate.of(2018, 4, 21), "unidentified")
      )

      when(repo.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, trustsService, auditService)

      SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiaryTransform(Some(10), Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("unidentified", Seq(
        beneficiaryJson("One"),
        beneficiaryJson("Two"),
        beneficiaryJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiaryTransform(Some(-1), Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
  }
}
