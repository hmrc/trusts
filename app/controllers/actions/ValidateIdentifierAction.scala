/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import javax.inject.Inject
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc._
import models.registration.ApiResponse.invalidUTRErrorResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class ValidateIdentifierActionProvider @Inject()()
                                                (implicit val parser: BodyParsers.Default, executionContext: ExecutionContext) {

  def apply(identifier: String) = new ValidateIdentifierAction(identifier)

}

class ValidateIdentifierAction @Inject()(identifier: String)
                                        (implicit val parser: BodyParsers.Default, val executionContext: ExecutionContext)
  extends ActionFilter[Request] with ActionBuilder[Request, AnyContent] {

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful{

    identifier match {
      case Identifiers.utrPattern(_) => None
      case Identifiers.urnPattern(_) => None
      case _ => Some(BadRequest(toJson(invalidUTRErrorResponse)))
    }
  }
}

object Identifiers {
  val utrPattern: Regex = "^([0-9]){10}$".r
  val urnPattern: Regex = "^([A-Z0-9]){15}$".r
}
