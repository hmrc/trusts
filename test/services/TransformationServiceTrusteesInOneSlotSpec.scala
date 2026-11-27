/*
 * Copyright 2026 HM Revenue & Customs
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
import errors.TrustErrors
import models.get_trust.{GetTrustResponse, ResponseHeader, TrustProcessedResponse}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import repositories.TransformationRepositoryImpl
import services.auditing.AuditService
import transformers.trustees.{AmendTrusteeTransform, RemoveTrusteeTransform}
import transformers.{ComposedDeltaTransform, DeltaTransform}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
import utils.{JsonUtils, Session}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationServiceTrusteesInOneSlotSpec
    extends AnyFreeSpec with MockitoSugar with ScalaFutures with EitherValues {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private def json(file: String): JsValue = JsonUtils.getJsonValueFromFile(s"trustees-in-one-slot/$file.json")

  private val inOneSlot  = json("etmp-trust")
  private val onePerSlot = json("etmp-trust-one-per-slot")

  private def original(index: Int): JsValue = (onePerSlot \ "details" \ "trust" \ "entities" \ "trustees" \ index).get

  private val endDate = LocalDate.parse("2020-11-17")

  private val amendOrgAtIndex1 =
    AmendTrusteeTransform(Some(1), json("amended-org"), original(1), endDate, BUSINESS_TRUSTEE)

  private val amendIndAtIndex0 =
    AmendTrusteeTransform(Some(0), json("amended-ind"), original(0), endDate, INDIVIDUAL_TRUSTEE)

  private val removeIndAtIndex0 = RemoveTrusteeTransform(Some(0), original(0), endDate, INDIVIDUAL_TRUSTEE)

  private def service(trust: JsValue, transforms: Seq[DeltaTransform]): TransformationService = {
    val trustsService = mock[TrustsService]
    when(trustsService.getTrustInfo(any(), any(), any())).thenReturn(
      EitherT[Future, TrustErrors, GetTrustResponse](
        Future.successful(Right(TrustProcessedResponse(trust, ResponseHeader("Processed", "1"))))
      )
    )

    val repository = mock[TransformationRepositoryImpl]
    when(repository.get(any(), any(), any())).thenReturn(
      EitherT[Future, TrustErrors, Option[ComposedDeltaTransform]](
        Future.successful(Right(Option.when(transforms.nonEmpty)(ComposedDeltaTransform(transforms))))
      )
    )

    new TransformationService(repository, trustsService, mock[AuditService])
  }

  private def transformed(trust: JsValue, transforms: DeltaTransform*): JsValue =
    service(trust, transforms).getTransformedData("utr", "internalId", Session.id(hc)).value.futureValue.value match {
      case TrustProcessedResponse(json, _) => json
      case other                           => fail(s"expected a processed trust, got $other")
    }

  private def declared(trust: JsValue, transforms: DeltaTransform*): JsValue =
    service(trust, transforms).applyDeclarationTransformations("utr", "internalId", trust).value.futureValue.value.get

  private def trustees(json: JsValue): JsValue = (json \ "details" \ "trust" \ "entities" \ "trustees").get

  "TransformationService for a trust with a trusteeInd and a trusteeOrg in one trustees slot" - {

    "when .getTransformedData" - {

      "must return the trustees one per slot when there are no transforms" in {
        transformed(inOneSlot) mustBe transformed(onePerSlot)
      }

      "must amend the organisation trustee at index 1" in {
        transformed(inOneSlot, amendOrgAtIndex1) mustBe transformed(onePerSlot, amendOrgAtIndex1)
      }

      "must amend the individual trustee at index 0 and keep the organisation" in {
        transformed(inOneSlot, amendIndAtIndex0) mustBe transformed(onePerSlot, amendIndAtIndex0)
      }

      "must remove the individual trustee at index 0 and keep the organisation" in {
        val result = transformed(inOneSlot, removeIndAtIndex0)

        result           mustBe transformed(onePerSlot, removeIndAtIndex0)
        trustees(result) mustBe JsArray(Seq(original(1)))
      }
    }

    "when .applyDeclarationTransformations" - {

      "must keep the organisation and end the removed individual trustee" in {
        val endedIndividual = Json.obj(
          INDIVIDUAL_TRUSTEE -> ((original(0) \ INDIVIDUAL_TRUSTEE)
            .as[JsObject] + (ENTITY_END -> JsString("2020-11-17")))
        )

        val result = declared(inOneSlot, removeIndAtIndex0)

        result           mustBe declared(onePerSlot, removeIndAtIndex0)
        trustees(result) mustBe JsArray(Seq(original(1), endedIndividual))
      }

      "must end the amended organisation trustee and keep the individual" in {
        declared(inOneSlot, amendOrgAtIndex1) mustBe declared(onePerSlot, amendOrgAtIndex1)
      }

      "must not change the trust when there are no transforms" in {
        declared(inOneSlot) mustBe inOneSlot
      }
    }
  }

}
