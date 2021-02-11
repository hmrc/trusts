/*
 * Copyright 2021 HM Revenue & Customs
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

package models.auditing

import models.Registration
import models.registration.{RegistrationFailureResponse, RegistrationTrnResponse}
import play.api.libs.json.{Format, JsValue, Json}

case class TrustRegistrationSubmissionAuditEvent(registration: Registration,
                                                 draftId: String,
                                                 internalAuthId: String,
                                                 response: RegistrationTrnResponse)

object TrustRegistrationSubmissionAuditEvent {
  implicit val formats: Format[TrustRegistrationSubmissionAuditEvent] = Json.format[TrustRegistrationSubmissionAuditEvent]
}

case class TrustRegistrationFailureAuditEvent(registration: Registration,
                                              draftId: String,
                                              internalAuthId: String,
                                              response: RegistrationFailureResponse)

object TrustRegistrationFailureAuditEvent {
  implicit val formats: Format[TrustRegistrationFailureAuditEvent] = Json.format[TrustRegistrationFailureAuditEvent]
}

case class GetTrustOrEstateAuditEvent(request: JsValue,
                                      internalAuthId: String,
                                      response: JsValue)

object GetTrustOrEstateAuditEvent {
  implicit val formats: Format[GetTrustOrEstateAuditEvent] = Json.format[GetTrustOrEstateAuditEvent]
}
