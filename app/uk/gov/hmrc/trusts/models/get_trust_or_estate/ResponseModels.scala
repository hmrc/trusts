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

package uk.gov.hmrc.trusts.models.get_trust_or_estate

import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.GetEstateResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustResponse

sealed trait ErrorResponse

case object InvalidUTRResponse extends GetTrustResponse with GetEstateResponse

case object InvalidRegimeResponse extends GetTrustResponse with GetEstateResponse

case object BadRequestResponse extends GetTrustResponse with GetEstateResponse

case object ResourceNotFoundResponse extends GetTrustResponse with GetEstateResponse

case object InternalServerErrorResponse extends GetTrustResponse with GetEstateResponse

case object ServiceUnavailableResponse extends GetTrustResponse with GetEstateResponse
