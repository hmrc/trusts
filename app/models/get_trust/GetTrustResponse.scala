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

package models.get_trust

import models.get_trust
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Constants._

import scala.language.implicitConversions

trait GetTrustResponse

trait GetTrustSuccessResponse extends GetTrustResponse {
  def responseHeader: ResponseHeader
}

object GetTrustSuccessResponse {

  implicit val writes: Writes[GetTrustSuccessResponse] = Writes{
    case TrustProcessedResponse(trust, header) =>
      Json.obj(
        RESPONSE_HEADER -> header,
        GET_TRUST -> Json.toJson(trust.as[GetTrust])
      )
    case TrustFoundResponse(header) =>
      Json.obj(RESPONSE_HEADER -> header)
  }

  implicit val reads: Reads[GetTrustSuccessResponse] = (json: JsValue) => {
    val header = (json \ RESPONSE_HEADER).validate[ResponseHeader]
    (json \ TRUST_OR_ESTATE_DISPLAY).toOption match {
      case None =>
        header.map(TrustFoundResponse)
      case Some(x) =>
        x.validate[GetTrust] match {
          case JsSuccess(_, _) =>
            header.map(h => get_trust.TrustProcessedResponse(x, h))
          case x: JsError => x
        }

    }
  }
}

object GetTrustResponse extends Logging {

  final val CLOSED_REQUEST_STATUS = 499

  implicit def httpReads(identifier: String): HttpReads[GetTrustResponse] = (_: String, _: String, response: HttpResponse) => {
    response.status match {
      case OK =>
        parseOkResponse(response, identifier)
      case BAD_REQUEST =>
        logger.warn(s"[UTR/URN: $identifier]" +
          s" bad request returned from des: ${response.json}")
        BadRequestResponse
      case NOT_FOUND =>
        logger.info(s"[UTR/URN: $identifier]" +
          s" trust not found in ETMP for given identifier")
        ResourceNotFoundResponse
      case SERVICE_UNAVAILABLE =>
        logger.warn(s"[UTR/URN: $identifier]" +
          s" service is unavailable, unable to get trust")
        ServiceUnavailableResponse
      case CLOSED_REQUEST_STATUS =>
        logger.warn(s"[UTR/URN: $identifier]" +
          s" service is unavailable, server closed the request")
        ClosedRequestResponse
      case status =>
        logger.error(s"[UTR/URN: $identifier]" +
          s" error occurred when getting trust, status: $status")
        InternalServerErrorResponse
    }
  }

  private def parseOkResponse(response: HttpResponse, identifier: String) : GetTrustResponse = {
    response.json.validate[GetTrustSuccessResponse] match {
      case JsSuccess(trustFound, _) => trustFound
      case JsError(errors) =>
        logger.error(s"[UTR/URN: $identifier] Cannot parse as TrustFoundResponse due to ${JsError.toJson(errors)}")
        NotEnoughDataResponse(response.json, JsError.toJson(errors))
    }
  }
}
