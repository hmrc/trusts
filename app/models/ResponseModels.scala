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

package models

import play.api.libs.json.{Json, OFormat}
import utils.Constants._

case class ErrorResponse(code: String, message: String)
object ErrorResponse {
  implicit val formats: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

object ApiResponse {
  def alreadyRegisteredEstateResponse = ErrorResponse(ALREADY_REGISTERED_CODE, ALREADY_REGISTERED_ESTATE_MESSAGE)
  def invalidUTRErrorResponse = ErrorResponse(INVALID_UTR_CODE, INVALID_UTR_MESSAGE)
  def noMatchRegistrationResponse = ErrorResponse(NO_MATCH_CODE, NO_MATCH_MESSAGE)
  def internalServerErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR_MESSAGE)
  def insufficientEnrolmentErrorResponse = ErrorResponse(UNAUTHORISED, INSUFFICIENT_ENROLMENT_MESSAGE)
  def noDraftIdProvided = ErrorResponse(NO_DRAFT_ID, NO_DRAFT_ID_MESSAGE)
}


