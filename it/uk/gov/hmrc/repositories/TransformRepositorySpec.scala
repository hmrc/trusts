/*
 * Copyright 2022 HM Revenue & Customs
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

import models.NameType
import models.variation.{AmendedLeadTrusteeIndType, IdentificationType, TrusteeIndividualType}
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import repositories.TransformationRepositoryImpl
import transformers.ComposedDeltaTransform
import transformers.trustees._
import uk.gov.hmrc.itbase.IntegrationTestBase

import java.time.LocalDate

class TransformRepositorySpec extends IntegrationTestBase {

  private val data: ComposedDeltaTransform = ComposedDeltaTransform(
    Seq(
      AmendTrusteeTransform(
        None,
        Json.toJson(AmendedLeadTrusteeIndType(
          None,
          NameType("New", Some("lead"), "Trustee"),
          LocalDate.parse("2000-01-01"),
          "",
          None,
          IdentificationType(None, None, None, None),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None
        )),
        Json.obj(),
        LocalDate.now(),
        "leadTrusteeInd"
      ),
      AddTrusteeTransform(
        Json.toJson(TrusteeIndividualType(
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
        )),
        "trusteeInd",
      )
    )
  )

  "a transform repository" should {

    "be able to store and retrieve a payload" in assertMongoTest(createApplication)({ (app) =>
      val repository = app.injector.instanceOf[TransformationRepositoryImpl]

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", data)
      storedOk.futureValue mustBe true

      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")
      retrieved.futureValue mustBe Some(data)
    })
  }
}
