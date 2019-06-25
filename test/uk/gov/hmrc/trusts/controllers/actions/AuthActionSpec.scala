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

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.mvc.{BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.BaseSpec

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends BaseSpec {

  implicit lazy val mtrlzr = injector.instanceOf[Materializer]

  class Harness(authAction: IdentifierAction) {
    def onSubmit() = authAction.apply(BodyParsers.parse.json) { _ => Results.Ok }
  }

  private def authRetrievals(affinityGroup: AffinityGroup = AffinityGroup.Individual, enrolment: Enrolments = noEnrollment) =
    Future.successful(new ~(new ~(Some("id"), Some(affinityGroup)), enrolment))

  private val agentAffinityGroup = AffinityGroup.Agent
  private val orgAffinityGroup = AffinityGroup.Organisation
  private val noEnrollment = Enrolments(Set())
  private val agentEnrolment = Enrolments(Set(Enrolment("HMRC-AS-AGENT",List(EnrolmentIdentifier("AgentReferenceNumber","SomeVal")),"Activated",None)))

  "Auth Action" when {

    "Agent user has not enrolled in Agent Services Account" must {

      "redirect the user to the create agent services page" in {
        
        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(agentAffinityGroup, noEnrollment)), appConfig)(global)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)
        status(result) mustBe UNAUTHORIZED

        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "UNAUTHORISED"
        (output \ "message").as[String] mustBe "Insufficient enrolment for authorised user."

        application.stop()
      }

    }

    "Agent user has correct enrolled in Agent Services Account" must {

      "allow user to continue" in {

        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(agentAffinityGroup, agentEnrolment)), appConfig)(global)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK

        application.stop()
      }

    }

    "Org user with no enrolments" must {

      "allow user to continue" in {

        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(orgAffinityGroup, noEnrollment)), appConfig)(global)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK

        application.stop()
      }

    }

    "Individual user" must {

      "redirect the user to the unauthorised page" in {
        
        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(enrolment = noEnrollment)), appConfig)(global)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)
        status(result) mustBe UNAUTHORIZED

        application.stop()
      }

    }

    "the user hasn't logged in" must {

      "redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig)(global)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED

        application.stop()
      }
    }

    "the user's session has expired" must {

      "redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig)(global)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED

        application.stop()
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}


class FakeAuthConnector(stubbedRetrievalResult: Future[_]) extends AuthConnector {

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    stubbedRetrievalResult.map(_.asInstanceOf[A])
  }

}

