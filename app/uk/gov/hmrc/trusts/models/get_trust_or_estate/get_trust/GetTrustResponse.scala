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

package uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust

import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate._

trait GetTrustResponse

case class TrustFoundResponse(getTrust: Option[GetTrust],
                              responseHeader: ResponseHeader) extends GetTrustResponse

object TrustFoundResponse {
  implicit val writes: Writes[TrustFoundResponse] = Json.writes[TrustFoundResponse]
  implicit val reads: Reads[TrustFoundResponse] = (
    (JsPath \ "trustOrEstateDisplay").readNullable[GetTrust] and
    (JsPath \ "responseHeader").read[ResponseHeader]
  )(TrustFoundResponse.apply _)
}

object GetTrustResponse {

  implicit lazy val httpReads: HttpReads[GetTrustResponse] =
    new HttpReads[GetTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetTrustResponse = {
        Logger.info(s"[SubscriptionIdResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.asOpt[TrustFoundResponse] match {
              case Some(trustFound) => trustFound
              case None => InternalServerErrorResponse
            }
          case BAD_REQUEST =>
            response.json.asOpt[DesErrorResponse] match {
              case Some(desErrorResponse) =>
                desErrorResponse.code match {
                  case "INVALID_UTR" =>
                    InvalidUTRResponse
                  case "INVALID_REGIME" =>
                    InvalidRegimeResponse
                  case _ =>
                    BadRequestResponse
                }
              case None =>
                InternalServerErrorResponse
            }
          case NOT_FOUND => ResourceNotFoundResponse
          case SERVICE_UNAVAILABLE => ServiceUnavailableResponse
          case _ => InternalServerErrorResponse
        }
      }
    }
}