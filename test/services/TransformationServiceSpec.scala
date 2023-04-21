/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import errors.{ServerError, TrustErrors}
import models.get_trust.{ClosedRequestResponse, GetTrustResponse, GetTrustSuccessResponse, TrustProcessedResponse}
import models.variation.{AmendedLeadTrusteeIndType, IdentificationType}
import models.{AddressType, NameType}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import repositories.TransformationRepositoryImpl
import services.auditing.AuditService
import transformers._
import transformers.beneficiaries.{AddBeneficiaryTransform, AmendBeneficiaryTransform}
import transformers.settlors.{AddSettlorTransform, AmendSettlorTransform}
import transformers.trustdetails.SetTrustDetailTransform
import transformers.trustees._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
import utils.{JsonFixtures, JsonUtils, Session}

import java.time.LocalDate
import java.time.Month._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with JsonFixtures with EitherValues {

  private val (year1965, day10, spanLength1000, spanLength15) = (1965, 10, 1000, 15)

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(spanLength1000, Millis),
    interval = Span(spanLength15, Millis))

  private val utr: String = "utr"
  private val internalId = "internalId"

  private val unitTestLeadTrusteeInfo = AmendedLeadTrusteeIndType(
    bpMatchStatus = None,
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(year1965, FEBRUARY, day10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None
  )

  private val auditService = mock[AuditService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val sessionId: String = Session.id(hc)

  private val originalTrusteeIndJson = Json.parse(
    """
      |{
      |  "trusteeInd": {
      |    "lineNo": "1",
      |    "bpMatchStatus": "01",
      |    "name": {
      |      "firstName": "Tamara",
      |      "middleName": "Hingis",
      |      "lastName": "Jones"
      |    },
      |    "dateOfBirth": "1965-02-28",
      |    "identification": {
      |      "safeId": "2222200000000"
      |    },
      |    "phoneNumber": "+447456788112",
      |    "entityStart": "2017-02-28"
      |  }
      |}
      |""".stripMargin)

  private val existingLeadTrusteeInfo = AmendedLeadTrusteeIndType(
    bpMatchStatus = None,
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = LocalDate.of(year1965, FEBRUARY, day10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None
  )

  private val newLeadTrusteeIndInfo = AmendedLeadTrusteeIndType(
    bpMatchStatus = None,
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = LocalDate.of(year1965, FEBRUARY, day10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None, None),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None
  )

  "TransformationService" - {

    "when .applyDeclarationTransformations" - {

      "must transform json data with the current transforms" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val existingTransforms = Seq(
          RemoveTrusteeTransform(Some(0), originalTrusteeIndJson, LocalDate.parse("2019-12-21"), "trusteeInd"),
          AmendTrusteeTransform(None, Json.toJson(unitTestLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
        )
        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(existingTransforms))))))
        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
        val afterJson: JsValue = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-ind-and-remove.json")

        val result = service.applyDeclarationTransformations(utr, internalId, beforeJson).value

        whenReady(result) { r =>
          r.value.get mustEqual afterJson
        }
      }

      "must transform json data when no current transforms" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(None))))
        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")

        val result = service.applyDeclarationTransformations(utr, internalId, beforeJson).value

        whenReady(result) { r =>
          r.value.get mustEqual beforeJson
        }
      }

      "must return ServerError() when repository.get returns an exception from Mongo" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Left(ServerError("exception message")))))

        val beforeJson: JsValue = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")

        val result = service.applyDeclarationTransformations(utr, internalId, beforeJson).value

        whenReady(result) { r =>
          r mustBe Left(ServerError("exception message"))
        }
      }
    }

    "when .populateLeadTrusteeAddress" - {
      "must apply the correspondence address to the lead trustee's address if it doesn't have one" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address-after.json")

        val result: JsResult[JsValue] = service.populateLeadTrusteeAddress(beforeJson)

        result.get mustEqual afterJson
      }
    }

    "when .getTransformedTrustJson" - {

      "must return Left(ServerError(message)) when trust is not in a processed state" in {
        val trustsService = mock[TrustsService]
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, trustsService, auditService)

        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ClosedRequestResponse))))

        when(service.getTransformedData(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ClosedRequestResponse))))

        val result = service.getTransformedTrustJson(utr, internalId, sessionId).value
        whenReady(result) { r =>
          r mustBe Left(ServerError("Trust is not in a processed state."))
        }

      }

      "must return Left(ServerError()) when there's an error getting trust info, message is nonEmpty" in {
        val trustsService = mock[TrustsService]
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, trustsService, auditService)

        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("validation errors")))))

        val result = service.getTransformedTrustJson(utr, internalId, sessionId).value
        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }

      }

      "must return Left(ServerError()) when failed to get trust info" in {
        val trustsService = mock[TrustsService]
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, trustsService, auditService)

        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val result = service.getTransformedTrustJson(utr, internalId, sessionId).value
        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }

      }
    }

    "when .getTransformedData" - {
      "must fix lead trustee address of ETMP json read from DES service" in {
        val response = get5MLDTrustResponse.as[GetTrustSuccessResponse]
        val processedResponse = response.asInstanceOf[TrustProcessedResponse]
        val trustsService = mock[TrustsService]
        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed.json")
        val expectedResponse = TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

        val repository = mock[TransformationRepositoryImpl]
        when(repository.get(any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(None))))
        val service = new TransformationService(repository, trustsService, auditService)
        val result = service.getTransformedData(utr, internalId, sessionId).value
        whenReady(result) { r =>
          r mustBe Right(expectedResponse)
        }
      }

      "must apply transformations to ETMP json read from DES service" in {
        val response = get5MLDTrustResponse.as[GetTrustSuccessResponse]
        val processedResponse = response.asInstanceOf[TrustProcessedResponse]
        val trustsService = mock[TrustsService]
        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val newLeadTrusteeIndInfo = AmendedLeadTrusteeIndType(
          bpMatchStatus = None,
          name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
          dateOfBirth = LocalDate.of(year1965, FEBRUARY, day10),
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

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(existingTransforms))))))

        val transformedJson = JsonUtils.getJsonValueFromFile("valid-get-trust-response-transformed-with-amend.json")
        val expectedResponse = models.get_trust.TrustProcessedResponse(transformedJson, processedResponse.responseHeader)

        val service = new TransformationService(repository, trustsService, auditService)

        val result = service.getTransformedData(utr, internalId, sessionId).value
        whenReady(result) { r =>
          r mustBe Right(expectedResponse)
        }
      }

      "must return TrustErrors when applyTransformations returns trustErrors" in {
        val response = get5MLDTrustResponse.as[GetTrustSuccessResponse]
        val trustsService = mock[TrustsService]
        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val repository = mock[TransformationRepositoryImpl]

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Left(ServerError()))))

        val service = new TransformationService(repository, trustsService, auditService)

        val result = service.getTransformedData(utr, internalId, sessionId).value
        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }
      }
    }

    "when .addNewTransform" - {

      "must write a corresponding transform to the transformation repository with no existing transforms" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(Nil))))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val result = service.addNewTransform(
          utr,
          internalId,
          AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
        ).value
        whenReady(result) { _ =>

          verify(repository).set(
            utr,
            internalId,
            sessionId,
            ComposedDeltaTransform(Seq(AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")))
          )
        }
      }

      "must write a corresponding transform to the transformation repository with existing transforms" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val existingTransforms = Seq(AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd"))

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(existingTransforms))))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val result = service.addNewTransform(
          utr,
          internalId,
          AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
        ).value

        whenReady(result) { _ =>
          verify(repository).set(
            utr,
            internalId,
            sessionId,
            ComposedDeltaTransform(Seq(
              AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd"),
              AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
            ))
          )
        }
      }

      "must return ServerError() when repository.get returns an error" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Left(ServerError()))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val result = service.addNewTransform(
          utr,
          internalId,
          AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
        ).value

        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }
      }

      "must return ServerError() when adding a new transform fails" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val existingTransforms = Seq(AmendTrusteeTransform(None, Json.toJson(existingLeadTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd"))

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(existingTransforms))))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val result = service.addNewTransform(
          utr,
          internalId,
          AmendTrusteeTransform(None, Json.toJson(newLeadTrusteeIndInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")
        ).value

        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }
      }
    }

    "when .removeAllTransformations" - {
      "must call resetCache" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val result = service.removeAllTransformations(utr, internalId, sessionId).value

        whenReady(result) { _ =>
          verify(repository).resetCache(utr, internalId, sessionId)
        }
      }

      "must return a Left(ServerError) repository returns an exception from Mongo" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val result = service.removeAllTransformations(utr, internalId, sessionId).value

        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }
      }
    }

    "when .removeTrustTypeDependentTransformFields" - {
      "must remove individual beneficiary and business settlor transforms that contain trust-type-dependent fields" in {

        val trustTypeDependentBeneficiaryFields = Json.parse(
          """
            |{
            |  "beneficiaryType": "Director"
            |}
            |""".stripMargin)

        val trustTypeDependentSettlorFields = Json.parse(
          """
            |{
            |  "companyType": "Trading",
            |  "companyTime": true
            |}
            |""".stripMargin)

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val existingTransforms = Seq(
          AmendTrusteeTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_LEAD_TRUSTEE),
          AddSettlorTransform(trustTypeDependentSettlorFields, BUSINESS_SETTLOR),
          AddSettlorTransform(Json.obj(), BUSINESS_SETTLOR),
          AmendSettlorTransform(None, trustTypeDependentSettlorFields, Json.obj(), LocalDate.now(), BUSINESS_SETTLOR),
          AmendSettlorTransform(None, Json.obj(), Json.obj(), LocalDate.now(), BUSINESS_SETTLOR),
          AmendSettlorTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_SETTLOR),
          AddBeneficiaryTransform(trustTypeDependentBeneficiaryFields, INDIVIDUAL_BENEFICIARY),
          AddBeneficiaryTransform(Json.obj(), INDIVIDUAL_BENEFICIARY),
          AmendBeneficiaryTransform(None, trustTypeDependentBeneficiaryFields, Json.obj(), LocalDate.now(), INDIVIDUAL_BENEFICIARY),
          AmendBeneficiaryTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_BENEFICIARY),
          AmendBeneficiaryTransform(None, Json.obj(), Json.obj(), LocalDate.now(), COMPANY_BENEFICIARY)
        )

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(existingTransforms))))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val result = service.removeTrustTypeDependentTransformFields(utr, internalId, sessionId).value

        whenReady(result) { _ =>
          verify(repository).set(
            utr,
            internalId,
            sessionId,
            ComposedDeltaTransform(Seq(
              AmendTrusteeTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_LEAD_TRUSTEE),
              AddSettlorTransform(Json.obj(), BUSINESS_SETTLOR),
              AddSettlorTransform(Json.obj(), BUSINESS_SETTLOR),
              AmendSettlorTransform(None, Json.obj(), Json.obj(), LocalDate.now(), BUSINESS_SETTLOR),
              AmendSettlorTransform(None, Json.obj(), Json.obj(), LocalDate.now(), BUSINESS_SETTLOR),
              AmendSettlorTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_SETTLOR),
              AddBeneficiaryTransform(Json.obj(), INDIVIDUAL_BENEFICIARY),
              AddBeneficiaryTransform(Json.obj(), INDIVIDUAL_BENEFICIARY),
              AmendBeneficiaryTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_BENEFICIARY),
              AmendBeneficiaryTransform(None, Json.obj(), Json.obj(), LocalDate.now(), INDIVIDUAL_BENEFICIARY),
              AmendBeneficiaryTransform(None, Json.obj(), Json.obj(), LocalDate.now(), COMPANY_BENEFICIARY)
            ))
          )
        }
      }

      "must return ServerError() when failed to remove fields" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Left(ServerError()))))

        when(repository.set(any(), any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError("exception message")))))

        val result = service.removeTrustTypeDependentTransformFields(utr, internalId, sessionId).value

        whenReady(result) { r =>
          r mustBe Left(ServerError())
        }
      }
    }

    "when .removeOptionalTrustDetailTransforms" - {
      "must remove all SetTrustDetailTransform transforms that correspond to optional fields" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        val existingTransforms = Seq(
          SetTrustDetailTransform(JsString("FR"), "lawCountry"),
          SetTrustDetailTransform(JsString("GB"), "administrationCountry"),
          SetTrustDetailTransform(Json.parse("""{"nonUK":{"sch5atcgga92":true}}"""), "residentialStatus"),
          SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"),
          SetTrustDetailTransform(JsBoolean(true), "trustRecorded"),
          SetTrustDetailTransform(JsBoolean(false), "trustUKRelation"),
          SetTrustDetailTransform(JsBoolean(false), "trustUKResident"),
          SetTrustDetailTransform(JsString("Inter vivos Settlement"), "typeOfTrust"),
          SetTrustDetailTransform(JsBoolean(true), "interVivos"),
          SetTrustDetailTransform(Json.toJson(LocalDate.parse("2000-01-01")), "efrbsStartDate"),
          SetTrustDetailTransform(JsString("Replaced the will trust"), "deedOfVariation")
        )
        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Right(Some(ComposedDeltaTransform(existingTransforms))))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))

        val result = service.removeOptionalTrustDetailTransforms(utr, internalId, sessionId).value

        whenReady(result) { _ =>
          verify(repository).set(
            utr,
            internalId,
            sessionId,
            ComposedDeltaTransform(Seq(
              SetTrustDetailTransform(JsString("GB"), "administrationCountry"),
              SetTrustDetailTransform(Json.parse("""{"nonUK":{"sch5atcgga92":true}}"""), "residentialStatus"),
              SetTrustDetailTransform(JsBoolean(true), "trustUKProperty"),
              SetTrustDetailTransform(JsBoolean(true), "trustRecorded"),
              SetTrustDetailTransform(JsBoolean(false), "trustUKResident"),
              SetTrustDetailTransform(JsString("Inter vivos Settlement"), "typeOfTrust")
            ))
          )
        }
      }

      "must return ServerError() when remove all transforms fails" in {
        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository, mock[TrustsService], auditService)

        when(repository.get(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](Future.successful(Left(ServerError("exception message")))))

        when(repository.set(any(), any(), any(), any())).thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Left(ServerError()))))

        val result = service.removeOptionalTrustDetailTransforms(utr, internalId, sessionId).value

        whenReady(result) { r =>
          r mustBe Left(ServerError())

        }
      }
    }
  }
}
