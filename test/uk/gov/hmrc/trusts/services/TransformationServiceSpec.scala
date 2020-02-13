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
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustLeadTrusteeType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.JsValue
import uk.gov.hmrc.trusts.transformers.{ComposedDeltaTransform, SetLeadTrusteeIndTransform}
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {

  val newTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = "newLineNo",
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = "2012-03-14"
  )

  val existingTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = "newLineNo",
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = "2002-03-14"
  )

  val unitTestTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = "newLineNo",
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = "2012-03-14"
  )


  "the transformation service" - {
    "must write a corresponding transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepository]
      val service = new TransformationService(repository)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrustee("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newTrusteeInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(SetLeadTrusteeIndTransform(newTrusteeInfo))))

      }
    }
    "must write a corresponding transform to the transformation repository with existing empty transforms" in {

      val repository = mock[TransformationRepository]
      val service = new TransformationService(repository)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrustee("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newTrusteeInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(SetLeadTrusteeIndTransform(newTrusteeInfo))))

      }
    }

    "must write a corresponding transform to the transformation repository with existing transforms" in {

      val repository = mock[TransformationRepository]
      val service = new TransformationService(repository)

      val existingTransforms = Seq(SetLeadTrusteeIndTransform(existingTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrustee("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newTrusteeInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            SetLeadTrusteeIndTransform(existingTrusteeInfo),
            SetLeadTrusteeIndTransform(newTrusteeInfo))))

      }
    }
    "must transform json data with the current transforms" in {
      val repository = mock[TransformationRepository]
      val service = new TransformationService(repository)

      val existingTransforms = Seq(SetLeadTrusteeIndTransform(unitTestTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")
      val afterJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-after-ind.json")

      val result: Future[JsValue] = service.applyTransformations("utr", "internalId", beforeJson)

      whenReady(result) {
        r => r mustEqual afterJson
      }
    }
    "must transform json data when no current transforms" in {
      val repository = mock[TransformationRepository]
      val service = new TransformationService(repository)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")

      val result: Future[JsValue] = service.applyTransformations("utr", "internalId", beforeJson)

      whenReady(result) {
        r => r mustEqual beforeJson
      }
    }
  }
}
