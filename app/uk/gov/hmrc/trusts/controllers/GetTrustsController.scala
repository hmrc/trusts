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

package uk.gov.hmrc.trusts.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.trusts.services.{AuthService, DesService}
import uk.gov.hmrc.trusts.actions.ValidateUTRAction
import uk.gov.hmrc.trusts.models.TrustFoundResponse

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class GetTrustsController @Inject()(authService : AuthService,
                                    desService: DesService) extends BaseController {

  def get(utr: String): Action[AnyContent] = (Action andThen ValidateUTRAction(utr)).async { implicit request =>

    import authService._

    authorisedUser() {
      _ =>
        //TODO: Find out what needs to be returned in body
        desService.getTrustInfo(utr) map {
          case response: TrustFoundResponse => Ok(response)
          case _ => InternalServerError
        }
    }
  }
}
