/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


sealed trait ExistingTrustResponse

object ExistingTrustResponse {

  final case class Success(`match`: Boolean) extends ExistingTrustResponse

  final case class Failure(message: String, code: String) extends ExistingTrustResponse

  implicit lazy val httpReads: HttpReads[ExistingTrustResponse] =
    new HttpReads[ExistingTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): ExistingTrustResponse = {
        response.status match {
          case OK ⇒
            response.json.as[Success]
          case BAD_REQUEST ⇒
            Failure(response.body, "")
          case status ⇒
            Failure(response.body, "")
        }
      }
    }
  implicit val successReads: Reads[Success] = Json.reads[Success]

}
