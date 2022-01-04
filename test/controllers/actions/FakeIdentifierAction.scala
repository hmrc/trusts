/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup
import models.requests.{CredentialData, IdentifierRequest}
import org.joda.time.DateTime
import uk.gov.hmrc.auth.core.retrieve.LoginTimes

import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction @Inject()(bodyParsers: BodyParser[AnyContent], affinityGroup: AffinityGroup) extends IdentifierAction {

  private val credential = CredentialData(None, LoginTimes(DateTime.parse("2020-10-10"), None), None, None)

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(IdentifierRequest(request, "id", "sessionID", affinityGroup, credential))

  override def parser: BodyParser[AnyContent] =
    bodyParsers

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

}
