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

package uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate

import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate._

trait GetEstateResponse

case class EstateFoundResponse(getEstate: Option[GetEstate],
                              responseHeader: ResponseHeader) extends GetEstateResponse

object EstateFoundResponse {
  implicit val writes: Writes[EstateFoundResponse] = Json.writes[EstateFoundResponse]
  implicit val reads: Reads[EstateFoundResponse] = (
    (JsPath \ "trustOrEstateDisplay").readNullable[GetEstate] and
    (JsPath \ "responseHeader").read[ResponseHeader]
  )(EstateFoundResponse.apply _)
}

object GetEstateResponse {

  implicit lazy val httpReads: HttpReads[GetEstateResponse] =
    new HttpReads[GetEstateResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetEstateResponse = {
        Logger.info(s"[GetEstateResonse] response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.validate[EstateFoundResponse] match {
              case JsSuccess(estateFound,_) =>
                Logger.info("[GetEstateResponse] response successfully parsed as EstateFoundResponse")
                estateFound
              case JsError(errors) =>
                Logger.info(s"[GetTrustResponse] Cannot parse as EstateFoundResponse due to $errors")
                InternalServerErrorResponse
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
