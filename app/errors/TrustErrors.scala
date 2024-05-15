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

package errors

import play.api.mvc.Result

sealed trait TrustErrors

final case class ServerError(message: String = "") extends TrustErrors
final case class NotFoundError(message: String) extends TrustErrors
final case class ErrorWithResult(result: Result) extends TrustErrors

case object OrchestratorToTaxableFailure extends TrustErrors
case object TaxEnrolmentFailure extends TrustErrors

trait VariationErrors extends TrustErrors

case class VariationFailureForAudit(reason: VariationErrors, message: String) extends TrustErrors

case object ServiceNotAvailableErrorResponse extends VariationErrors
case object InternalServerErrorResponse extends VariationErrors
case object InvalidCorrelationIdErrorResponse extends VariationErrors
case object DuplicateSubmissionErrorResponse extends VariationErrors
case object BadRequestErrorResponse extends VariationErrors
case object EtmpCacheDataStaleErrorResponse extends VariationErrors
