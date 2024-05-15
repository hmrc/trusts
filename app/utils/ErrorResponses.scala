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

package utils

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results._
import models.registration.RegistrationErrorResponse

object ErrorResponses {

  protected def doErrorResponse(code: String, message: String): JsValue =
    Json.toJson(RegistrationErrorResponse(code: String, message: String))

  def invalidNameErrorResponse: Result =
    BadRequest(doErrorResponse("INVALID_NAME", "Provided name is invalid."))

  def invalidUtrErrorResponse: Result =
    BadRequest(doErrorResponse("INVALID_UTR", "Provided utr is invalid."))

  def invalidPostcodeErrorResponse: Result =
    BadRequest(doErrorResponse("INVALID_POSTCODE", "Provided postcode is invalid."))

  def invalidRequestErrorResponse: Result =
    BadRequest(doErrorResponse("BAD_REQUEST", "Provided request is invalid."))

  def invalidCorrelationIdErrorResponse: Result =
    InternalServerError(doErrorResponse("INVALID_CORRELATIONID", "Submission has not passed validation. Invalid CorrelationId."))

  def duplicateSubmissionErrorResponse: Result =
    Conflict(doErrorResponse("DUPLICATE_SUBMISSION", "Duplicate Correlation Id was submitted."))

  def internalServerErrorErrorResponse: Result =
    InternalServerError(doErrorResponse("INTERNAL_SERVER_ERROR", "Internal server error."))

  def serviceUnavailableErrorResponse: Result =
    ServiceUnavailable(doErrorResponse("SERVICE_UNAVAILABLE", "Service unavailable."))

  def etmpDataStaleErrorResponse: Result =
    BadRequest(doErrorResponse("ETMP_DATA_STALE", "ETMP returned a changed form bundle number for the trust."))
}
