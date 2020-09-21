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

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.test.Helpers.running
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, AmendLeadTrusteeIndTransform, ComposedDeltaTransform}

import scala.concurrent.ExecutionContext.Implicits.global

class TransformRepositorySpec extends FreeSpec with MustMatchers with TransformIntegrationTest {

  "a transform repository" - {

    "must be able to store and retrieve a payload" in {

      val application = applicationBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[TransformationRepository]

          val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
          storedOk.futureValue mustBe true

          val retrieved = repository.get("UTRUTRUTR", "InternalId")
            .map(_.getOrElse(fail("The record was not found in the database")))

          retrieved.futureValue mustBe data

          dropTheDatabase(connection)
        }.get
      }
    }
  }

  val data = ComposedDeltaTransform(
    Seq(
      AmendLeadTrusteeIndTransform(
        DisplayTrustLeadTrusteeIndType(
          Some(""),
          None,
          NameType("New", Some("lead"), "Trustee"),
          LocalDate.parse("2000-01-01"),
          "",
          None,
          DisplayTrustIdentificationType(None, None, None, None),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          Some(LocalDate.parse("2010-10-10"))
        )
      ),
      AddTrusteeIndTransform(
        DisplayTrustTrusteeIndividualType(
          Some("lineNo"),
          Some("bpMatchStatus"),
          NameType("New", None, "Trustee"),
          Some(LocalDate.parse("2000-01-01")),
          Some("phoneNumber"),
          Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          LocalDate.parse("2010-10-10")
        )
      )
    )
  )
}
