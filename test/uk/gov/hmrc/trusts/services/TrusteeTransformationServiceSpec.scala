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
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.Future

class TrusteeTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {
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

  val newLeadTrusteeOrgInfo = DisplayTrustLeadTrusteeOrgType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = "Company Name",
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationOrgType(None, Some("UTR"), None),
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

  val newTrusteeOrgInfo = DisplayTrustTrusteeOrgType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = "newCompanyName",
    phoneNumber = Some("newPhone"),
    email = None,
    identification = Some(DisplayTrustIdentificationOrgType(
      None,
      Some("newUtr"),
      None)
    ),
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

  "the trustee transformation service" - {

    "must write an amend lead trustee transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TrusteeTransformationService(repository, mock[DesService], auditService)

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
      val service = new TrusteeTransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAddTrusteeTransformer("utr", "internalId", DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AddTrusteeIndTransform(newTrusteeIndInfo))))

      }
    }

    "must write an add trustee org transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TrusteeTransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAddTrusteeTransformer("utr", "internalId", DisplayTrustTrusteeType(None, Some(newTrusteeOrgInfo)))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AddTrusteeOrgTransform(newTrusteeOrgInfo))))

      }
    }

    "must write a promote trustee ind transform to the transformation repository with no existing transforms" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]

      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(processedResponse))

      val repository = mock[TransformationRepositoryImpl]
      val service = new TrusteeTransformationService(repository, desService, auditService)
      val index = 1

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = LocalDate.of(2014, 3, 14)
      val result = service.addPromoteTrusteeTransformer("utr", "internalId", index, DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None), endDate)
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(PromoteTrusteeIndTransform(index, newLeadTrusteeIndInfo, endDate, trustee1Json))))
      }
    }

    "must write a promote trustee org transform to the transformation repository with no existing transforms" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]

      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(processedResponse))

      val repository = mock[TransformationRepositoryImpl]
      val service = new TrusteeTransformationService(repository, desService, auditService)
      val index = 1

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))
      val endDate = LocalDate.of(2013, 6, 28)

      val result = service.addPromoteTrusteeTransformer(
        "utr",
        "internalId",
        index,
        DisplayTrustLeadTrusteeType(None, Some(newLeadTrusteeOrgInfo)),
        endDate)
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(PromoteTrusteeOrgTransform(index, newLeadTrusteeOrgInfo, endDate, trustee1Json))))
      }
    }

    "must write a RemoveTrustee transform to the transformation repository with no existing transforms" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]
      val desService = mock[DesService]

      when (desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(processedResponse))

      val repository = mock[TransformationRepositoryImpl]
      val service = new TrusteeTransformationService(repository, desService, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = LocalDate.parse("2010-10-10")

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
      val service = new TrusteeTransformationService(repository, desService, auditService)

      val existingTransforms = Seq(AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = LocalDate.parse("2010-10-10")

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
      val service = new TrusteeTransformationService(repository, mock[DesService], auditService)

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
      val service = new TrusteeTransformationService(repository, mock[DesService], auditService)

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


    "must write an amend trustee transform to the transformation repository with no existing transforms" in {
      val index = 0
      val repository = mock[TransformationRepositoryImpl]
      val service = new TrusteeTransformationService(repository, mock[DesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendTrusteeTransformer("utr", index, "internalId", DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AmendTrusteeIndTransform(index, newTrusteeIndInfo))))

      }
    }

    "must write an amend unidentified beneficiary transform to the transformation repository with no existing transforms" in {
      val index = 0
      val repository = mock[TransformationRepositoryImpl]
      val service = new BeneficiaryTransformationService(repository, mock[DesService], auditService)
      val newDescription = "Some Description"

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendUnidentifiedBeneficiaryTransformer("utr", index, "internalId", newDescription)
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AmendUnidentifiedBeneficiaryTransform(index, newDescription))))

      }
    }
  }
}
