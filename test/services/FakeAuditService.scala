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

package services

import models.Registration
import models.registration.RegistrationResponse
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject

class FakeAuditService @Inject()(auditConnector: AuditConnector)
  extends AuditService(auditConnector) {

  override def audit(event: String, registration: Registration, draftId: String, internalId: String, response: RegistrationResponse)
                    (implicit hc: HeaderCarrier): Unit = ()

  override def audit(event: String, request: JsValue, internalId: String, response: JsValue)
                    (implicit hc: HeaderCarrier): Unit = ()

  override def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: String)
                                 (implicit hc: HeaderCarrier): Unit = ()

}
