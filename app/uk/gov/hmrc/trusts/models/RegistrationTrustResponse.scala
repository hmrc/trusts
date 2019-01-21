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

package uk.gov.hmrc.trusts.models

import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, CONFLICT, OK, SERVICE_UNAVAILABLE, FORBIDDEN}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

/**
  * Created by manish on 18/01/19.
  */

trait RegistrationTrustResponse

case class SuccessRegistrationResponse(trn : String) extends  RegistrationTrustResponse
object SuccessRegistrationResponse {
  implicit val formats = Json.format[SuccessRegistrationResponse]
}

case class ErrorRegistrationTrustsResponse(code: String,reason: String) extends RegistrationTrustResponse
object ErrorRegistrationTrustsResponse {
  implicit val formats = Json.format[ErrorRegistrationTrustsResponse]
}



object RegistrationTrustResponse {


  implicit lazy val httpReads: HttpReads[RegistrationTrustResponse] =
    new HttpReads[RegistrationTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): RegistrationTrustResponse = {
        Logger.info(s"response status received from des: ${response.status}")
        response.status match {
          case OK =>response.json.as[SuccessRegistrationResponse]
          case FORBIDDEN =>{
            response.json.asOpt[DesErrorResponse] match {
              case Some(desReponse) if desReponse.code == "ALREADY_REGISTERED"=>
                ErrorRegistrationTrustsResponse("ALREADY_REGISTERED", "Trust is already registered.")
              case _ => ErrorRegistrationTrustsResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
            }
          }
          case BAD_REQUEST => ErrorRegistrationTrustsResponse("BAD_REQUEST", "Invalid payload submitted.")
          case SERVICE_UNAVAILABLE => ErrorRegistrationTrustsResponse("SERVIVE_UNAVAILABLE", "Depedent system is not responding.")
          case status =>  ErrorRegistrationTrustsResponse("INTERNAL_SERVER_ERROR", "Internal server error.")
        }
      }
    }
  implicit val registrationDesResponseReads = RegistrationDesResponse.formats
  implicit val registrationDesErrorResponseReads = DesErrorResponse.formats

}
case class RegistrationDesResponse(trn:String)
object RegistrationDesResponse {
  implicit val formats = Json.format[DesResponse]
}

/*
case class RegistrationDesErrorResponse(code: String,reason: String )

object RegistrationDesErrorResponse {
  implicit val formats = Json.format[DesErrorResponse]
}*/
