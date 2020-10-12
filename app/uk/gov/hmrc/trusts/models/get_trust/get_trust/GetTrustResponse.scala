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

package uk.gov.hmrc.trusts.models.get_trust.get_trust

import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.models
import uk.gov.hmrc.trusts.models.existing_trust.DesErrorResponse
import uk.gov.hmrc.trusts.models.get_trust._

trait GetTrustResponse

trait GetTrustSuccessResponse extends GetTrustResponse {
  def responseHeader: ResponseHeader
}

object GetTrustSuccessResponse {

  implicit val writes: Writes[GetTrustSuccessResponse] = Writes{
    case TrustProcessedResponse(trust, header) => Json.obj("responseHeader" -> header, "getTrust" -> Json.toJson(trust.as[GetTrust]))
    case TrustFoundResponse(header) => Json.obj("responseHeader" -> header)
  }

  implicit val reads: Reads[GetTrustSuccessResponse] = (json: JsValue) => {
    val header = (json \ "responseHeader").validate[ResponseHeader]
    (json \ "trustOrEstateDisplay").toOption match {
      case None => header.map(TrustFoundResponse)
      case Some(x) =>
        x.validate[GetTrust] match {
          case JsSuccess(_, _) =>
            header.map(h => models.get_trust.get_trust.TrustProcessedResponse(x, h))
          case x: JsError => x
        }

    }
  }
}

object GetTrustResponse extends Logging {

  implicit lazy val httpReads: HttpReads[GetTrustResponse] = new HttpReads[GetTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetTrustResponse = {
        logger.info(s"[GetTrustResponse] response status received from des: ${response.status}")
        response.status match {
          case OK =>
            parseOkResponse(response)
          case BAD_REQUEST =>
            parseBadRequestResponse(response)
          case NOT_FOUND =>
            ResourceNotFoundResponse
          case SERVICE_UNAVAILABLE =>
            ServiceUnavailableResponse
          case _ =>
            InternalServerErrorResponse
        }
      }
    }

  private def parseOkResponse(response: HttpResponse) : GetTrustResponse = {
    response.json.validate[GetTrustSuccessResponse] match {
      case JsSuccess(trustFound, _) => trustFound
      case JsError(errors) =>
        logger.error(s"[GetTrustResponse] Cannot parse as TrustFoundResponse due to $errors")
        NotEnoughDataResponse(response.json, JsError.toJson(errors))
    }
  }

  private def parseBadRequestResponse(response: HttpResponse) : TrustErrorResponse = {
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
  }
}