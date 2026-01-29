/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{Format, JsValue, Json}

case class RequestAndResponseAuditEvent(request: JsValue, internalAuthId: String, response: JsValue)

object RequestAndResponseAuditEvent {
  implicit val formats: Format[RequestAndResponseAuditEvent] = Json.format[RequestAndResponseAuditEvent]
}

case class VariationAuditEvent(request: JsValue, internalAuthId: String, migrateToTaxable: Boolean, response: JsValue)

object VariationAuditEvent {
  implicit val formats: Format[VariationAuditEvent] = Json.format[VariationAuditEvent]
}

case class OrchestratorAuditEvent(request: JsValue, response: JsValue)

object OrchestratorAuditEvent {
  implicit val formats: Format[OrchestratorAuditEvent] = Json.format[OrchestratorAuditEvent]
}
