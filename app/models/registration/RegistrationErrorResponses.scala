/*
 * Copyright 2024 HM Revenue & Customs
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

package models.registration

import play.api.libs.json.{Json, OFormat}
import utils.Constants._

case class RegistrationErrorResponse(code: String, message: String)

object RegistrationErrorResponse {
  implicit val formats: OFormat[RegistrationErrorResponse] = Json.format[RegistrationErrorResponse]
}

object ApiResponse {

  def alreadyRegisteredTrustsResponse: RegistrationErrorResponse    =
    RegistrationErrorResponse(ALREADY_REGISTERED_CODE, ALREADY_REGISTERED_TRUSTS_MESSAGE)

  def alreadyRegisteredEstateResponse: RegistrationErrorResponse    =
    RegistrationErrorResponse(ALREADY_REGISTERED_CODE, ALREADY_REGISTERED_ESTATE_MESSAGE)

  def invalidUTRErrorResponse: RegistrationErrorResponse            =
    RegistrationErrorResponse(INVALID_UTR_CODE, INVALID_UTR_MESSAGE)

  def noMatchRegistrationResponse: RegistrationErrorResponse        =
    RegistrationErrorResponse(NO_MATCH_CODE, NO_MATCH_MESSAGE)

  def internalServerErrorResponse: RegistrationErrorResponse        =
    RegistrationErrorResponse(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR_MESSAGE)

  def insufficientEnrolmentErrorResponse: RegistrationErrorResponse =
    RegistrationErrorResponse(UNAUTHORISED, INSUFFICIENT_ENROLMENT_MESSAGE)

  def noDraftIdProvided: RegistrationErrorResponse                  = RegistrationErrorResponse(NO_DRAFT_ID, NO_DRAFT_ID_MESSAGE)
}
