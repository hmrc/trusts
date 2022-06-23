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

package connectors

import java.time.{LocalDate, LocalDateTime, ZoneId}
import connector.NonRepudiationConnector
import models.nonRepudiation._
import models.requests.CredentialData
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.{ACCEPTED, BAD_GATEWAY, BAD_REQUEST, GATEWAY_TIMEOUT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import utils.JsonUtils

class NonRepudiationConnectorSpec extends ConnectorSpecHelper with Matchers with JsonUtils {

  lazy val connector: NonRepudiationConnector = injector.instanceOf[NonRepudiationConnector]
  final val CHECKSUM_FAILED = 419

  ".nonRepudiationConnector" should {

    val identityData = IdentityData(
      internalId = "internalId",
      affinityGroup = Agent,
      deviceId = "deviceId",
      clientIP = "clientIp",
      clientPort = "clientPort",
      sessionId = "sessionId",
      requestId = "requestId",
      declaration = Json.obj("example" -> "declaration"),
      agentDetails = Some(Json.obj("example" -> "agent")),
      credential = CredentialData(groupIdentifier = None, loginTimes = LoginTimes(LocalDate.parse("2020-10-10").atStartOfDay(ZoneId.of("Europe/London")).toInstant, None), provider = None, email = None)
    )

    val payLoad = NRSSubmission("eyJtYXRjaERhdGEiOnsidXRyIjoiNTQ1NDU0MTYxNSIsIm5hbWUiOiJNYXJrIFdvbGJlcmciLCJwb3N0Q29kZSI6IlRGMyAyQlgifSwic3VibWlzc2lvbkRhdGUiOiIyMDIwLTA5LTAxIiwiY29ycmVzcG9uZGVuY2UiOnsiYWJyb2FkSW5kaWNhdG9yIjpmYWxzZSwibmFtZSI6IkFjbWUgSW52ZXN0IiwiYWRkcmVzcyI6eyJsaW5lMSI6IjEzNDQgQXJteSBSb2FkIiwibGluZTIiOiJTdWl0ZSAxMTEiLCJsaW5lNCI6IlRlbGZvcmQiLCJjb3VudHJ5IjoiWlcifSwicGhvbmVOdW1iZXIiOiIwMTIxODIyOTgzIn0sInllYXJzUmV0dXJucyI6eyJ0YXhSZXR1cm5zTm9EdWVzIjp0cnVlLCJyZXR1cm5zIjpbeyJ0YXhSZXR1cm5ZZWFyIjoiMTYiLCJ0YXhDb25zZXF1ZW5jZSI6ZmFsc2V9LHsidGF4UmV0dXJuWWVhciI6IjE1IiwidGF4Q29uc2VxdWVuY2UiOmZhbHNlfSx7InRheFJldHVyblllYXIiOiIxNCIsInRheENvbnNlcXVlbmNlIjpmYWxzZX0seyJ0YXhSZXR1cm5ZZWFyIjoiMTMiLCJ0YXhDb25zZXF1ZW5jZSI6ZmFsc2V9XX0sImRlY2xhcmF0aW9uIjp7Im5hbWUiOnsiZmlyc3ROYW1lIjoiSm9obiIsIm1pZGRsZU5hbWUiOiJXaWxsaWFtIiwibGFzdE5hbWUiOiJPJ0Nvbm5vciJ9LCJhZGRyZXNzIjp7ImxpbmUxIjoiMTM0NCBBcm15IFJvYWQiLCJsaW5lMiI6IlN1aXRlIDExMSIsImxpbmU0IjoiVGVsZm9yZCIsInBvc3RDb2RlIjoiVEYxIDVEUiIsImNvdW50cnkiOiJHQiJ9fSwiZGV0YWlscyI6eyJ0cnVzdCI6eyJkZXRhaWxzIjp7InN0YXJ0RGF0ZSI6IjE1MDAtMDEtMDEiLCJhZG1pbmlzdHJhdGlvbkNvdW50cnkiOiJHQiIsInJlc2lkZW50aWFsU3RhdHVzIjp7InVrIjp7InNjb3R0aXNoTGF3IjpmYWxzZSwicHJlT2ZmU2hvcmUiOiJBRCJ9fSwidHlwZU9mVHJ1c3QiOiJXaWxsIFRydXN0IG9yIEludGVzdGFjeSBUcnVzdCIsImRlZWRPZlZhcmlhdGlvbiI6IlByZXZpb3VzbHkgdGhlcmUgd2FzIG9ubHkgYW4gYWJzb2x1dGUgaW50ZXJlc3QgdW5kZXIgdGhlIHdpbGwiLCJpbnRlclZpdm9zIjpmYWxzZSwiZWZyYnNTdGFydERhdGUiOiIyMDAxLTAxLTAxIiwidHJ1c3RVS1Jlc2lkZW50IjpmYWxzZSwidHJ1c3RUYXhhYmxlIjp0cnVlLCJleHByZXNzVHJ1c3QiOnRydWUsInRydXN0UmVjb3JkZWQiOmZhbHNlLCJ0cnVzdFVLUmVsYXRpb24iOmZhbHNlfSwiZW50aXRpZXMiOnsibmF0dXJhbFBlcnNvbiI6W3sibmFtZSI6eyJmaXJzdE5hbWUiOiJKb2huIiwibWlkZGxlTmFtZSI6IldpbGxpYW0iLCJsYXN0TmFtZSI6Ik8nQ29ubm9yIn0sImRhdGVPZkJpcnRoIjoiMTk1Ni0wMi0xMiJ9XSwiYmVuZWZpY2lhcnkiOnsiaW5kaXZpZHVhbERldGFpbHMiOlt7Im5hbWUiOnsiZmlyc3ROYW1lIjoiSm9obiIsIm1pZGRsZU5hbWUiOiJXaWxsaWFtIiwibGFzdE5hbWUiOiJPJ0Nvbm5vciJ9LCJkYXRlT2ZCaXJ0aCI6IjIwMDEtMDEtMDEiLCJ2dWxuZXJhYmxlQmVuZWZpY2lhcnkiOnRydWUsImJlbmVmaWNpYXJ5VHlwZSI6IkRpcmVjdG9yIiwiYmVuZWZpY2lhcnlEaXNjcmV0aW9uIjp0cnVlLCJiZW5lZmljaWFyeVNoYXJlT2ZJbmNvbWUiOiIxMDAiLCJpZGVudGlmaWNhdGlvbiI6eyJwYXNzcG9ydCI6eyJudW1iZXIiOiJhYmNkZWZnaCIsImV4cGlyYXRpb25EYXRlIjoiMjAwMS0wMS0wMSIsImNvdW50cnlPZklzc3VlIjoiR0IifSwiYWRkcmVzcyI6eyJsaW5lMSI6IjEzNDQgQXJteSBSb2FkIiwibGluZTIiOiJTdWl0ZSAxMTEiLCJsaW5lNCI6IlRlbGZvcmQiLCJwb3N0Q29kZSI6IlRGMSA1RFIiLCJjb3VudHJ5IjoiR0IifX19XSwib3RoZXIiOlt7ImFkZHJlc3MiOnsibGluZTEiOiIxMzQ0IEFybXkgUm9hZCIsImxpbmUyIjoiU3VpdGUgMTExIiwibGluZTQiOiJUZWxmb3JkIiwicG9zdENvZGUiOiJURjEgNURSIiwiY291bnRyeSI6IkdCIn0sImRlc2NyaXB0aW9uIjoiTG9yZW0gaXBzdW0ifV19LCJkZWNlYXNlZCI6eyJuYW1lIjp7ImZpcnN0TmFtZSI6IkpvaG4iLCJtaWRkbGVOYW1lIjoiV2lsbGlhbSIsImxhc3ROYW1lIjoiTydDb25ub3IifSwiZGF0ZU9mQmlydGgiOiIxOTU2LTAyLTEyIiwiZGF0ZU9mRGVhdGgiOiIyMDE2LTAxLTAxIiwiaWRlbnRpZmljYXRpb24iOnsibmlubyI6IktDNDU2NzM2In19LCJsZWFkVHJ1c3RlZXMiOnsibmFtZSI6eyJmaXJzdE5hbWUiOiIxMjM0NTY3ODkwIFF3RXJUeVVpT3AgLC4oLykmJy0gbmFtZSIsImxhc3ROYW1lIjoiMTIzNDU2Nzg5MCBRd0VyVHlVaU9wICwuKC8pJictIG5hbWUifSwiZGF0ZU9mQmlydGgiOiIyMDE1LTAzLTIwIiwiaWRlbnRpZmljYXRpb24iOnsibmlubyI6IkFBMTAwMDAxQSJ9LCJwaG9uZU51bWJlciI6IisxMjM0NTY3ODkwMTIzNDU2NzgiLCJjb3VudHJ5T2ZSZXNpZGVuY2UiOiJERSIsIm5hdGlvbmFsaXR5IjoiREUiLCJsZWdhbGx5SW5jYXBhYmxlIjpmYWxzZX0sInRydXN0ZWVzIjpbeyJ0cnVzdGVlSW5kIjp7Im5hbWUiOnsiZmlyc3ROYW1lIjoiSm9obiIsIm1pZGRsZU5hbWUiOiJXaWxsaWFtIiwibGFzdE5hbWUiOiJPJ0Nvbm5vciJ9LCJkYXRlT2ZCaXJ0aCI6IjE5NTYtMDItMTIiLCJpZGVudGlmaWNhdGlvbiI6eyJuaW5vIjoiU1QxMjM0NTYifSwicGhvbmVOdW1iZXIiOiIwMTIxNTQ2NTQ2In19XSwic2V0dGxvcnMiOnsic2V0dGxvciI6W3sibmFtZSI6eyJmaXJzdE5hbWUiOiJhYmNkZWZnaGlqa2wiLCJtaWRkbGVOYW1lIjoiYWJjZGVmZ2hpamtsbW4iLCJsYXN0TmFtZSI6ImFiY2RlIn0sImRhdGVPZkJpcnRoIjoiMjAwMS0wMS0wMSIsImlkZW50aWZpY2F0aW9uIjp7Im5pbm8iOiJTVDAxOTA5MSJ9fV0sInNldHRsb3JDb21wYW55IjpbeyJuYW1lIjoiYWJjZGVmZ2hpamtsbW5vcHFyIiwiY29tcGFueVR5cGUiOiJUcmFkaW5nIiwiY29tcGFueVRpbWUiOmZhbHNlLCJpZGVudGlmaWNhdGlvbiI6eyJ1dHIiOiIxMjM0NTYxMjM0In19XX19LCJhc3NldHMiOnsibW9uZXRhcnkiOlt7ImFzc2V0TW9uZXRhcnlBbW91bnQiOjEwMDAwMH1dLCJwcm9wZXJ0eU9yTGFuZCI6W3siYnVpbGRpbmdMYW5kTmFtZSI6IkFDQiBIb3VzZSIsImFkZHJlc3MiOnsibGluZTEiOiIxMzQ0IEFybXkgUm9hZCIsImxpbmUyIjoiU3VpdGUgMTExIiwibGluZTQiOiJUZWxmb3JkIiwicG9zdENvZGUiOiJURjEgNURSIiwiY291bnRyeSI6IkdCIn0sInZhbHVlRnVsbCI6MTAwMDAwMCwidmFsdWVQcmV2aW91cyI6MjAwMDAwfV0sInNoYXJlcyI6W3sibnVtYmVyT2ZTaGFyZXMiOiIxMDAiLCJvcmdOYW1lIjoiTW9jayBPcmcgTFREIiwidXRyIjoiMTIzMTcxMjM0MiIsInNoYXJlQ2xhc3MiOiJPcmRpbmFyeSBzaGFyZXMiLCJ0eXBlT2ZTaGFyZSI6IlF1b3RlZCIsInZhbHVlIjo0NTg0NTQ1fV0sInBhcnRuZXJTaGlwIjpbeyJ1dHIiOiI1NDU2NDU2MjIxIiwiZGVzY3JpcHRpb24iOiJQYXJ0bmVyc2hpcCBUcmFkZSBcXCB7w4Aty7_igJl9XFwtICZgJ14iLCJwYXJ0bmVyc2hpcFN0YXJ0IjoiMjAwMS0wMS0wMSJ9XSwib3RoZXIiOlt7ImRlc2NyaXB0aW9uIjoiTG9yZW0gaXBzdW0gYW51bSIsInZhbHVlIjoxOTgyMzcyfV19fX19",
      MetaData(businessId = "trs",
        notableEvent = NotableEvent.TrsRegistration,
        payloadContentType = "application/json",
        payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
        userSubmissionTimestamp = LocalDateTime.now,
        identityData = identityData,
        userAuthToken = "AbCdEf123456",
        headerData = Json.obj(
          "Gov-Client-Public-IP" -> "198.51.100.0",
          "Gov-Client-Public-Port" -> "12345"
        ),
        searchKeys = SearchKeys(SearchKey.TRN, "ABTRUST123456789")
      ))

    "return NRS submission Id" when {

      "submitting a taxable registration event" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          ACCEPTED,
          Some("""{"nrSubmissionId": "2880d8aa-4691-49a4-aa6a-99191a51b9ef"}"""))

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")
        }
      }
    }

    "return NRS failure responses" when {

      "return ServiceUnavailableResponse when NRS returns service unavailable" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          SERVICE_UNAVAILABLE)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.ServiceUnavailable
        }
      }

      "return BadRequestResponse when NRS returns bad request" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          BAD_REQUEST)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.BadRequest
        }
      }

      "return BadGatewayResponse when NRS returns bad gateway" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          BAD_GATEWAY)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.BadGateway
        }
      }
      "return UnauthorisedResponse when NRS returns unauthorised response" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          UNAUTHORIZED)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.Unauthorised
        }
      }
      "return GatewayTimeoutResponse when NRS returns gateway timeout response" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          GATEWAY_TIMEOUT)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.GatewayTimeout
        }
      }
      "return InternalServerErrorResponse when NRS returns internal server error response" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          INTERNAL_SERVER_ERROR)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.InternalServerError
        }
      }

      "return ChecksumFailedResponse when NRS returns checksum failed error response" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubNRSPost(server,
          nonRepudiationEndpointUrl,
          Json.toJson(payLoad).toString,
          CHECKSUM_FAILED)

        val futureResult = connector.nonRepudiate(payLoad)

        whenReady(futureResult) {
          result =>
            result mustBe NRSResponse.ChecksumFailed
        }
      }

    }
  }
}
