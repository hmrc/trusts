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
import models.nonRepudiation._
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import retry.RetryHelper
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import java.time.ZoneOffset
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class NonRepudiationService @Inject()(connector: NonRepudiationConnector,
                                      localDateTimeService: LocalDateTimeService,
                                      payloadEncodingService: PayloadEncodingService,
                                      retryHelper: RetryHelper)(implicit val ec: ExecutionContext) extends Logging {

  private def authorityData(payload: JsValue)(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): JsValue = {

    val takeFirstForwardedFor: Option[String] = hc.forwarded.map { forwardedFor =>
      Try(forwardedFor.value.split(",").head)
        .getOrElse(forwardedFor.value)
    }

    val commonAuthorityData = Json.obj(
      "internalId" -> request.internalId,
      "affinityGroup" -> request.affinityGroup,
      "deviceId" -> s"${hc.deviceID.getOrElse("No Device ID")}",
      "clientIP" -> s"${hc.trueClientIp.getOrElse(takeFirstForwardedFor.getOrElse("No Client IP"))}",
      "clientPort" -> s"${hc.trueClientPort.getOrElse("No Client Port")}",
      "sessionId" -> s"${hc.sessionId.map(_.value).getOrElse("No Session ID")}",
      "requestId" -> s"${hc.requestId.map(_.value).getOrElse("No Request ID")}",
      "declaration" -> getDeclaration(payload)
    )

    if (request.affinityGroup == Agent) {
      commonAuthorityData ++ Json.obj("agentDetails" -> getAgentDetails(payload))
    } else {
      commonAuthorityData
    }
  }

  private final def sendEvent(payload: JsValue,
                              notableEvent: String,
                              searchKey: SearchKey,
                              searchValue: String
                             )(implicit hc: HeaderCarrier,
                               request: IdentifierRequest[_]): Future[NrsResponse] = {
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
            authorityData(payload),
            token.value,
            JsObject(request.headers.toMap.map(header => header._1 -> JsString(header._2 mkString ","))),
            SearchKeys(searchKey, searchValue)
          )
        )

        scheduleNrsSubmission(event)
      case None =>
        Future.successful(NoActiveSessionResponse)
    }
  }

  private def scheduleNrsSubmission(event: NRSSubmission)(implicit hc: HeaderCarrier): Future[NrsResponse] = {

    def f: () => Future[NrsResponse] = () => connector.nonRepudiate(event)

    retryHelper.retryOnFailure(f).map {
      p =>
        p.result match {
          case Some(value) => value.asInstanceOf[NrsResponse]
          case None => InternalServerErrorResponse
        }
    }
  }

  private def handleCallback(f: Future[NrsResponse]): Unit = {
    f onComplete {
      case Success(value) =>
        // TXM success
        logger.info(s"[NonRepudiationService] NRS submission completed, result was $value")
        ()
      case Failure(exception) =>
        // Txm failure event
        logger.info(s"[NonRepudiationService] NRS submission failed due to ${exception.getMessage}")
        ()
    }
  }

  def getDeclaration(payload: JsValue): JsValue = {
    payload.transform(
      (__ \ "declaration" \ "name").json.pick
    ).getOrElse(JsString("No Declaration Name"))
  }

  def getAgentDetails(payload: JsValue): Option[JsValue] =
    payload.transform((__ \ "agentDetails").json.pick).asOpt

  def register(trn: String, payload: JsValue)(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Future[NrsResponse] =
    sendEvent(payload, "trs-registration", SearchKey.TRN, trn)

  def maintain(identifier: String, payload: JsValue)(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Future[NrsResponse] = {

    val isUtr = (x: String) => x.length != 15

    if (isUtr(identifier)) {
      sendEvent(payload, "trs-update-taxable", SearchKey.UTR, identifier)
    } else {
      sendEvent(payload, "trs-update-non-taxable", SearchKey.URN, identifier)
    }
  }
}
