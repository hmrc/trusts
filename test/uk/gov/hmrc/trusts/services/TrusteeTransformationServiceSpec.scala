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

  private implicit val hc : HeaderCarrier = HeaderCarrier()

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
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))

      }
    }

    "must add a new add trustee ind transform using the transformation service" in {
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAddTrusteeTransformer("utr", "internalId", DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddTrusteeIndTransform(newTrusteeIndInfo))

      }
    }

    "must add a new add trustee org transform using the transformation service" in {
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAddTrusteeTransformer("utr", "internalId", DisplayTrustTrusteeType(None, Some(newTrusteeOrgInfo)))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddTrusteeOrgTransform(newTrusteeOrgInfo))

      }
    }

    "must write a promote trustee ind transform using the transformation service" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.getTransformedData(any(), any())(any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val index = 1

      val endDate = LocalDate.of(2014, 3, 14)
      val result = service.addPromoteTrusteeTransformer("utr", "internalId", index, DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None), endDate)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", PromoteTrusteeIndTransform(index, newLeadTrusteeIndInfo, endDate, originalTrusteeOrgJson))
      }
    }

    "must write a promote trustee org transform using the transformation service" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.getTransformedData(any(), any())(any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val index = 1

      val endDate = LocalDate.of(2013, 6, 28)

      val result = service.addPromoteTrusteeTransformer(
        "utr",
        "internalId",
        index,
        DisplayTrustLeadTrusteeType(None, Some(newLeadTrusteeOrgInfo)),
        endDate)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", PromoteTrusteeOrgTransform(index, newLeadTrusteeOrgInfo, endDate, originalTrusteeOrgJson))
      }
    }

    "must write a RemoveTrustee transform using the transformation service" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.getTransformedData(any(), any())(any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

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
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))

      }
    }

    "must add a new amend trustee transform using the transformation service" in {
      val response = getTrustResponse.as[GetTrustSuccessResponse]
      val processedResponse = response.asInstanceOf[TrustProcessedResponse]

      val index = 0
      
      val transformationService = mock[TransformationService]
      val service = new TrusteeTransformationService(transformationService)

      when(transformationService.getTransformedData(any(), any())(any())).thenReturn(Future.successful(processedResponse))
      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAmendTrusteeTransformer("utr", index, "internalId", DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendTrusteeIndTransform(index, newTrusteeIndInfo, originalTrusteeIndJson))
      }
    }
  }
}
