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

package uk.gov.hmrc.trusts.services

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsResult, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust.get_trust
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{TrustProcessedResponse, _}
import uk.gov.hmrc.trusts.models.{AddressType, NameType}
import uk.gov.hmrc.trusts.repositories.TransformationRepositoryImpl
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonFixtures, JsonUtils}

import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))


  val unitTestTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    entityStart = Some(LocalDate.parse("2012-03-14"))
  )

  private val auditService = mock[AuditService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val originalTrusteeIndJson = Json.parse(
    """
      |{
      |            "trusteeInd": {
      |              "lineNo": "1",
      |              "bpMatchStatus": "01",
      |              "name": {
      |                "firstName": "Tamara",
      |                "middleName": "Hingis",
      |                "lastName": "Jones"
      |              },
      |              "dateOfBirth": "1965-02-28",
      |              "identification": {
      |                "safeId": "2222200000000"
      |              },
      |              "phoneNumber": "+447456788112",
      |              "entityStart": "2017-02-28"
      |            }
      |          }
      |""".stripMargin)

  val existingLeadTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    entityStart = Some(LocalDate.parse("2002-03-14"))
  )

  val newLeadTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    entityStart = Some(LocalDate.parse("2012-03-14"))
  )

  "must transform json data with the current transforms" in {
    val repository = mock[TransformationRepositoryImpl]
    val service = new TransformationService(repository, mock[DesService], auditService)

    val existingTransforms = Seq(
      RemoveTrusteeTransform(LocalDate.parse("2019-12-21"), 0, originalTrusteeIndJson),
      AmendLeadTrusteeIndTransform(unitTestTrusteeInfo)
    )
    when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
    when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

    val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
    val afterJson: JsValue = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-ind-and-remove.json")

    val result: Future[JsResult[JsValue]] = service.applyDeclarationTransformations("utr", "internalId", beforeJson)

    whenReady(result) {
      r => r.get mustEqual afterJson
    }
  }
  "must transform json data when no current transforms" in {
    val repository = mock[TransformationRepositoryImpl]
    val service = new TransformationService(repository, mock[DesService], auditService)

    when(repository.get(any(), any())).thenReturn(Future.successful(None))
    when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

    val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")

    val result: Future[JsResult[JsValue]] = service.applyDeclarationTransformations("utr", "internalId", beforeJson)

    whenReady(result) {
      r => r.get mustEqual beforeJson
    }
  }
  "must apply the correspondence address to the lead trustee's address if it doesn't have one" in {
    val repository = mock[TransformationRepositoryImpl]
    val service = new TransformationService(repository, mock[DesService], auditService)

    val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address.json")
    val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address-after.json")

    val result: JsResult[JsValue] = service.populateLeadTrusteeAddress(beforeJson)

    result.get mustEqual afterJson
  }
  "must fix lead trustee address of ETMP json read from DES service" in {
    val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
    val processedResponse = response.asInstanceOf[TrustProcessedResponse]
    val desService = mock[DesService]
    when(desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(response))

    val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed.json")
    val expectedResponse = get_trust.TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

    val repository = mock[TransformationRepositoryImpl]
    when(repository.get(any(), any())).thenReturn(Future.successful(None))
    val service = new TransformationService(repository, desService, auditService)
    val result = service.getTransformedData("utr", "internalId")
    whenReady(result) {
      r => r mustEqual expectedResponse
    }
  }
  "must apply transformations to ETMP json read from DES service" in {
    val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
    val processedResponse = response.asInstanceOf[TrustProcessedResponse]
    val desService = mock[DesService]
    when(desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(response))

    val newLeadTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
      lineNo = None,
      bpMatchStatus = None,
      name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
      dateOfBirth = LocalDate.of(1965, 2, 10),
      phoneNumber = "newPhone",
      email = Some("newEmail"),
      identification = DisplayTrustIdentificationType(
        None,
        Some("newNino"),
        None,
        Some(AddressType("newLine1", "newLine2", None, None, Some("NE1 2LA"), "GB"))),
      countryOfResidence = None,
      legallyIncapable = None,
      nationality = None,
      entityStart = None
    )

    val existingTransforms = Seq(
      AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo)
    )

    val repository = mock[TransformationRepositoryImpl]

    when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))

    val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed-with-amend.json")
    val expectedResponse = get_trust.TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

    val service = new TransformationService(repository, desService, auditService)

    val result = service.getTransformedData("utr", "internalId")
    whenReady(result) {
      r => r mustEqual expectedResponse
    }
  }

  "addNewTransform" - {

    "must write a corresponding transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform("utr", "internalId", AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))
      }
    }

    "must write a corresponding transform to the transformation repository with existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[DesService], auditService)

      val existingTransforms = Seq(AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform("utr", "internalId", AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
            AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))
      }
    }
  }
}
