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

package uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust

import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{ResponseHeader, _}
import uk.gov.hmrc.trusts.transformers.mdtp.Trustees
import uk.gov.hmrc.trusts.transformers.mdtp.beneficiaries.Beneficiaries
import uk.gov.hmrc.trusts.transformers.mdtp.settlors.Settlors

trait GetTrustResponse

trait GetTrustSuccessResponse extends GetTrustResponse {
  def responseHeader: ResponseHeader
}

object GetTrustSuccessResponse {

  implicit val writes: Writes[GetTrustSuccessResponse] = Writes{
    case TrustProcessedResponse(trust, header) => Json.obj("responseHeader" -> header, "getTrust" -> Json.toJson(trust.as[GetTrust]))
    case TrustFoundResponse(header) => Json.obj("responseHeader" -> header)
  }

  implicit val reads: Reads[GetTrustSuccessResponse] = new Reads[GetTrustSuccessResponse] {
    override def reads(json: JsValue): JsResult[GetTrustSuccessResponse] = {
      val header = (json \ "responseHeader").validate[ResponseHeader]
      (json \ "trustOrEstateDisplay").toOption match {
        case None => header.map(TrustFoundResponse)
        case Some(x) =>
          x.validate[GetTrust] match {
            case JsSuccess(_, _) =>
              header.map(h => TrustProcessedResponse(x, h))
            case x : JsError => x
          }

      }
    }
  }
}

case class TrustProcessedResponse(getTrust: JsValue,
                              responseHeader: ResponseHeader) extends GetTrustSuccessResponse {

  def transform : JsResult[TrustProcessedResponse] = {
    getTrust.transform(
      Trustees.transform(getTrust) andThen
      Beneficiaries.transform(getTrust) andThen
      Settlors.transform(getTrust)
    ).map {
      json =>
        TrustProcessedResponse(json, responseHeader)
    }
  }

}

object TrustProcessedResponse {
  val mongoWrites: Writes[TrustProcessedResponse] = new Writes[TrustProcessedResponse] {
    override def writes(o: TrustProcessedResponse): JsValue = Json.obj(
      "responseHeader" -> Json.toJson(o.responseHeader)(ResponseHeader.mongoWrites),
      "trustOrEstateDisplay" -> o.getTrust)
  }
}

case class TrustFoundResponse(responseHeader: ResponseHeader) extends GetTrustSuccessResponse

object GetTrustResponse {

  implicit lazy val httpReads: HttpReads[GetTrustResponse] = new HttpReads[GetTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): GetTrustResponse = {
        Logger.info(s"[GetTrustResponse] response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.validate[GetTrustSuccessResponse] match {
              case JsSuccess(trustFound,_) => trustFound
              case JsError(errors) =>
                  Logger.error(s"[GetTrustResponse] Cannot parse as TrustFoundResponse due to $errors")
                  NotEnoughDataResponse
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
          case NOT_FOUND =>
            ResourceNotFoundResponse
          case SERVICE_UNAVAILABLE =>
            ServiceUnavailableResponse
          case _ =>
            InternalServerErrorResponse
        }
      }
    }
}
