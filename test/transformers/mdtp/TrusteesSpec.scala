/*
 * Copyright 2026 HM Revenue & Customs
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

package transformers.mdtp

import models.variation.TrusteeType
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._
import utils.JsonOps.prunePathAndPutNewValue
import utils.JsonUtils

class TrusteesSpec extends AnyFreeSpec {

  private val inOneSlot  = JsonUtils.getJsonValueFromFile("trustees-in-one-slot/etmp-trust.json")
  private val onePerSlot = JsonUtils.getJsonValueFromFile("trustees-in-one-slot/etmp-trust-one-per-slot.json")

  private val trusteesPath = __ \ "details" \ "trust" \ "entities" \ "trustees"

  private def withTrustees(trustees: JsValue*): JsValue =
    inOneSlot.transform(prunePathAndPutNewValue(trusteesPath, JsArray(trustees))).get

  private def trustees(json: JsValue): JsArray = json.transform(trusteesPath.json.pick[JsArray]).get

  private val individual = trustees(onePerSlot).value(0)
  private val business   = trustees(onePerSlot).value(1)
  private val bothInOne  = trustees(inOneSlot).value(0)

  "Trustees" - {

    "when .splitTrusteesInOneSlot" - {

      "must split a slot with a trusteeInd and a trusteeOrg into two slots, the individual first" in {
        Trustees.splitTrusteesInOneSlot(inOneSlot).get mustBe onePerSlot
      }

      "must not change trustees that already have one trustee per slot" in {
        Trustees.splitTrusteesInOneSlot(onePerSlot).get mustBe onePerSlot
      }

      "must keep the order of the other trustees when several slots hold two trustees" in {
        val json = withTrustees(business, bothInOne, individual, bothInOne)

        trustees(Trustees.splitTrusteesInOneSlot(json).get) mustBe
          JsArray(Seq(business, individual, business, individual, individual, business))
      }

      "must not change a trust with an empty trustees array" in {
        val json = withTrustees()

        Trustees.splitTrusteesInOneSlot(json).get mustBe json
      }

      "must not change a trust without trustees" in {
        val json = inOneSlot.transform(trusteesPath.json.prune).get

        Trustees.splitTrusteesInOneSlot(json).get mustBe json
      }
    }

    "when .transform" - {

      "must give the same trustees for the raw json and the split json, so the indexes match" in {
        def transformed(json: JsValue): JsResult[JsObject] =
          json.transform(Trustees.transform(json)(TrusteeType.trusteeTypeFormat))

        transformed(inOneSlot) mustBe transformed(Trustees.splitTrusteesInOneSlot(inOneSlot).get)
      }
    }
  }

}
