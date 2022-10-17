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

import base.BaseSpec
import com.google.inject.Inject
import org.scalatest.matchers.must.Matchers._
import play.api.mvc.{BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends BaseSpec {

  private implicit class HelperOps[A](a: A) {
    def ~[B](b: B) = new ~(a, b)
  }

  private val cc = stubControllerComponents()

  class Harness(authAction: IdentifierAction) {
    def onSubmit() = authAction.apply(cc.parsers.json) { _ => Results.Ok }
  }

  private type AllRetrievals = Future[Some[String] ~ Some[AffinityGroup] ~ Option[String] ~ LoginTimes ~ Option[Credentials] ~ Option[String]]

  private def minimumAuthRetrievals(affinityGroup: AffinityGroup): AllRetrievals =
    Future.successful(Some("id") ~ Some(affinityGroup) ~ None ~ LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None) ~ None ~ None)

  private def allRetrievals(affinityGroup: AffinityGroup): AllRetrievals =
    Future.successful(Some("id") ~ Some(affinityGroup) ~ Some("groupIdentifier") ~ LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None) ~ Some(Credentials("12345", "governmentGateway")) ~ Some("org@email.com"))

  private def actionToTest(authConnector: AuthConnector) = {
    new AuthenticatedIdentifierAction(authConnector, injector.instanceOf[BodyParsers.Default])(ExecutionContext.Implicits.global)
  }

  private val agentAffinityGroup = AffinityGroup.Agent
  private val orgAffinityGroup = AffinityGroup.Organisation

  "Auth Action" when {

    "retrieving data" must {

      "return email, login times, groupIdentifier and provider information" in {
        val authAction = actionToTest(new FakeAuthConnector(allRetrievals(agentAffinityGroup)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK
      }
    }

    "Agent user" must {

      "allow user to continue" in {

        val authAction = actionToTest(new FakeAuthConnector(minimumAuthRetrievals(agentAffinityGroup)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK
      }

    }

    "Org user with no enrolments" must {

      "allow user to continue" in {

        val authAction = actionToTest(new FakeAuthConnector(minimumAuthRetrievals(orgAffinityGroup)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK
      }

    }

    "Individual user" must {

      "redirect the user to the unauthorised page" in {

        val authAction = actionToTest(new FakeAuthConnector(minimumAuthRetrievals(Individual)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)
        status(result) mustBe UNAUTHORIZED
      }

    }

    "the user hasn't logged in" must {

      "redirect the user to log in " in {

        val authAction = actionToTest(new FakeFailingAuthConnector(new MissingBearerToken))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED
      }
    }

    "the user's session has expired" must {

      "redirect the user to log in " in {

        val authAction = actionToTest(new FakeFailingAuthConnector(new BearerTokenExpired))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED
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

