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
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustTrusteeIndividualType

case class AddTrusteeIndTransform(trustee: DisplayTrustTrusteeIndividualType) extends DeltaTransform with AddTrusteeCommon {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    addTrustee(input, Json.toJson(trustee), TrusteeInd)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    addTrustee(input, Json.toJson(trustee), TrusteeInd)
  }
}

object AddTrusteeIndTransform {

  val key = "AddTrusteeIndTransform"

  implicit val format: Format[AddTrusteeIndTransform] = Json.format[AddTrusteeIndTransform]
}
