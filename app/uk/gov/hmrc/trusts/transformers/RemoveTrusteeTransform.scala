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

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustTrusteeType
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

case class RemoveTrusteeTransform(trustee: DisplayTrustTrusteeType, endDate: DateTime) extends DeltaTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val trusteePath = (__ \ 'details \ 'trust \ 'entities \ 'trustees).json

    input.transform(trusteePath.pick) match {
      case JsSuccess(json, _) =>

        val trustees = json.as[List[DisplayTrustTrusteeType]]
        val filtered = trustees.filterNot(_ == trustee)
        val trusteesRemovedAsJson = Json.toJson(filtered)

        input.transform(
          trusteePath.prune andThen
            JsPath.json.update {
              trusteePath.put(Json.toJson(trusteesRemovedAsJson))
            }
        )

      case JsError(errors) =>
        throw JsResultException(errors)
    }
  }

}

object RemoveTrusteeTransform {

  val key = "RemoveTrusteeTransform"

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  implicit val format: Format[RemoveTrusteeTransform] = Json.format[RemoveTrusteeTransform]
}
