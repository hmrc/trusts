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

package uk.gov.hmrc.trusts.models.mapping.variations

import org.joda.time.DateTime
import org.scalatest.enablers.Definition
import play.api.libs.json._
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.models.variation.EstateVariation

class EstateVariationMappingSpec extends BaseSpec {

  implicit object JsonDefined extends Definition[JsLookupResult] {
    override def isDefined(thing: JsLookupResult): Boolean = thing.toOption.isDefined
  }

  "Variations" when {

    "reading json" must {

      "create a model" in {
        val payload = getJsonValueFromFile("valid-estate-variation-api.json")

        payload.validate[EstateVariation] match {
          case JsSuccess(model, _) =>

            model mustBe a[EstateVariation]

            model.details.administrationEndDate mustBe defined
            model.details.administrationEndDate.get mustBe DateTime.parse("2017-06-01")

          case JsError(errors) =>
            fail(errors.toString())
        }
      }
    }

    "writing json" must {

      "format json accepted by downstream" in {
        val payload = getJsonValueFromFile("valid-estate-variation-perrep-org-api.json").as[EstateVariation]

        val json = Json.toJson(payload)

        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0) mustBe defined
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepOrg" \ "orgName").as[String] mustNot be(empty)
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepOrg" \ "entityStart").as[String] mustEqual "2017-02-28"
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepInd") mustNot be(defined)
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 1 \ "estatePerRepInd") mustNot be(defined)
      }

      "format json accepted by downstream (two personal representatives)" in {
        val payload = getJsonValueFromFile("valid-estate-variation-add-perrep-ind-api.json").as[EstateVariation]

        val json = Json.toJson(payload)

        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0) mustBe defined
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepOrg" \ "orgName").as[String] mustBe "Trust Services LTD"
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepOrg" \ "entityEnd").as[String] mustEqual "2001-02-28"
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepInd") mustNot be(defined)
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 1) mustBe defined
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 1 \ "estatePerRepInd" \ "name" \ "firstName").as[String] mustBe "John"
      }

      "fully hydrate the variation models as expected" in {
        val payload = getJsonValueFromFile("valid-estate-variation-add-perrep-ind-api.json").as[EstateVariation]

        val jsonFromModels = Json.toJson(payload)

        val expectedJson: JsValue = getJsonValueFromFile("valid-estate-variation-add-perrep-ind-api.json")

        jsonFromModels mustBe expectedJson
      }

      "format Json when closing an estate" in {

        val payload = getJsonValueFromFile("valid-estate-variation-with-closing-date-api.json").as[EstateVariation]

        val json = Json.toJson(payload)

        (json \ "trustEndDate") mustBe defined
        (json \ "trustEndDate").as[String] mustEqual "2019-01-01"

      }

    }

  }

}
