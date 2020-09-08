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

package uk.gov.hmrc.trusts.controllers.actions

import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.trusts.models.ApiResponse.invalidUTRErrorResponse

import scala.concurrent.Future

case class ValidateUTRAction(input: String) extends ActionFilter[Request] with ActionBuilder[Request] {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful{
    if (input.matches("^[0-9]{10}$")) {
      None
    } else {
      Some(BadRequest(toJson(invalidUTRErrorResponse)))
    }
  }
}