/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.testOnly

import controllers._
import javax.inject.Inject
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{Session, ValidationUtil}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TrustMigrationTestController @Inject()(migrationService: TaxableMigrationService,
                                             cc: ControllerComponents
                                             )(implicit ec: ExecutionContext) extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def migrateToTaxable(subscriptionId: String, urn: String): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc : HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      logger.info(s"[TrustMigrationTestController][migrateToTaxable][Session ID: ${Session.id(hc)}][SubscriptionId: $subscriptionId, URN: $urn]" +
        s" Tax-enrolment: migration subscription callback message was: ${request.body}")
      migrationService.migrateSubscriberToTaxable(subscriptionId, urn)
      Future(Ok("Done"))
  }
}
