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

package uk.gov.hmrc.trusts.services

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.retrieve.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class AuthService  @Inject()(override val authConnector :AuthConnector) extends  AuthorisedFunctions {


   def authorisedUser()(f: Boolean => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(affinityGroup) {

      response => response match {
        case Some(AffinityGroup.Organisation)=>f(true)
        case _=> f(false)
      }
    } recover {
      case e: Exception => {
        Logger.error(s"[AuthService] Exception received ${e.getMessage}.")
        Logger.error(s"[AuthService] Returning unauthorized.")
        Results.Unauthorized
      }
    }
  }
}


