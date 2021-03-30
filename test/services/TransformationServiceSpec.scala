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

import models.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import models.variation.{AmendedLeadTrusteeIndType, IdentificationType}
import models.{AddressType, NameType}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsResult, JsValue, Json}
import repositories.TransformationRepositoryImpl
import transformers._
import transformers.assets._
import transformers.trustdetails.SetTrustDetailTransform
import transformers.trustees._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
import utils.{JsonFixtures, JsonUtils}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val utr: String = "utr"
  private val internalId = "internalId"
  
  private val unitTestLeadTrusteeInfo = AmendedLeadTrusteeIndType(
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None
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

  private val existingLeadTrusteeInfo = AmendedLeadTrusteeIndType(
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None
  )

  private val newLeadTrusteeIndInfo = AmendedLeadTrusteeIndType(
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None
  )

  "must transform json data with the current transforms" in {
    val repository = mock[TransformationRepositoryImpl]
    val service = new TransformationService(repository, mock[TrustsService], auditService)

    val existingTransforms = Seq(
      RemoveTrusteeTransform(Some(0), originalTrusteeIndJson, LocalDate.parse("2019-12-21"), "trusteeInd"),
      AmendTrusteeTransform(None, Json.toJson(unitTestLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
    )
    when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
    when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

    val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
    val afterJson: JsValue = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-ind-and-remove.json")

    val result: Future[JsResult[JsValue]] = service.applyDeclarationTransformations(utr, internalId, beforeJson)

    whenReady(result) {
      _.get mustEqual afterJson
    }
  }

  "must transform json data when no current transforms" in {
    val repository = mock[TransformationRepositoryImpl]
    val service = new TransformationService(repository, mock[TrustsService], auditService)

    when(repository.get(any(), any())).thenReturn(Future.successful(None))
    when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

    val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")

    val result: Future[JsResult[JsValue]] = service.applyDeclarationTransformations(utr, internalId, beforeJson)

    whenReady(result) {
      _.get mustEqual beforeJson
    }
  }

  "must apply the correspondence address to the lead trustee's address if it doesn't have one" in {
    val repository = mock[TransformationRepositoryImpl]
    val service = new TransformationService(repository, mock[TrustsService], auditService)

    val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address.json")
    val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address-after.json")

    val result: JsResult[JsValue] = service.populateLeadTrusteeAddress(beforeJson)

    result.get mustEqual afterJson
  }

  "must fix lead trustee address of ETMP json read from DES service" in {
    val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
    val processedResponse = response.asInstanceOf[TrustProcessedResponse]
    val trustsService = mock[TrustsService]
    when(trustsService.getTrustInfo(any(), any())).thenReturn(Future.successful(response))

    val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed.json")
    val expectedResponse = TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

    val repository = mock[TransformationRepositoryImpl]
    when(repository.get(any(), any())).thenReturn(Future.successful(None))
    val service = new TransformationService(repository, trustsService, auditService)
    val result = service.getTransformedData(utr, internalId)
    whenReady(result) {
      _ mustEqual expectedResponse
    }
  }

  "must apply transformations to ETMP json read from DES service" in {
    val response = get4MLDTrustResponse.as[GetTrustSuccessResponse]
    val processedResponse = response.asInstanceOf[TrustProcessedResponse]
    val trustsService = mock[TrustsService]
    when(trustsService.getTrustInfo(any(), any())).thenReturn(Future.successful(response))

    val newLeadTrusteeIndInfo = AmendedLeadTrusteeIndType(
      name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
      dateOfBirth = LocalDate.of(1965, 2, 10),
      phoneNumber = "newPhone",
      email = Some("newEmail"),
      identification = IdentificationType(
        Some("newNino"),
        None,
        Some(AddressType("newLine1", "newLine2", None, None, Some("NE1 2LA"), "GB")),
        None),
      countryOfResidence = None,
      legallyIncapable = None,
      nationality = None
    )

    val existingTransforms = Seq(
      AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
    )

    val repository = mock[TransformationRepositoryImpl]

    when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))

    val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed-with-amend.json")
    val expectedResponse = models.get_trust.TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

    val service = new TransformationService(repository, trustsService, auditService)

    val result = service.getTransformedData(utr, internalId)
    whenReady(result) {
      _ mustEqual expectedResponse
    }
  }

  ".addNewTransform" - {

    "must write a corresponding transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[TrustsService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform(
        utr,
        internalId,
        AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")

      )
      whenReady(result) { _ =>

        verify(repository).set(
          utr,
          internalId,
          ComposedDeltaTransform(Seq(AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")))
        )
      }
    }

    "must write a corresponding transform to the transformation repository with existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, mock[TrustsService], auditService)

      val existingTransforms = Seq(AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd"))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform(
        utr,
        internalId,
        AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
      )

      whenReady(result) { _ =>
        verify(repository).set(
          utr,
          internalId,
          ComposedDeltaTransform(Seq(
            AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd"),
            AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
          ))
        )
      }
    }
  }

  // TODO - delete
  ".migratingFromNonTaxableToTaxable" - {

    "return true" - {
      "when there are taxable migration transforms present" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val transforms = ComposedDeltaTransform(Seq(
          AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd"),

          AddAssetTransform(Json.obj(), OTHER_ASSET),
          AmendAssetTransform(Some(0), Json.obj(), Json.obj(), LocalDate.parse("2021-01-01"), OTHER_ASSET),
          RemoveAssetTransform(Some(0), Json.obj(), LocalDate.parse("2021-01-01"), OTHER_ASSET),

          AddAssetTransform(Json.obj(), NON_EEA_BUSINESS_ASSET),
          AmendAssetTransform(Some(0), Json.obj(), Json.obj(), LocalDate.parse("2021-01-01"), NON_EEA_BUSINESS_ASSET),
          RemoveAssetTransform(Some(0), Json.obj(), LocalDate.parse("2021-01-01"), NON_EEA_BUSINESS_ASSET),

          SetTrustDetailTransform(Json.obj(), "expressTrust"),
          SetTrustDetailTransform(Json.obj(), "trustUKResident"),
          SetTrustDetailTransform(Json.obj(), "trustTaxable"),
          SetTrustDetailTransform(Json.obj(), "trustUKProperty"),
          SetTrustDetailTransform(Json.obj(), "trustRecorded"),
          SetTrustDetailTransform(Json.obj(), "trustUKRelation"),

          SetTrustDetailTransform(Json.obj(), "lawCountry"),
          SetTrustDetailTransform(Json.obj(), "administrationCountry"),
          SetTrustDetailTransform(Json.obj(), "typeOfTrust"),
          SetTrustDetailTransform(Json.obj(), "deedOfVariation"),
          SetTrustDetailTransform(Json.obj(), "interVivos"),
          SetTrustDetailTransform(Json.obj(), "efrbsStartDate"),
          SetTrustDetailTransform(Json.obj(), "residentialStatus")
        ))

        when(repository.get(any(), any())).thenReturn(Future.successful(Some(transforms)))

        val result = service.migratingFromNonTaxableToTaxable(utr, internalId)

        whenReady(result) { r =>
          r mustBe true
        }
      }
    }

    "return false" - {
      "when there are no transforms" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val transforms = ComposedDeltaTransform(Nil)

        when(repository.get(any(), any())).thenReturn(Future.successful(Some(transforms)))

        val result = service.migratingFromNonTaxableToTaxable(utr, internalId)

        whenReady(result) { r =>
          r mustBe false
        }
      }

      "where there are transforms but none of them are taxable migration transforms" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val transforms = ComposedDeltaTransform(Seq(
          AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
        ))

        when(repository.get(any(), any())).thenReturn(Future.successful(Some(transforms)))

        val result = service.migratingFromNonTaxableToTaxable(utr, internalId)

        whenReady(result) { r =>
          r mustBe false
        }
      }
    }

    "return error" - {
      "failed to get the transforms from the repository" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any())).thenReturn(Future.failed(new Throwable("repository get failed")))

        val result = service.migratingFromNonTaxableToTaxable(utr, internalId)

        recoverToSucceededIf[Exception](result)
      }
    }
  }
}
