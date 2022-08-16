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

import models.NameType
import models.variation.{AmendedLeadTrusteeIndType, IdentificationType, TrusteeIndividualType}
import org.mongodb.scala.Document
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.TransformationRepositoryImpl
import transformers.ComposedDeltaTransform
import transformers.trustees._
import uk.gov.hmrc.itbase.IntegrationTestBase

import java.time.LocalDate

class TransformRepositorySpec extends AsyncFreeSpec with IntegrationTestBase {

  private val repository = createApplication.injector.instanceOf[TransformationRepositoryImpl]

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

  private def dropDB(): Unit = {
    await(repository.collection.deleteMany(filter = Document()).toFuture())
    await(repository.ensureIndexes)
  }

  "a transform repository" - {

    "must be able to store and retrieve a payload" in {
      dropDB()

      val storedOk = repository.set("UTRUTRUTR", "InternalId", "sessionId", data)
      storedOk.futureValue mustBe true

      val retrieved = repository.get("UTRUTRUTR", "InternalId", "sessionId")
      retrieved.futureValue mustBe Some(data)
    }
  }
}
