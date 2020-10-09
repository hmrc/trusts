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
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{TrustProcessedResponse, _}
import uk.gov.hmrc.trusts.models.variation._
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.transformers.remove.RemoveTrustee
import uk.gov.hmrc.trusts.utils.JsonFixtures

import scala.concurrent.Future

class TrusteeTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val currentDate: LocalDate = LocalDate.of(1999, 3, 14)
  private object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = currentDate
  }

  val newLeadTrusteeIndInfo = LeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    entityStart = LocalDate.parse("2012-03-14"),
    None
  )

  val newLeadTrusteeOrgInfo = LeadTrusteeOrgType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = "Company Name",
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationOrgType(Some("UTR"), None, None),
    countryOfResidence = None,
    entityStart = LocalDate.parse("2012-03-14"),
    entityEnd = None
  )

  val newTrusteeIndInfo = TrusteeIndividualType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = Some(LocalDate.of(1965, 2, 10)),
    phoneNumber = Some("newPhone"),
    identification = Some(IdentificationType(Some("newNino"), None, None, None)),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    entityStart = LocalDate.parse("2012-03-14"),
    entityEnd = None
  )

  val newTrusteeOrgInfo = TrusteeOrgType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = "newCompanyName",
    phoneNumber = Some("newPhone"),
    email = None,
    identification = Some(IdentificationOrgType(
      Some("newUtr"),
      None,
      None)
    ),
    countryOfResidence = None,
    entityStart = LocalDate.parse("2012-03-14"),
    entityEnd = None
  )

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

  private val originalTrusteeOrgJson = Json.parse(
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

    "must add a new amend lead trustee transform using the transformation service" in {
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer(
        "utr",
        "internalId",
        LeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))

      }
    }

    "must add a new add trustee ind transform using the transformation service" in {
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAddTrusteeTransformer(
        "utr",
        "internalId",
        TrusteeType(Some(newTrusteeIndInfo), None))

      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddTrusteeIndTransform(newTrusteeIndInfo))

      }
    }

    "must add a new add trustee org transform using the transformation service" in {
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAddTrusteeTransformer(
        "utr",
        "internalId",
        TrusteeType(None, Some(newTrusteeOrgInfo)))

      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddTrusteeOrgTransform(newTrusteeOrgInfo))

      }
    }

    "must write a promote trustee ind transform using the transformation service" in {
      val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.getTransformedData(any(), any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val index = 1

      val endDate = LocalDate.of(2014, 3, 14)
      val result = service.addPromoteTrusteeTransformer(
        "utr",
        "internalId",
        index,
        LeadTrusteeType(Some(newLeadTrusteeIndInfo), None), endDate)

      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",
          PromoteTrusteeIndTransform(index, newLeadTrusteeIndInfo, endDate, originalTrusteeOrgJson, LocalDate.of(1999, 3, 14)))
      }
    }

    "must write a promote trustee org transform using the transformation service" in {
      val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.getTransformedData(any(), any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val index = 1

      val endDate = LocalDate.of(2013, 6, 28)

      val result = service.addPromoteTrusteeTransformer(
        "utr",
        "internalId",
        index,
        LeadTrusteeType(None, Some(newLeadTrusteeOrgInfo)),
        endDate)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",
          PromoteTrusteeOrgTransform(index, newLeadTrusteeOrgInfo, endDate, originalTrusteeOrgJson, LocalDate.of(1999, 3, 14)))
      }
    }

    "must write a RemoveTrustee transform using the transformation service" in {
      val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.getTransformedData(any(), any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = LocalDate.parse("2010-10-10")

      val payload = RemoveTrustee(
        endDate = endDate,
        index = 1
      )

      val result = service.addRemoveTrusteeTransformer("utr", "internalId", payload)

      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", RemoveTrusteeTransform(endDate, index = 1, originalTrusteeOrgJson))
      }
    }


    "must write a corresponding transform using the transformation service" in {

      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer(
        "utr",
        "internalId",
        LeadTrusteeType(Some(newLeadTrusteeIndInfo), None))

      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))

      }
    }

    "must add a new amend trustee transform using the transformation service" in {
      val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val index = 0
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService, LocalDateServiceStub)

      when(transformationService.getTransformedData(any(), any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendTrusteeTransformer(
        "utr",
        index,
        "internalId",
        TrusteeType(Some(newTrusteeIndInfo), None))

      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",
          AmendTrusteeIndTransform(index, newTrusteeIndInfo, originalTrusteeIndJson, currentDate))
      }
    }
  }
}
