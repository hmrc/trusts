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

import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.variation.UnidentifiedType
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.services.{AuditService, DesService, TransformationService}
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class RemoveBeneficiariesTransformSpec extends FreeSpec with MustMatchers with OptionValues with ScalaFutures with MockitoSugar {

  private def beneficiaryJson(value1 : String, endDate: Option[LocalDate] = None, withLineNo: Boolean = true) = {
    val a = Json.obj("field1" -> value1, "field2" -> "value20")

    val b = if (endDate.isDefined) {a.deepMerge(Json.obj("entityEnd" -> endDate.get))
    } else {a}

    if (withLineNo) {b.deepMerge(Json.obj("lineNo" -> 12))}
    else {b}
  }

  private def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json
      .put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove Beneficiary Transforms should round trip through JSON as part of Composed Transform" in {
    val OUT = ComposedDeltaTransform(Seq(
      RemoveBeneficiariesTransform(56, beneficiaryJson("Blah Blah Blah"), LocalDate.of(1563, 10, 23), "unidentified"),
      RemoveBeneficiariesTransform(12, beneficiaryJson("Foo"), LocalDate.of(2317, 12, 21),  "individualDetails")
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

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform(1, Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

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

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform(10, Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

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

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform(-1, Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

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
        RemoveBeneficiariesTransform(0, beneficiaryJson("One", None, false), LocalDate.of(2018, 4, 21), "charity")
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
      val desService = mock[DesService]
      val auditService = mock[AuditService]
      val transforms = Seq(RemoveBeneficiariesTransform( 1, beneficiaryJson("Two"), LocalDate.of(2018, 4, 21), "unidentified"))
      when(repo.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, desService, auditService)

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
      val desService = mock[DesService]
      val auditService = mock[AuditService]
      val transforms = Seq(
        AddUnidentifiedBeneficiaryTransform(UnidentifiedType(None, None, "Description", None, None, DateTime.parse("1967-12-30T12:00:00Z"), None)),
        RemoveBeneficiariesTransform(3, beneficiaryJson("Two", None, false), LocalDate.of(2018, 4, 21), "unidentified")
      )

      when(repo.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, desService, auditService)

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

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform(10, Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

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

      val OUT = ComposedDeltaTransform(Seq(RemoveBeneficiariesTransform(-1, Json.obj(), LocalDate.of(2018, 4, 21), "unidentified")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
  }
}
