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

package models.existing_trust

import play.api.libs.json.{Json, OFormat}

case class ExistingTrustResponse(`match`: Boolean)

object ExistingTrustResponse {
  implicit val formats: OFormat[ExistingTrustResponse] = Json.format[ExistingTrustResponse]
}

case class DesErrorResponse(code: String, reason: String)

object DesErrorResponse {
  implicit val formats: OFormat[DesErrorResponse] = Json.format[DesErrorResponse]
}

case class HipTrustMatchResponse(success: ExistingTrustResponse)

object HipTrustMatchResponse {
  implicit val formats: OFormat[HipTrustMatchResponse] = Json.format[HipTrustMatchResponse]
}

case class HipErr(code: String, message: String, logID: String)

object HipErr {
  implicit val formats: OFormat[HipErr] = Json.format[HipErr]
}

case class HipErrResponse(error: HipErr)

object HipErrResponse {
  implicit val formats: OFormat[HipErrResponse] = Json.format[HipErrResponse]
}

case class HipCustomErr(processingDate: String, errorId: String, text: String)

object HipCustomErr {
  implicit val formats: OFormat[HipCustomErr] = Json.format[HipCustomErr]
}

case class HipCustomErrResponse(error: HipCustomErr)

object HipCustomErrResponse {
  implicit val formats: OFormat[HipCustomErrResponse] = Json.format[HipCustomErrResponse]
}
