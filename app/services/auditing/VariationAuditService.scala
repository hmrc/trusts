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

package services.auditing

import models.auditing.{TrustAuditing, VariationAuditEvent}
import models.variation.VariationResponse
import play.api.libs.json.{JsBoolean, JsPath, JsSuccess, JsValue, Json, Reads}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.Constants.{DETAILS, TAXABLE, TRUST}

import javax.inject.Inject

class VariationAuditService @Inject()(auditConnector: AuditConnector)
  extends AuditService(auditConnector) {

  import scala.concurrent.ExecutionContext.Implicits._

  private def auditVariation(event: String,
                             request: JsValue,
                             internalId: String,
                             migrateToTaxable: Boolean,
                             response: JsValue)(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = VariationAuditEvent(
      request = request,
      internalAuthId = internalId,
      migrateToTaxable = migrateToTaxable,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def auditVariationSuccess(internalId: String,
                            migrateToTaxable: Boolean,
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

    auditVariation(
      event = event,
      request = payload,
      internalId = internalId,
      migrateToTaxable = migrateToTaxable,
      response = response
    )
  }

  def auditTransformationError(internalId: String,
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


}
