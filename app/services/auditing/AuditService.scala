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

import models.auditing._
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}

@Singleton
class AuditService @Inject()(auditConnector: AuditConnector){

  import scala.concurrent.ExecutionContext.Implicits._

  def audit(event: String,
            request: JsValue,
            internalId: String,
            response: JsValue
           )
           (implicit hc: HeaderCarrier): Unit = {

    val auditPayload = RequestAndResponseAuditEvent(
      request = request,
      internalAuthId = internalId,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def auditErrorResponse(eventName: String,
                         request: JsValue,
                         internalId: String,
                         errorReason: String
                        )
                        (implicit hc: HeaderCarrier): Unit = {

    audit(
      event = eventName,
      request = request,
      internalId = internalId,
      response = Json.obj("errorReason" -> errorReason)
    )
  }


}
