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

case class SetTrustDetailTransform(value: JsValue, key: String) extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ 'details \ 'trust \ 'details \ key

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    pruneThenAddTo(input, path, value)
  }

  override val isTaxableMigrationTransform: Boolean = {

    val taxableMigrationDetailKeys: Seq[String] = Seq(
      LAW_COUNTRY,
      ADMINISTRATION_COUNTRY,
      TYPE_OF_TRUST,
      DEED_OF_VARIATION,
      INTER_VIVOS,
      EFRBS_START_DATE,
      RESIDENTIAL_STATUS
    )

    taxableMigrationDetailKeys.contains(key)
  }

}

object SetTrustDetailTransform {

  val key = "SetTrustDetailTransform"

  implicit val format: Format[SetTrustDetailTransform] = Json.format[SetTrustDetailTransform]

}
