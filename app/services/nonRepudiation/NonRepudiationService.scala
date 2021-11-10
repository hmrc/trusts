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

package services.nonRepudiation

import connector.NonRepudiationConnector
import models.auditing.NrsAuditEvent
import models.nonRepudiation._
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames
import play.api.libs.json._
import retry.RetryHelper
import services.auditing.NRSAuditService
import services.dates.LocalDateTimeService
import services.encoding.PayloadEncodingService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Headers, Session}

import java.time.ZoneOffset
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NonRepudiationService @Inject()(connector: NonRepudiationConnector,
                                      localDateTimeService: LocalDateTimeService,
                                      payloadEncodingService: PayloadEncodingService,
                                      retryHelper: RetryHelper,
                                      nrsAuditService: NRSAuditService
                                     )
                                     (implicit val ec: ExecutionContext) extends Logging {

  private def identityData(payload: JsValue)(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): IdentityData = {

    val takeFirstForwardedFor: Option[String] = hc.forwarded.map { forwardedFor =>
      Try(forwardedFor.value.split(",").head)
        .getOrElse(forwardedFor.value)
    }

    IdentityData(
      internalId = request.internalId,
      affinityGroup = request.affinityGroup,
      deviceId = hc.deviceID.getOrElse("No Device ID"),
      clientIP = hc.trueClientIp.getOrElse(takeFirstForwardedFor.getOrElse("No Client IP")),
      clientPort = hc.trueClientPort.getOrElse("No Client Port"),
      sessionId = hc.sessionId.map(_.value).getOrElse("No Session ID"),
      requestId = hc.requestId.map(_.value).getOrElse("No Request ID"),
      credential = request.credentialData,
      declaration = getDeclaration(payload),
      agentDetails = getAgentDetails(payload)
    )
  }

  private def headers(implicit request: IdentifierRequest[_]): Map[String, JsString] = {
    val headers: Map[String, JsString] = request.headers
      .toMap
      .map(header => header._1 -> JsString(header._2 mkString ","))

    val trueUserAgent: String =
      request.headers.get(Headers.TRUE_USER_AGENT)
      .getOrElse(
        request.headers.get(HeaderNames.USER_AGENT)
          .getOrElse("No User Agent")
      )

    headers
    .-(HeaderNames.USER_AGENT)
    .+((HeaderNames.USER_AGENT, JsString(trueUserAgent)))
  }

  private final def sendEvent(payload: JsValue,
                              notableEvent: NotableEvent,
                              searchKey: SearchKey,
                              searchValue: String
                             )(implicit hc: HeaderCarrier,
                               request: IdentifierRequest[_]): Future[NRSResponse] = {
    hc.authorization match {
      case Some(token) =>
        val encodedPayload = payloadEncodingService.encode(payload)
        val payloadChecksum = payloadEncodingService.generateChecksum(payload)

        val event = NRSSubmission(
          encodedPayload,
          MetaData(
            "trs",
            notableEvent,
            JSON,
            payloadChecksum,
            localDateTimeService.now(ZoneOffset.UTC),
            identityData(payload),
            token.value,
            JsObject(headers),
            SearchKeys(searchKey, searchValue)
          )
        )

        scheduleNrsSubmission(event)
      case None =>
        Future.successful(NRSResponse.NoActiveSession)
    }
  }

  private def scheduleNrsSubmission(event: NRSSubmission)(implicit hc: HeaderCarrier): Future[NRSResponse] = {

    def f: () => Future[NRSResponse] = () => connector.nonRepudiate(event)

    retryHelper.retryOnFailure(f).map {
      finalResult =>
        auditEvent(event, finalResult)
    }
  }

  private def auditEvent(event: NRSSubmission, execution: RetryHelper.RetryExecution)(implicit hc: HeaderCarrier): NRSResponse = {
    execution.result match {
      case Some(success @ NRSResponse.Success(_)) =>
        logger.info(s"[Session ID: ${Session.id(hc)}] Successfully non-repudiated submission")
        val auditEvent = NrsAuditEvent(event.metadata, success)
        nrsAuditService.audit(auditEvent)
        success
      case Some(error: NRSResponse) =>
        logger.info(s"[Session ID: ${Session.id(hc)}] Unable to non-repudiated submission due to $error")
        val auditEvent = NrsAuditEvent(event.metadata, error)
        nrsAuditService.audit(auditEvent)
        error
      case _ =>
        logger.info(s"[Session ID: ${Session.id(hc)}] Unable to non-repudiate submission, internal server error")
        val response = NRSResponse.InternalServerError
        val auditEvent = NrsAuditEvent(event.metadata, response)
        nrsAuditService.audit(auditEvent)
        response
    }
  }

  def getDeclaration(payload: JsValue): JsValue = {
    payload.transform(
      (__ \ "declaration" \ "name").json.pick
    ).getOrElse(JsString("No Declaration Name"))
  }

  def getAgentDetails(payload: JsValue): Option[JsValue] =
    payload.transform((__ \ "agentDetails").json.pick).asOpt

  def register(trn: String, payload: JsValue)(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Future[NRSResponse] =
    sendEvent(payload, NotableEvent.TrsRegistration, SearchKey.TRN, trn)

  def maintain(identifier: String, payload: JsValue)(implicit hc: HeaderCarrier, request: IdentifierRequest[_]): Future[NRSResponse] = {

    val isUtr = (x: String) => x.length != 15

    if (isUtr(identifier)) {
      sendEvent(payload, NotableEvent.TrsUpdateTaxable, SearchKey.UTR, identifier)
    } else {
      sendEvent(payload, NotableEvent.TrsUpdateNonTaxable, SearchKey.URN, identifier)
    }
  }
}
