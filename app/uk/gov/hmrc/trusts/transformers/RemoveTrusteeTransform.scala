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
import play.api.libs.json.Reads.of
import play.api.libs.json._
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

case class RemoveTrusteeTransform(endDate: DateTime, index: Int, originalTrusteeJson: JsValue) extends DeltaTransform {
  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  private val trusteePath = (__ \ 'details \ 'trust \ 'entities \ 'trustees).json

  override def applyTransform(input: JsValue): JsResult[JsValue] = {

    input.transform(trusteePath.pick) match {
      case JsSuccess(json, _) =>

        val array = json.as[JsArray]

        val filtered = array.value.take(index) ++ array.value.drop(index + 1)

        input.transform(
          trusteePath.prune andThen
            JsPath.json.update {
              trusteePath.put(Json.toJson(filtered))
            }
        )

      case e: JsError => e
    }
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    originalTrusteeJson.transform(addEntityEnd(originalTrusteeJson, endDate)) match {
      case JsSuccess(endedTrusteeJson, _) =>
        val trustees: Reads[JsObject] =
          trusteePath.update( of[JsArray]
            .map {
              trustees => trustees :+ endedTrusteeJson
            }
          )
        input.transform(trustees)

      case e: JsError => e
    }
  }

  private def addEntityEnd(originalJson: JsValue, endDate: DateTime) = {
    val entityEndPath =
      if (originalJson.transform((__ \ 'trusteeInd).json.pick).isSuccess)
        (__ \ 'trusteeInd \ 'entityEnd).json
      else
        (__ \ 'trusteeOrg \ 'entityEnd).json

    (__).json.update(entityEndPath.put(Json.toJson(endDate)))
  }
}

object RemoveTrusteeTransform {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  implicit val format: Format[RemoveTrusteeTransform] = Json.format[RemoveTrusteeTransform]
}
