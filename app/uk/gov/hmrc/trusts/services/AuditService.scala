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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.trusts.models.auditing.TrustRegistrationSubmissionAuditEvent
import uk.gov.hmrc.trusts.models.{Registration, RegistrationResponse}

class AuditService @Inject()(auditConnector: AuditConnector){

  import scala.concurrent.ExecutionContext.Implicits._

  def audit(event: String,
            registration: Registration,
            draftId: String,
            internalId: String,
            response: RegistrationResponse)(implicit hc: HeaderCarrier) = {

    val auditPayload = TrustRegistrationSubmissionAuditEvent(
      registration = registration,
      draftId = draftId,
      internalAuthId = internalId,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

}
