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

package transformers.settlors

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import repositories.TransformationRepository
import services.{AuditService, TransformationService, TrustsService}
import transformers.ComposedDeltaTransform
import uk.gov.hmrc.http.HeaderCarrier
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class RemoveSettlorTransformSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  private def settlorJson(value1 : String, endDate: Option[LocalDate] = None, withLineNo: Boolean = true) = {
    val a = Json.obj("field1" -> value1, "field2" -> "value20")

    val b = if (endDate.isDefined) {a.deepMerge(Json.obj("entityEnd" -> endDate.get))
    } else a

    if (withLineNo) {b.deepMerge(Json.obj("lineNo" -> 12))}
    else b
  }

  private def buildInputJson(settlorType: String, settlorData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "settlors" \ settlorType).json
      .put(JsArray(settlorData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove Settlor Transforms should round trip through JSON as part of Composed Transform" in {
    val OUT = ComposedDeltaTransform(Seq(
      RemoveSettlorTransform(Some(56), settlorJson("Blah Blah Blah"), LocalDate.of(1563, 10, 23), "settlor"),
      RemoveSettlorTransform(Some(12), settlorJson("Foo"), LocalDate.of(2317, 12, 21),  "settlorCompany")
    ))

    Json.toJson(OUT).validate[ComposedDeltaTransform] match {
      case JsSuccess(result, _) => result mustBe OUT
      case _ => fail("Transform failed")
    }
  }

  "the remove individual settlor normal transform must" - {
    "remove an individual settlor from the list that is returned to the frontend" in {
      val inputJson = buildInputJson("settlor", Seq(
        settlorJson("One"),
        settlorJson("Two"),
        settlorJson("Three")
      ))

      val expectedOutput = buildInputJson("settlor", Seq(
        settlorJson("One"),
        settlorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveSettlorTransform(Some(1), Json.obj(), LocalDate.of(2018, 4, 21), "settlor")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case JsError(errors) => fail(s"Transform failed: $errors")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("settlor", Seq(
        settlorJson("One"),
        settlorJson("Two"),
        settlorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveSettlorTransform(Some(10), Json.obj(), LocalDate.of(2018, 4, 21), "settlor")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("settlor", Seq(
        settlorJson("One"),
        settlorJson("Two"),
        settlorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveSettlorTransform(Some(-1), Json.obj(), LocalDate.of(2018, 4, 21), "settlor")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "remove the section if the last settlor in that section is removed" in {
      val inputJson = buildInputJson("settlor", Seq(
        settlorJson("One")
      ))

      val transforms = Seq(
        RemoveSettlorTransform(Some(0), settlorJson("One", None, withLineNo = false), LocalDate.of(2018, 4, 21), "settlor")
      )

      val OUT = ComposedDeltaTransform(transforms)

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value.transform(
          (JsPath()  \ "details" \ "trust" \ "entities" \ "settlors" \ "settlor"  ).json.pick).isError mustBe true
        case _ => fail("Transform failed")
      }
    }

  }

  "the remove business settlor declaration transform must" - {
    "set an end date on a business settlor from the list that is sent to ETMP" in {
      val inputJson = buildInputJson("settlorCompany", Seq(
        settlorJson("One"),
        settlorJson("Two"),
        settlorJson("Three")
      ))

      val expectedOutput = buildInputJson("settlorCompany", Seq(
        settlorJson("One"),
        settlorJson("Three"),
        settlorJson("Two", Some(LocalDate.of(2018, 4, 21)))

      ))

      val repo = mock[TransformationRepository]
      val trustsService = mock[TrustsService]
      val auditService = mock[AuditService]
      val transforms = Seq(RemoveSettlorTransform(Some(1), settlorJson("Two"), LocalDate.of(2018, 4, 21), "settlorCompany"))
      when(repo.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, trustsService, auditService)

      SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("settlorCompany", Seq(
        settlorJson("One"),
        settlorJson("Two"),
        settlorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveSettlorTransform(Some(10), Json.obj(), LocalDate.of(2018, 4, 21), "settlorCompany")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("settlorCompany", Seq(
        settlorJson("One"),
        settlorJson("Two"),
        settlorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveSettlorTransform(Some(-1), Json.obj(), LocalDate.of(2018, 4, 21), "settlorCompany")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
  }
}
