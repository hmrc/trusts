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

package transformers

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import repositories.TransformationRepository
import services.{AuditService, TrustsService, TransformationService}
import utils.JsonUtils

import scala.concurrent.Future

class RemoveProtectorsTransformSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  private def protectorJson(value1 : String, endDate: Option[LocalDate] = None, withLineNo: Boolean = true) = {
    val a = Json.obj("field1" -> value1, "field2" -> "value20")

    val b = if (endDate.isDefined) {a.deepMerge(Json.obj("entityEnd" -> endDate.get))
    } else a

    if (withLineNo) {b.deepMerge(Json.obj("lineNo" -> 12))}
    else b
  }

  private def buildInputJson(protectorType: String, protectorData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "protectors" \ protectorType).json
      .put(JsArray(protectorData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove Protector Transforms should round trip through JSON as part of Composed Transform" in {
    val OUT = ComposedDeltaTransform(Seq(
      RemoveProtectorsTransform(56, protectorJson("Blah Blah Blah"), LocalDate.of(1563, 10, 23), "protector"),
      RemoveProtectorsTransform(12, protectorJson("Foo"), LocalDate.of(2317, 12, 21),  "protectorCompany")
    ))

    Json.toJson(OUT).validate[ComposedDeltaTransform] match {
      case JsSuccess(result, _) => result mustBe OUT
      case _ => fail("Transform failed")
    }
  }

  "the remove individual protector normal transform must" - {

    "remove an individual protector from the list that is returned to the frontend" in {

      val inputJson = buildInputJson("protector", Seq(
        protectorJson("One"),
        protectorJson("Two"),
        protectorJson("Three")
      ))

      val expectedOutput = buildInputJson("protector", Seq(
        protectorJson("One"),
        protectorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveProtectorsTransform(1, Json.obj(), LocalDate.of(2018, 4, 21), "protector")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case JsError(errors) => fail(s"Transform failed: $errors")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("protector", Seq(
        protectorJson("One"),
        protectorJson("Two"),
        protectorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveProtectorsTransform(10, Json.obj(), LocalDate.of(2018, 4, 21), "protector")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("protector", Seq(
        protectorJson("One"),
        protectorJson("Two"),
        protectorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveProtectorsTransform(-1, Json.obj(), LocalDate.of(2018, 4, 21), "protector")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "remove the section if the last protector in that section is removed" in {
      val inputJson = buildInputJson("protector", Seq(
        protectorJson("One")
      ))

      val transforms = Seq(
        RemoveProtectorsTransform(0, protectorJson("One", None, withLineNo = false), LocalDate.of(2018, 4, 21), "protector")
      )

      val OUT = ComposedDeltaTransform(transforms)

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value.transform(
          (JsPath()  \ "details" \ "trust" \ "entities" \ "protectors" \ "protector"  ).json.pick).isError mustBe true
        case _ => fail("Transform failed")
      }
    }

  }

  "the remove business protector declaration transform must" - {

    "set an end date on a business protector from the list that is sent to ETMP" in {

      val inputJson = buildInputJson("protectorCompany", Seq(
        protectorJson("One"),
        protectorJson("Two"),
        protectorJson("Three")
      ))

      val expectedOutput = buildInputJson("protectorCompany", Seq(
        protectorJson("One"),
        protectorJson("Three"),
        protectorJson("Two", Some(LocalDate.of(2018, 4, 21)))

      ))

      val repo = mock[TransformationRepository]
      val trustsService = mock[TrustsService]
      val auditService = mock[AuditService]
      val transforms = Seq(RemoveProtectorsTransform(1, protectorJson("Two"), LocalDate.of(2018, 4, 21), "protectorCompany"))
      when(repo.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(transforms))))

      val SUT = new TransformationService(repo, trustsService, auditService)

      SUT.applyDeclarationTransformations("UTRUTRUTR", "InternalId", inputJson)(HeaderCarrier()).futureValue match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson("protectorCompany", Seq(
        protectorJson("One"),
        protectorJson("Two"),
        protectorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveProtectorsTransform(10, Json.obj(), LocalDate.of(2018, 4, 21), "protectorCompany")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson("protectorCompany", Seq(
        protectorJson("One"),
        protectorJson("Two"),
        protectorJson("Three")
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveProtectorsTransform(-1, Json.obj(), LocalDate.of(2018, 4, 21), "protectorCompany")))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }
  }
}
