/*
 * Copyright 2024 HM Revenue & Customs
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

package models.mapping.variations

import base.BaseSpec
import models.variation.TrustVariation
import org.scalatest.enablers.Definition
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsError, JsLookupResult, JsSuccess, Json}

class TrustVariationMappingSpec extends BaseSpec {

  implicit object JsonDefined extends Definition[JsLookupResult] {
    override def isDefined(thing: JsLookupResult): Boolean = thing.toOption.isDefined
  }

  "Variations" when {

    "reading json" must {

      "create a model" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api.json")

        payload.validate[TrustVariation] match {
          case JsSuccess(model, _) =>

            model mustBe a[TrustVariation]

            model.details.entities.leadTrustees mustNot be(empty)
            model.details.entities.leadTrustees.head.leadTrusteeOrg mustBe defined

          case JsError(errors) =>
            fail(errors.toString())
        }
      }
    }

    "writing json" must {

      "format json accepted by downstream" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api.json").as[TrustVariation]

        val json = Json.toJson(payload)

        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0) mustBe defined
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeOrg" \ "name")
          .as[String] mustNot be(empty)
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeOrg" \ "entityStart")
          .as[String] mustEqual "1998-02-12"
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeInd") mustNot be(defined)
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 1 \ "leadTrusteeInd") mustNot be(defined)
      }

      "format json accepted by downstream (two lead trustees)" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api-two-lead-trustees.json").as[TrustVariation]

        val json = Json.toJson(payload)

        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0) mustBe defined
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeOrg" \ "name")
          .as[String]                                                  mustBe "Trust Services LTD"
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeInd") mustNot be(defined)
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 1) mustBe defined
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 1 \ "leadTrusteeInd" \ "name" \ "firstName")
          .as[String]                                                  mustBe "John"

        (json \ "details" \ "trust" \ "assets" \ "monetary" \ 0 \ "assetMonetaryAmount").as[Int] mustBe 100000

      }

      "fully hydrate the variation models as expected" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api-two-lead-trustees.json").as[TrustVariation]

        val jsonFromModels = Json.toJson(payload)

        val expectedJson = getJsonValueFromFile("valid-trusts-variations-api-two-lead-trustees-expected.json")

        jsonFromModels mustBe expectedJson
      }

    }

  }

}
