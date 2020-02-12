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
import org.scalatest.FreeSpec
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustLeadTrusteeType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.trusts.transformers.{ComposedDeltaTransform, SetLeadTrusteeIndTransform}

import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures {
  val repository = mock[TransformationRepository]

  val service = new TransformationService(repository)

  "the transformation service" - {
    "must write a corresponding transform to the transformation repository" in {


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

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrustee("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newTrusteeInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(SetLeadTrusteeIndTransform(newTrusteeInfo))))

      }
    }
  }
}
