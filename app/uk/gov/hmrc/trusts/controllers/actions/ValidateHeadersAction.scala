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

package uk.gov.hmrc.trusts.controllers.actions

import play.api.mvc._
import uk.gov.hmrc.trusts.utils.ErrorResponses._
import uk.gov.hmrc.trusts.utils.{Headers, ValidationUtil}

import scala.concurrent.Future
import scala.util.matching.Regex

case class ValidateHeadersAction(regex: Regex) extends ActionFilter[Request] with ActionBuilder[Request] with ValidationUtil {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful{
    if (isValidCorrelationId(request.headers.get(Headers.CORRELATION_HEADER), regex)) {
      None
    } else {
      Some(invalidCorrelationIdErrorResponse)
    }
  }
}