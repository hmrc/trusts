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

package controllers.actions

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{Request, Result, _}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.registration.ApiResponse._
import models.requests.{CredentialData, IdentifierRequest}
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction with AuthorisedFunctions with Logging {

  def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    val retrievals = Retrievals.internalId and
      Retrievals.affinityGroup and
      Retrievals.groupIdentifier and
      Retrievals.loginTimes and
      Retrievals.credentials and
      Retrievals.email

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised().retrieve(retrievals) {
      case Some(internalId) ~ Some(Agent) ~ groupIdentifier ~ loginTimes ~ credentials ~ email        =>
        val credential = CredentialData(groupIdentifier, loginTimes, credentials, email)
        block(IdentifierRequest(request, internalId, Session.id(hc), Agent, credential))
      case Some(internalId) ~ Some(Organisation) ~ groupIdentifier ~ loginTimes ~ credentials ~ email =>
        val credential = CredentialData(groupIdentifier, loginTimes, credentials, email)
        block(IdentifierRequest(request, internalId, Session.id(hc), Organisation, credential))
      case _                                                                                          =>
        logger.warn(
          s"[AuthenticatedIdentifierAction][invokeBlock][Session ID: ${Session.id(hc)}] user isn't authorised due to insufficient enrolments"
        )
        Future.successful(Unauthorized(Json.toJson(insufficientEnrolmentErrorResponse)))
    } recoverWith { case e: AuthorisationException =>
      logger.error(
        s"[AuthenticatedIdentifierAction][invokeBlock][AuthenticatedIdentifierAction][invokeBlock][Session ID: ${Session.id(hc)}] AuthorisationException: $e",
        e
      )
      Future.successful(Unauthorized)
    }
  }

}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]
