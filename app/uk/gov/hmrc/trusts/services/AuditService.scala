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
import play.api.libs.json.{JsPath, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models.Registration
import uk.gov.hmrc.trusts.models.auditing.{GetTrustOrEstateAuditEvent, TrustAuditing, TrustRegistrationSubmissionAuditEvent}
import uk.gov.hmrc.trusts.models.registration.RegistrationResponse
import uk.gov.hmrc.trusts.models.variation.VariationResponse

class AuditService @Inject()(auditConnector: AuditConnector, config : AppConfig){

  import scala.concurrent.ExecutionContext.Implicits._

  def audit(event: String,
            registration: Registration,
            draftId: String,
            internalId: String,
            response: RegistrationResponse)(implicit hc: HeaderCarrier): Unit = {

    if (config.auditingEnabled) {
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
    } else {
      ()
    }
  }

  def audit(event: String,
            request: JsValue,
            internalId: String,
            response: JsValue)(implicit hc: HeaderCarrier): Unit = {

    if (config.auditingEnabled) {
      val auditPayload = GetTrustOrEstateAuditEvent(
        request = request,
        internalAuthId = internalId,
        response = response
      )

      auditConnector.sendExplicitAudit(
        event,
        auditPayload
      )
    } else {
      ()
    }
  }

  def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: String)(implicit hc: HeaderCarrier): Unit = {

    if (config.auditingEnabled) {
      val response = Json.obj("errorReason" -> errorReason)

      audit(
        event = eventName,
        request = request,
        internalId = internalId,
        response = response
      )
    } else {
      ()
    }
  }

  def auditVariationSubmitted(internalId: String,
                              payload: JsValue,
                              response: VariationResponse
                             )(implicit hc: HeaderCarrier): Unit = {
    val hasField = (field: String) =>
      payload.transform((JsPath \ field).json.pick).isSuccess

    val isAgent = hasField("agentDetails")
    val isClose = hasField("trustEndDate")

    val event = (isAgent, isClose) match {
      case (false, false) => TrustAuditing.VARIATION_SUBMITTED_BY_ORGANISATION
      case (false, true) => TrustAuditing.CLOSURE_SUBMITTED_BY_ORGANISATION
      case (true, false) => TrustAuditing.VARIATION_SUBMITTED_BY_AGENT
      case _ => TrustAuditing.CLOSURE_SUBMITTED_BY_AGENT

    }

    audit(
      event = event,
      request = payload,
      internalId = internalId,
      response = Json.toJson(response)
    )
  }

}
