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

package uk.gov.hmrc.repositories

import java.time.LocalDate

import org.scalatest.{AsyncFreeSpec, MustMatchers}
import uk.gov.hmrc.itbase.IntegrationTestBase
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.variation.{AmendedLeadTrusteeIndType, IdentificationType, TrusteeIndividualType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, AmendLeadTrusteeIndTransform, ComposedDeltaTransform}

class TransformRepositorySpec extends AsyncFreeSpec with MustMatchers with IntegrationTestBase {

  "a transform repository" - {

    "must be able to store and retrieve a payload" in assertMongoTest(createApplication) { application =>

      val repository = application.injector.instanceOf[TransformationRepository]

      val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
      storedOk.futureValue mustBe true

      val retrieved = repository.get("UTRUTRUTR", "InternalId")

      retrieved.futureValue mustBe Some(data)
    }
  }

  val data = ComposedDeltaTransform(
    Seq(
      AmendLeadTrusteeIndTransform(
        AmendedLeadTrusteeIndType(
          NameType("New", Some("lead"), "Trustee"),
          LocalDate.parse("2000-01-01"),
          "",
          None,
          IdentificationType(None, None, None, None),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None
        )
      ),
      AddTrusteeIndTransform(
        TrusteeIndividualType(
          Some("lineNo"),
          Some("bpMatchStatus"),
          NameType("New", None, "Trustee"),
          Some(LocalDate.parse("2000-01-01")),
          Some("phoneNumber"),
          Some(IdentificationType(Some("nino"), None, None, None)),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          LocalDate.parse("2010-10-10"),
          None
        )
      )
    )
  )
}
