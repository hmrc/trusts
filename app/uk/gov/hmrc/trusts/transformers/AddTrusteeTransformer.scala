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
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.variation.TrusteeType

class AddTrusteeTransformer(newTrustee: TrusteeType) extends DeltaTransform {

  override def applyTransform(input: JsValue): JsValue = {

    val trustees: Reads[JsObject] =
      (__ \ 'details \ 'trust \ 'entities \ 'trustees).json.update( of[JsArray]
        .map {
          trustees => (newTrustee.trusteeInd, newTrustee.trusteeOrg) match {
            case (Some(individual), None) => trustees :+ Json.obj("trusteeInd" -> Json.toJson(individual))
            case (None, Some(organisation)) => trustees :+ Json.obj("trusteeOrg" -> Json.toJson(organisation))
          }
        }
      )

    input.transform(trustees).fold(
      errors  => throw new Exception(s"Failed to transform Json with the following errors: $errors"),
      valid   => valid
    )
  }
}
