/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import connector.NonRepudiationConnector
import javax.inject.Inject
import models.nonRepudiation.{MetaData, NRSSubmission, NrsResponse, SearchKeys}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class NonRepudiationService @Inject()(connector: NonRepudiationConnector, localDateTimeService: LocalDateTimeService) {

  def register(payload: JsValue)(implicit hc: HeaderCarrier): Future[NrsResponse] = {

    val bearerToken = hc.authorization.get.value
    val encodedPayload = Json.stringify(payload)

   val event = NRSSubmission(
      encodedPayload,
        MetaData("trs",
        "trs-registration",
        "application/json; charset=utf-8",
        "checkSum",
        localDateTimeService.now,
        Json.obj(),
        bearerToken,
        Json.obj(),
        SearchKeys("trn")
      ))

    connector.nonRepudiate(event)

  }
}
