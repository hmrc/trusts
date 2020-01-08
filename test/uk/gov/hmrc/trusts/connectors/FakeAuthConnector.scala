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

package uk.gov.hmrc.trusts.connectors

import com.google.inject.Inject
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

final protected class FakeAuthConnector @Inject()(exception : Option[AuthorisationException] = None) extends AuthConnector {

  private def success : Any = ()

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    exception.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))
  }
}

object FakeAuthConnector {

  def apply() = new FakeAuthConnector()

  def apply(exception: AuthorisationException) = new FakeAuthConnector(Some(exception))

}