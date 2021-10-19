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

import config.AppConfig
import connector.NonRepudiationConnector
import models.nonRepudiation.{MetaData, NRSSubmission, NoActiveSessionResponse, NrsResponse, SearchKey, SearchKeys}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utils.RetryHelper

import java.time.ZoneOffset
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NonRepudiationService @Inject()(connector: NonRepudiationConnector,
                                      localDateTimeService: LocalDateTimeService,
                                      payloadEncodingService: PayloadEncodingService,
                                      config: AppConfig)(implicit val ec: ExecutionContext) extends RetryHelper {

  private final def sendEvent(payload: JsValue,
                              notableEvent: String,
                              searchKey: SearchKey,
                              searchValue: String
                             )(implicit hc: HeaderCarrier): Future[NrsResponse] = {

    hc.authorization match {
      case Some(token) =>
        val encodedPayload = payloadEncodingService.encode(payload)
        val payloadChecksum = payloadEncodingService.generateChecksum(payload)

        val event = NRSSubmission(
          encodedPayload,
          MetaData(
            "trs",
            notableEvent,
            "application/json; charset=utf-8",
            payloadChecksum,
            localDateTimeService.now(ZoneOffset.UTC),
            Json.obj(),
            token.value,
            Json.obj(),
            SearchKeys(searchKey, searchValue)
          ))

        val f: () => Future[NrsResponse] = () => connector.nonRepudiate(event)

        retryOnFailure(f, config)

      case None => Future.successful(NoActiveSessionResponse)
    }
  }

  def register(trn: String, payload: JsValue)(implicit hc: HeaderCarrier): Future[NrsResponse] =
    sendEvent(payload, "trs-registration", SearchKey.TRN, trn)

  def maintain(identifier: String, payload: JsValue)(implicit hc: HeaderCarrier): Future[NrsResponse] = {

    val isUtr = (x: String) => x.length != 15

    if (isUtr(identifier)) {
      sendEvent(payload, "trs-update-taxable", SearchKey.UTR, identifier)
    } else {
      sendEvent(payload, "trs-update-non-taxable", SearchKey.URN, identifier)
    }
  }
}
