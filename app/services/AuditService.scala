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

package services

import models.Registration
import models.auditing.{GetTrustOrEstateAuditEvent, OrchestratorAuditEvent, TrustAuditing, TrustRegistrationFailureAuditEvent, TrustRegistrationSubmissionAuditEvent}
import models.registration.{RegistrationFailureResponse, RegistrationTrnResponse}
import models.variation.VariationResponse
import play.api.libs.json.{JsBoolean, JsPath, JsSuccess, JsValue, Json, Reads}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.Constants._
import javax.inject.Inject

class AuditService @Inject()(auditConnector: AuditConnector){

  import scala.concurrent.ExecutionContext.Implicits._

  def audit(event: String,
            registration: Registration,
            draftId: String,
            internalId: String,
            response: RegistrationTrnResponse)(implicit hc: HeaderCarrier): Unit = {

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

  def audit(event: String,
            registration: Registration,
            draftId: String,
            internalId: String,
            response: RegistrationFailureResponse)(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = TrustRegistrationFailureAuditEvent(
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

  def audit(event: String,
            request: JsValue,
            internalId: String,
            response: JsValue)(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = GetTrustOrEstateAuditEvent(
      request = request,
      internalAuthId = internalId,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def auditErrorResponse(eventName: String, request: JsValue, internalId: String, errorReason: String)(implicit hc: HeaderCarrier): Unit = {

    audit(
      event = eventName,
      request = request,
      internalId = internalId,
      response = Json.obj("errorReason" -> errorReason)
    )
  }

  def auditVariationSubmitted(internalId: String,
                              payload: JsValue,
                              variationResponse: VariationResponse
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

    val trustTaxablePick: Reads[JsBoolean] = (TRUST \ DETAILS \ TAXABLE).json.pick[JsBoolean]
    val response = payload.transform(trustTaxablePick) match {
      case JsSuccess(JsBoolean(trustTaxable), _) =>
        Json.obj(
          "tvn" -> variationResponse.tvn,
          "trustTaxable" -> trustTaxable
        )
      case _ => Json.toJson(variationResponse)
    }

    audit(
      event = event,
      request = payload,
      internalId = internalId,
      response = response
    )
  }

  def auditVariationTransformationError(internalId: String,
                                        utr: String,
                                        data: JsValue = Json.obj(),
                                        transforms: JsValue,
                                        errorReason: String = "",
                                        jsErrors: JsValue = Json.obj()
                                       )(implicit hc: HeaderCarrier): Unit = {
    val request = Json.obj(
      "utr" -> utr,
      "data" -> data,
      "transformations" -> transforms
    )

    val response = Json.obj(
      "errorReason" -> errorReason,
      "jsErrors" -> jsErrors
    )

    audit(
      event = TrustAuditing.TRUST_VARIATION_PREPARATION_FAILED,
      request = request,
      internalId = internalId,
      response = response
    )
  }

  def auditOrchestrator(event: String,
            request: JsValue,
            response: JsValue)(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = OrchestratorAuditEvent(
      request = request,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def auditOrchestratorTransformationToTaxableSuccess(urn: String, utr: String)(implicit hc: HeaderCarrier): Unit = {
    val request = Json.obj(
      "urn" -> urn,
      "utr" -> utr
    )

    auditOrchestrator(
      event = TrustAuditing.ORCHESTRATOR_TO_TAXABLE_SUCCESS,
      request = request,
      response = Json.obj("success" -> true)
    )
  }

  def auditOrchestratorTransformationToTaxableError(urn: String, utr: String, errorReason: String)(implicit hc: HeaderCarrier): Unit = {
    val request = Json.obj(
      "urn" -> urn,
      "utr" -> utr
    )

    auditOrchestrator(
      event = TrustAuditing.ORCHESTRATOR_TO_TAXABLE_FAILED,
      request = request,
      response = Json.obj("errorReason" -> errorReason)
    )
  }

  def auditTaxEnrolmentTransformationToTaxableError(subscriptionId: String, urn: String, errorMessage: String)(implicit hc: HeaderCarrier) : Unit = {
    auditErrorResponse(
      TrustAuditing.TAX_ENROLMENT_TO_TAXABLE_FAILED,
      Json.obj("urn" -> urn),
      subscriptionId,
      errorMessage
    )
  }
}
