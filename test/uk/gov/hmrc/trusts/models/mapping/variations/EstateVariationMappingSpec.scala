/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.enablers.Definition
import play.api.libs.json.{JsError, JsLookupResult, JsSuccess, Json}
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.models.variation.EstateVariation

class EstateVariationMappingSpec extends BaseSpec {

  implicit object JsonDefined extends Definition[JsLookupResult] {
    override def isDefined(thing: JsLookupResult): Boolean = thing.toOption.isDefined
  }

  "Variations" when {

    "reading json" must {

      "create a model" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api.json")

        payload.validate[EstateVariation] match {
          case JsSuccess(model, _) =>

            model mustBe a[EstateVariation]

            model.details.entities.personalRepresentative mustNot be(empty)
            model.details.entities.personalRepresentative.estatePerRepOrg mustBe defined

          case JsError(errors) =>
            fail(errors.toString())
        }
      }
    }

    "writing json" must {

      "format json accepted by downstream" in {
        val payload = getJsonValueFromFile("valid-estate-variation-api.json").as[EstateVariation]

        val json = Json.toJson(payload)

        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0) mustBe defined
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepOrg" \ "name").as[String] mustNot be(empty)
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepOrg" \ "entityStart").as[String] mustEqual "1998-02-12"
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 0 \ "estatePerRepInd") mustNot be(defined)
        (json \ "details" \ "estate" \ "entities" \ "personalRepresentative" \ 1 \ "estatePerRepInd") mustNot be(defined)
      }

      "format json accepted by downstream (two lead trustees)" in {
        val payload = getJsonValueFromFile("valid-estate-variation-api-two-personal-representatives.json").as[EstateVariation]

        val json = Json.toJson(payload)

        (json \ "details" \ "estate" \ "entities" \ "leadTrustees" \ 0) mustBe defined
        (json \ "details" \ "estate" \ "entities" \ "leadTrustees" \ 0 \ "estatePerRepOrg" \ "name").as[String] mustBe "Trust Services LTD"
        (json \ "details" \ "estate" \ "entities" \ "leadTrustees" \ 0 \ "estatePerRepInd") mustNot be(defined)
        (json \ "details" \ "estate" \ "entities" \ "leadTrustees" \ 1) mustBe defined
        (json \ "details" \ "estate" \ "entities" \ "leadTrustees" \ 1 \ "estatePerRepInd" \ "name" \ "firstName").as[String] mustBe "John"


      }

    }

  }

}
