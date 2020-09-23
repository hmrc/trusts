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
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.trusts.models.ApiResponse._
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                              val parser: BodyParsers.Default)
                                             (implicit val executionContext: ExecutionContext)
  extends IdentifierAction with AuthorisedFunctions with Logging {

  def invokeBlock[A](request: Request[A],
                     block: IdentifierRequest[A] => Future[Result]) : Future[Result] = {

    val retrievals = Retrievals.internalId and
                     Retrievals.affinityGroup

    implicit val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    authorised().retrieve(retrievals) {
      case Some(internalId) ~ Some(Agent) =>
        block(IdentifierRequest(request, internalId, Agent))
      case Some(internalId) ~ Some(Organisation) =>
        block(IdentifierRequest(request, internalId, Organisation))
      case _ =>
        logger.info(s"[IdentifierAction] Insufficient enrolment")
        Future.successful(Unauthorized(Json.toJson(insufficientEnrolmentErrorResponse)))
    } recoverWith {
      case e : AuthorisationException =>
        logger.info(s"[IdentifierAction] AuthorisationException: $e")
        Future.successful(Unauthorized)
    }
  }

}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]
