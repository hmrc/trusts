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

package models.get_trust

import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}

case class HipGetTrustResponse(success: GetTrustResponse) extends GetTrustResponse

case object HipGetTrustResponse {

  implicit val reads: Reads[HipGetTrustResponse] = (json: JsValue) => {
    val header = (json \ "success" \ "responseHeader").validate[ResponseHeader]

    header match {
      case JsSuccess(parsedHeader, _) =>
        (json \ "success" \ "trustOrEstateDisplay").toOption match {
          case None    =>
            JsSuccess(HipGetTrustResponse(TrustFoundResponse(parsedHeader)))
          case Some(x) =>
            x.validate[GetTrust] match {
              case JsSuccess(_, _) =>
                JsSuccess(HipGetTrustResponse(TrustProcessedResponse(x, parsedHeader)))
              case x: JsError      =>
                JsSuccess(HipGetTrustResponse(NotEnoughDataResponse(json, JsError.toJson(x))))
            }
        }
      case e: JsError  =>
        JsSuccess(
          HipGetTrustResponse(
            NotEnoughDataResponse(json, JsError.toJson(e))
          )
        )
    }
  }

}
