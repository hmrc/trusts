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

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{Request, Result, _}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
import uk.gov.hmrc.trusts.models.ApiResponse._

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                               config: AppConfig)
                                             (override implicit val executionContext: ExecutionContext)
  extends IdentifierAction with AuthorisedFunctions {

  private def authoriseAgent[A](request : Request[A],
                                enrolments : Enrolments,
                                internalId : String,
                                block: IdentifierRequest[A] => Future[Result]
                               ) : Future[Result] = {

    val hmrcAgentEnrolmentKey = "HMRC-AS-AGENT"
    val arnIdentifier = "AgentReferenceNumber"

    val unauthorisedResult = Future.successful(Unauthorized(Json.toJson(insufficientEnrolmentErrorResponse)))

    enrolments.getEnrolment(hmrcAgentEnrolmentKey).fold(
      unauthorisedResult
    ){
      agentEnrolment =>
        agentEnrolment.getIdentifier(arnIdentifier).fold(
          unauthorisedResult
        ){
          enrolmentIdentifier =>
            val arn = enrolmentIdentifier.value

            if(arn.isEmpty) {
              unauthorisedResult
            } else {
              block(IdentifierRequest(request, internalId, AffinityGroup.Agent, Some(arn)))
            }
        }
    }
  }


  def invokeBlock[A](request: Request[A],
                     block: IdentifierRequest[A] => Future[Result]) : Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    val retrievals = Retrievals.internalId and
                     Retrievals.affinityGroup and
                     Retrievals.allEnrolments

    authorised().retrieve(retrievals) {
      case Some(internalId) ~ Some(Agent) ~ enrolments =>
        authoriseAgent(request, enrolments, internalId, block)
      case Some(internalId) ~ Some(Organisation) ~ _ =>
        block(IdentifierRequest(request, internalId, AffinityGroup.Organisation))
      case _ =>
        Future.successful(Unauthorized(Json.toJson(insufficientEnrolmentErrorResponse)))
    }
  }

}

trait IdentifierAction extends ActionBuilder[IdentifierRequest]
