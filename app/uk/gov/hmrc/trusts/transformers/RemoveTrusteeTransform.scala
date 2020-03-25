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

import java.time.LocalDate

import org.joda.time.DateTime
import play.api.libs.json.Reads.of
import play.api.libs.json._
import uk.gov.hmrc.trusts.utils.Constants.dateTimePattern

case class RemoveTrusteeTransform(endDate: LocalDate, index: Int, trusteeToRemove: JsValue)
  extends DeltaTransform
  with JsonOperations {

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  private val trusteePath = (__ \ 'details \ 'trust \ 'entities \ 'trustees)

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    removeAtPosition(input, trusteePath, index, trusteeToRemove)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isKnownToEtmp(trusteeToRemove)) {
      trusteeToRemove.transform(addEntityEnd(trusteeToRemove, endDate)) match {
        case JsSuccess(endedTrusteeJson, _) =>
          val trustees: Reads[JsObject] =
            trusteePath.json.update(of[JsArray]
              .map {
                trustees => trustees :+ endedTrusteeJson
              }
            )
          input.transform(trustees)

        case e: JsError => e
      }
    } else {
      // Do not add the trustee back into the record
      super.applyDeclarationTransform(input)
    }
  }

  private def isKnownToEtmp(json: JsValue): Boolean = {
    json.transform((__ \ 'trusteeInd \ 'lineNo).json.pick).isSuccess |
      json.transform((__ \ 'trusteeOrg \ 'lineNo).json.pick).isSuccess
  }

  private def addEntityEnd(trusteeToRemove: JsValue, endDate: LocalDate): Reads[JsObject] = {
    val entityEndPath =
      if (trusteeToRemove.transform((__ \ 'trusteeInd).json.pick).isSuccess) {
        (__ \ 'trusteeInd \ 'entityEnd).json
      } else {
        (__ \ 'trusteeOrg \ 'entityEnd).json
      }

    __.json.update(entityEndPath.put(Json.toJson(endDate)))
  }
}

object RemoveTrusteeTransform {

  val key = "RemoveTrusteeTransform"

  implicit val dateFormat: Format[DateTime] = Format[DateTime](Reads.jodaDateReads(dateTimePattern), Writes.jodaDateWrites(dateTimePattern))

  implicit val format: Format[RemoveTrusteeTransform] = Json.format[RemoveTrusteeTransform]
}
