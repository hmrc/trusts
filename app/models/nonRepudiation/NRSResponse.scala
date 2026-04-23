/*
 * Copyright 2026 HM Revenue & Customs
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

package models.nonRepudiation

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import retry.RetryPolicy
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait NRSResponse extends RetryPolicy {
  val statusCode: Int
}

object NRSResponse extends Logging {

  final private val CHECKSUM_FAILED = 419

  implicit lazy val httpReads: HttpReads[NRSResponse] =
    (_: String, _: String, response: HttpResponse) =>
      response.status match {
        case ACCEPTED            =>
          response.json.as[Success]
        case BAD_REQUEST         =>
          BadRequest
        case UNAUTHORIZED        =>
          logger.error("[NRSResponse] No X-API-Key provided or it is invalid.")
          Unauthorised
        case BAD_GATEWAY         =>
          BadGateway
        case SERVICE_UNAVAILABLE =>
          logger.error("[NRSResponse] Service unavailable response from NRS.")
          ServiceUnavailable
        case GATEWAY_TIMEOUT     =>
          GatewayTimeout
        case CHECKSUM_FAILED     =>
          ChecksumFailed
        case status              =>
          logger.error(s"[NRSResponse] Error response from NRS with status $status and body: ${response.body}")
          InternalServerError
      }

  implicit val writes: Writes[NRSResponse] = Writes {
    case Success(nrSubmissionId) => Json.obj("nrSubmissionId" -> nrSubmissionId, "code" -> ACCEPTED)
    case e                       => Json.obj("code" -> e.statusCode, "reason" -> e.toString)
  }

  implicit val reads: Reads[NRSResponse] = (json: JsValue) => json.validate[Success]

  case class Success(nrSubmissionId: String) extends NRSResponse with RetryPolicy {
    override val retry           = false
    override val statusCode: Int = ACCEPTED
  }

  object Success {
    implicit val formats: Format[Success] = Json.format[Success]
  }

  case object BadRequest extends NRSResponse with RetryPolicy {
    override val retry           = false
    override val statusCode: Int = BAD_REQUEST
  }

  case object BadGateway extends NRSResponse with RetryPolicy {
    override val retry           = true
    override val statusCode: Int = BAD_GATEWAY
  }

  case object Unauthorised extends NRSResponse with RetryPolicy {
    override val retry           = false
    override val statusCode: Int = UNAUTHORIZED
  }

  case object ServiceUnavailable extends NRSResponse with RetryPolicy {
    override val retry           = true
    override val statusCode: Int = SERVICE_UNAVAILABLE
  }

  case object GatewayTimeout extends NRSResponse with RetryPolicy {
    override val retry           = true
    override val statusCode: Int = GATEWAY_TIMEOUT
  }

  case object InternalServerError extends NRSResponse with RetryPolicy {
    override val retry           = true
    override val statusCode: Int = INTERNAL_SERVER_ERROR
  }

  case object ChecksumFailed extends NRSResponse with RetryPolicy {
    override val retry           = false
    override val statusCode: Int = CHECKSUM_FAILED
  }

  case object NoActiveSession extends NRSResponse with RetryPolicy {
    override val retry           = false
    override val statusCode: Int = INTERNAL_SERVER_ERROR
  }

}
