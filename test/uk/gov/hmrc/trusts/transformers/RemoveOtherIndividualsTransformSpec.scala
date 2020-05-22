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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.services.{AuditService, DesService, TransformationService}
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class RemoveOtherIndividualsTransformSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  private def otherIndividualJson(endDate: Option[LocalDate] = None, withLineNo: Boolean = true) = {
    val a = Json.obj("field1" -> "value20")

    val b = if (endDate.isDefined) {a.deepMerge(Json.obj("entityEnd" -> endDate.get))
    } else a

    if (withLineNo) {b.deepMerge(Json.obj("lineNo" -> 12))}
    else b
  }

  private def buildInputJson(otherIndividualData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "naturalPerson").json
      .put(JsArray(otherIndividualData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove OtherIndividual Transforms should round trip through JSON as part of Composed Transform" in {
    val OUT = ComposedDeltaTransform(Seq(
      RemoveOtherIndividualsTransform(56, otherIndividualJson(), LocalDate.of(1563, 10, 23)),
      RemoveOtherIndividualsTransform(12, otherIndividualJson(), LocalDate.of(2317, 12, 21))
    ))

    Json.toJson(OUT).validate[ComposedDeltaTransform] match {
      case JsSuccess(result, _) => result mustBe OUT
      case _ => fail("Transform failed")
    }
  }

  "the remove otherIndividual normal transform must" - {

    "remove an otherIndividual from the list that is returned to the frontend" in {

      val inputJson = buildInputJson(Seq(
        otherIndividualJson(),
        otherIndividualJson(),
        otherIndividualJson()
      ))

      val expectedOutput = buildInputJson(Seq(
        otherIndividualJson(),
        otherIndividualJson()
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveOtherIndividualsTransform(1, Json.obj(), LocalDate.of(2018, 4, 21))))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe expectedOutput
        case JsError(errors) => fail(s"Transform failed: $errors")
      }
    }

    "not affect the document if the index is too high" in {
      val inputJson = buildInputJson(Seq(
        otherIndividualJson(),
        otherIndividualJson(),
        otherIndividualJson()
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveOtherIndividualsTransform(10, Json.obj(), LocalDate.of(2018, 4, 21))))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "not affect the document if the index is too low" in {
      val inputJson = buildInputJson(Seq(
        otherIndividualJson(),
        otherIndividualJson(),
        otherIndividualJson()
      ))

      val OUT = ComposedDeltaTransform(Seq(RemoveOtherIndividualsTransform(-1, Json.obj(), LocalDate.of(2018, 4, 21))))

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value mustBe inputJson
        case _ => fail("Transform failed")
      }
    }

    "remove the section if the last otherIndividual in that section is removed" in {
      val inputJson = buildInputJson(Seq(
        otherIndividualJson()
      ))

      val transforms = Seq(
        RemoveOtherIndividualsTransform(0, otherIndividualJson(None, withLineNo = false), LocalDate.of(2018, 4, 21))
      )

      val OUT = ComposedDeltaTransform(transforms)

      OUT.applyTransform(inputJson) match {
        case JsSuccess(value, _) => value.transform(
          (JsPath()  \ "details" \ "trust" \ "entities" \ "otherIndividuals" \ "naturalPerson"  ).json.pick).isError mustBe true
        case _ => fail("Transform failed")
      }
    }
  }
}
