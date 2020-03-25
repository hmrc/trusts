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
import play.api.libs.json.{JsArray, JsObject, JsValue, Json, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.variation.{IndividualDetailsType, UnidentifiedType}
import uk.gov.hmrc.trusts.models.{NameType, RemoveBeneficiary, RemoveTrustee}
import uk.gov.hmrc.trusts.repositories.TransformationRepositoryImpl
import uk.gov.hmrc.trusts.transformers._
import uk.gov.hmrc.trusts.utils.{JsonRequests, JsonUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BeneficiaryTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {
  // Removing the usage of GuiceOneAppPerSuite started timing out a test without this.
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private implicit val hc : HeaderCarrier = HeaderCarrier()

  private def beneficiaryJson(value1 : String, endDate: Option[LocalDate] = None) = {
    if (endDate.isDefined) {
      Json.obj("field1" -> value1, "field2" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> value1, "field2" -> "value20", "lineNo" -> 65)
    }
  }

  def buildInputJson(beneficiaryType: String, beneficiaryData: Seq[JsValue]) = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "beneficiary" \ beneficiaryType).json
      .put(JsArray(beneficiaryData))

    baseJson.as[JsObject](__.json.update(adder))
  }


  "The beneficiary transformation service" - {

    "must add a new remove beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService)
      val beneficiary = beneficiaryJson("Blah Blah Blah")

      when(transformationService.addNewTransform(any(), any(), any()))
        .thenReturn(Future.successful(()))
      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(
          buildInputJson("individualDetails", Seq(beneficiary)),
          ResponseHeader("status", "formBundlNo")
        )))

      val result = service.removeBeneficiary("utr", "internalId", RemoveBeneficiary(LocalDate.of(2013, 2, 20), 0, "individualDetails"))
      whenReady(result) { _ =>
        verify(transformationService).addNewTransform("utr",
          "internalId", RemoveBeneficiariesTransform(0, beneficiary, LocalDate.of(2013, 2, 20), "individualDetails"))
      }
    }

    "must add a new amend unidentified beneficiary transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService)
      val newDescription = "Some Description"

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAmendUnidentifiedBeneficiaryTransformer("utr", index, "internalId", newDescription)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendUnidentifiedBeneficiaryTransform(index, newDescription))
      }
    }

    "must add a new add unidentified beneficiary transform using the transformation service" in {
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService)
      val newBeneficiary = UnidentifiedType(
        None,
        None,
        "Some description",
        None,
        None,
        DateTime.parse("2010-01-01"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAddUnidentifiedBeneficiaryTransformer("utr", "internalId", newBeneficiary)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddUnidentifiedBeneficiaryTransform(newBeneficiary))
      }
    }

    "must add a new amend individual beneficiary transform using the transformation service" in {
      val index = 0
      val transformationService = mock[TransformationService]
      val service = new BeneficiaryTransformationService(transformationService)
      val newIndividual = IndividualDetailsType(
        None,
        None,
        NameType("First", None, "Last"),
        None,
        vulnerableBeneficiary = false,
        None,
        None,
        None,
        None,
        DateTime.parse("2010-01-01"),
        None
      )

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(()))

      val result = service.addAmendIndividualBeneficiaryTransformer("utr", index, "internalId", newIndividual)
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AmendIndividualBeneficiaryTransform(index, newIndividual))
      }
    }
  }
}
