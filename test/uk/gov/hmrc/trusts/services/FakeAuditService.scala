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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models.{Registration, RegistrationResponse}

class FakeAuditService @Inject()(auditConnector: AuditConnector, config: AppConfig)
  extends AuditService(auditConnector, config) {

  override def audit(event: String, registration: Registration, draftId: String, internalId: String, response: RegistrationResponse)
                    (implicit hc: HeaderCarrier): Unit = ()

  override def audit(event: String, request: JsValue, internalId: String, response: JsValue)
                    (implicit hc: HeaderCarrier): Unit = ()

  override def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: String)
                                 (implicit hc: HeaderCarrier): Unit = ()

}
