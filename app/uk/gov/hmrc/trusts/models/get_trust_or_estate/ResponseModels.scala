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

package uk.gov.hmrc.trusts.models.get_trust_or_estate

import play.api.libs.json.JsValue
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustResponse

sealed trait TrustErrorResponse extends GetTrustResponse with GetEstateResponse

case object InvalidUTRResponse extends TrustErrorResponse

case object InvalidRegimeResponse extends TrustErrorResponse

case object BadRequestResponse extends TrustErrorResponse

case object ResourceNotFoundResponse extends TrustErrorResponse

case object InternalServerErrorResponse extends TrustErrorResponse

case class NotEnoughDataResponse(json: JsValue, errors: JsValue) extends TrustErrorResponse

case object ServiceUnavailableResponse extends TrustErrorResponse

case class TransformationErrorResponse(errors: String) extends TrustErrorResponse
