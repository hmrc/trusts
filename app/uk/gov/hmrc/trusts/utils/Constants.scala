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

package uk.gov.hmrc.trusts.utils

object Constants {

  val ALREADY_REGISTERED_CODE = "ALREADY_REGISTERED"
  val ALREADY_REGISTERED_TRUSTS_MESSAGE = "The trust is already registered."
  val ALREADY_REGISTERED_ESTATE_MESSAGE = "The estate is already registered."

  val NO_MATCH_CODE = "NO_MATCH"
  val NO_MATCH_MESSAGE = "No match has been found in HMRC's records."
  val INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR"
  val INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error."


  val INSUFFICIENT_ENROLMENT_MESSAGE = "Insufficient enrolment for authorised user."
  val UNAUTHORISED = "UNAUTHORISED"

  val NO_DRAFT_ID = "NO_DRAFT_ID"
  val NO_DRAFT_ID_MESSAGE = "No draft registration identifier provided."

  val CONTENT_TYPE = "Content-Type"
  val CONTENT_TYPE_JSON = "application/json; charset=utf-8"

  val INVALID_UTR_CODE = "INVALID_UTR"
  val INVALID_UTR_MESSAGE = "The UTR provided is invalid."

}
