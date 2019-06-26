/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.models.auditing

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.trusts.models.{EstateRegistration, Registration, RegistrationResponse}

case class TrustRegistrationSubmissionAuditEvent(
                                                  registration: Registration,
                                                  draftId : String,
                                                  internalAuthId : String,
                                                  response: RegistrationResponse
                                                )

object TrustRegistrationSubmissionAuditEvent {

  implicit val formats: Format[TrustRegistrationSubmissionAuditEvent] = Json.format[TrustRegistrationSubmissionAuditEvent]

}

case class EstateRegistrationSubmissionAuditEvent(
                                                  registration: EstateRegistration,
                                                  draftId : String,
                                                  internalAuthId : String,
                                                  response: RegistrationResponse
                                                )

object EstateRegistrationSubmissionAuditEvent {

  implicit val formats: Format[EstateRegistrationSubmissionAuditEvent] = Json.format[EstateRegistrationSubmissionAuditEvent]

}