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
import uk.gov.hmrc.trusts.models.variation.Variation

class VariationMappingSpec extends BaseSpec {

  implicit object JsonDefined extends Definition[JsLookupResult] {
    override def isDefined(thing: JsLookupResult): Boolean = thing.toOption.isDefined
  }

  "Variations" when {

    "reading json" must {

      "create a model" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api.json")

        payload.validate[Variation] match {
          case JsSuccess(model, _) =>

            model mustBe a[Variation]

            model.details.entities.leadTrustees mustNot be(empty)
            model.details.entities.leadTrustees.head.leadTrusteeOrg mustBe defined

          case JsError(errors) =>
            fail(errors.toString())
        }
      }
    }

    "writing json" must {

      "format json accepted by downstream" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api.json").as[Variation]

        val json = Json.toJson(payload)

        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0) mustBe defined
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeOrg" \ "name").as[String] mustNot be(empty)
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeInd") mustNot be(defined)
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 1 \ "leadTrusteeInd") mustNot be(defined)
      }

      "format json accepted by downstream (two lead trustees)" in {
        val payload = getJsonValueFromFile("valid-trusts-variations-api-two-lead-trustees.json").as[Variation]

        val json = Json.toJson(payload)

        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0) mustBe defined
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeOrg" \ "name").as[String] mustBe "Trust Services LTD"
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 0 \ "leadTrusteeInd") mustNot be(defined)
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 1) mustBe defined
        (json \ "details" \ "trust" \ "entities" \ "leadTrustees" \ 1 \ "leadTrusteeInd" \ "name" \ "firstName").as[String] mustBe "John"
      }

    }

  }

}
