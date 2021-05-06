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

package transformers.trustdetails

import play.api.libs.json._
import transformers.{DeltaTransform, JsonOperations}
import utils.Constants._

case class SetTrustDetailsTransform(value: JsValue,
                                    migratingFromNonTaxableToTaxable: Boolean) extends DeltaTransform with JsonOperations {

  val path: JsPath = TRUST \ DETAILS

  override def applyTransform(input: JsValue): JsResult[JsValue] = {

    /**
     * Prunes the optional trust details fields so that the new details take precedence (i.e. so a None overwrites a Some)
     * Also prunes residentialStatus to ensure we don't end up with ResidentialStatusType(Some, Some)
     */
    def pruneOptionalFields(existingTrustDetails: JsValue): JsValue = {
      val fields = if (migratingFromNonTaxableToTaxable) {
        Seq(LAW_COUNTRY, RESIDENTIAL_STATUS, UK_RELATION, DEED_OF_VARIATION, INTER_VIVOS, EFRBS_START_DATE)
      } else {
        Seq(UK_RELATION)
      }
      removeJsValueFields(existingTrustDetails, fields)
    }

    for {
      existingTrustDetails <- input.transform(path.json.pick)
      optionalFieldsRemoved = pruneOptionalFields(existingTrustDetails)
      mergedTrustDetails = merge(optionalFieldsRemoved, value)
      updatedInput <- pruneThenAddTo(input, path, mergedTrustDetails)
    } yield updatedInput
  }
}

object SetTrustDetailsTransform {

  val key = "SetTrustDetailsTransform"

  implicit val format: Format[SetTrustDetailsTransform] = Json.format[SetTrustDetailsTransform]
}
