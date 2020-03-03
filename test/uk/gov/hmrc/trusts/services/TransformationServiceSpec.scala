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

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsResult, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.{AddressType, NameType, RemoveTrustee}
import uk.gov.hmrc.trusts.repositories.TransformationRepositoryImpl
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, AmendLeadTrusteeIndTransform, ComposedDeltaTransform, RemoveTrusteeTransform}
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {
  // Removing the usage of GuiceOneAppPerSuite started timing out a test without this.
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  val newLeadTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = Some(DateTime.parse("2012-03-14"))
  )

  val newTrusteeIndInfo = DisplayTrustTrusteeIndividualType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = Some(new DateTime(1965, 2, 10, 12, 30)),
    phoneNumber = Some("newPhone"),
    identification = Some(DisplayTrustIdentificationType(None, Some("newNino"), None, None)),
    entityStart = DateTime.parse("2012-03-14")
  )

  val existingLeadTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = Some(DateTime.parse("2002-03-14"))
  )

  val unitTestTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = Some(DateTime.parse("2012-03-14"))
  )

  val existingTrusteeIndividualInfo = DisplayTrustTrusteeIndividualType(
    lineNo = Some("1"),
    bpMatchStatus = Some("01"),
    name = NameType("John", Some("William"), "O'Connor"),
    dateOfBirth = Some(DateTime.parse("1956-02-12")),
    phoneNumber = Some("0121546546"),
    identification = Some(DisplayTrustIdentificationType(None, Some("ST123456"), None, None)),
    entityStart = DateTime.parse("1998-02-12")
  )

  private val auditService = mock[AuditService]

  private implicit val hc : HeaderCarrier = HeaderCarrier()

  private val trustee1Json = Json.parse(
    """
      |           {
      |              "trusteeOrg": {
      |                "lineNo": "1",
      |                "name": "MyOrg Incorporated",
      |                "phoneNumber": "+447456788112",
      |                "email": "a",
      |                "identification": {
      |                  "safeId": "2222200000000"
      |                },
      |                "entityStart": "2017-02-28"
      |              }
      |            }
      |""".stripMargin)

  "the transformation service" - {

    "must write an amend lead trustee transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))

      }
    }

    "must write an add trustee ind transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAddTrusteeTransformer("utr", "internalId", DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AddTrusteeIndTransform(newTrusteeIndInfo))))

      }
    }

    "must write a RemoveTrustee transform to the transformation repository with no existing transforms" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]

      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(processedResponse))

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, desService, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = DateTime.parse("2010-10-10")

      val payload = RemoveTrustee(
        endDate = endDate,
        index = 1
      )

      val result = service.addRemoveTrusteeTransformer("utr", "internalId", payload)

      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(RemoveTrusteeTransform(endDate, index = 1, trustee1Json))))
      }
    }

    "must write a RemoveTrustee transform to the transformation repository with existing transforms" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]

      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(processedResponse))

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, desService, auditService)

      val existingTransforms = Seq(AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = DateTime.parse("2010-10-10")

      val payload = RemoveTrustee(
        endDate = endDate,
        index = 1
      )

      val result = service.addRemoveTrusteeTransformer("utr", "internalId", payload)

      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
            RemoveTrusteeTransform(endDate, index = 1, trustee1Json)
          )))
      }
    }

    "must write a corresponding transform to the transformation repository with existing empty transforms" in {

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
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

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
            AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))

      }
    }
    "must transform json data with the current transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[DesService], auditService)

      val existingTransforms = Seq(
        AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
        AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo),
        AmendLeadTrusteeIndTransform(unitTestTrusteeInfo)
      )
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")
      val afterJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-after-ind.json")

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

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")

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
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]
      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(response))

      val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed.json")
      val expectedResponse = TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

      val repository = mock[TransformationRepositoryImpl]
      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      val service = new TransformationService(repository, desService, auditService)
      val result = service.getTransformedData("utr", "internalId")
      whenReady(result) {
        r => r mustEqual expectedResponse
      }
    }
    "must apply transformations to ETMP json read from DES service" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]
      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(response))

      val newLeadTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = new DateTime(1965, 2, 10, 0, 0),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(
          None,
          Some("newNino"),
          None,
          Some(AddressType("newLine1", "newLine2", None, None, Some("NE1 2LA"), "GB"))),
        entityStart = None
      )

      val existingTransforms = Seq(
        AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo)
      )

      val repository = mock[TransformationRepositoryImpl]

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))

      val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed-with-amend.json")
      val expectedResponse = TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

      val service = new TransformationService(repository, desService, auditService)

      val result = service.getTransformedData("utr", "internalId")
      whenReady(result) {
        r => r mustEqual expectedResponse
      }
    }
  }
}
