/*
 * Copyright 2023 HM Revenue & Customs
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

package services.auditing

import models.auditing.{NrsAuditEvent, TrustAuditing}
import models.nonRepudiation.NotableEvent
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NRSAuditService @Inject()(auditConnector: AuditConnector)(implicit ec: ExecutionContext){

  def audit(event: NrsAuditEvent)(implicit hc: HeaderCarrier): Unit = {
    val auditType = event.metaData.notableEvent match {
      case NotableEvent.TrsRegistration => TrustAuditing.NRS_TRS_REGISTRATION
      case NotableEvent.TrsUpdateTaxable => TrustAuditing.NRS_TRS_TAXABLE_UPDATE
      case NotableEvent.TrsUpdateNonTaxable => TrustAuditing.NRS_TRS_NON_TAXABLE_UPDATE
    }
    auditConnector.sendExplicitAudit(auditType, Json.toJson(event)(NrsAuditEvent.txmWrites))
  }

}
