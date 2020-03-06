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

package uk.gov.hmrc.trusts.transformers

import java.time.LocalDate

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType}

class DeltaTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "DeltaTransform" - {

    "serialise and deserialise json correctly" in {

      val amendLeadTrusteeTransform = AmendLeadTrusteeIndTransform(
        DisplayTrustLeadTrusteeIndType(
          None,
          None,
          NameType("New", Some("lead"), "Trustee"),
          DateTime.parse("2000-01-01"),
          "",
          None,
          DisplayTrustIdentificationType(None, None, None, None),
          None
        ))

      val addTrusteeTransform = AddTrusteeIndTransform(
        DisplayTrustTrusteeIndividualType(
          Some("lineNo"),
          Some("bpMatchStatus"),
          NameType("New", None, "Trustee"),
          Some(DateTime.parse("2000-01-01")),
          Some("phoneNumber"),
          Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
          DateTime.parse("2000-01-01")
        ))

      val removeTrusteeTransform = RemoveTrusteeTransform(
        endDate = LocalDate.parse("2010-01-01"),
        index = 0,
        Json.obj()
      )

      val json = Json.parse(
        s"""{
          |        "deltaTransforms" : [
          |            {
          |                "AmendLeadTrusteeIndTransform": ${Json.toJson(amendLeadTrusteeTransform)}
          |            },
          |            {
          |                "AddTrusteeIndTransform": ${Json.toJson(addTrusteeTransform)}
          |            },
          |            {
          |               "RemoveTrusteeTransform": ${Json.toJson(removeTrusteeTransform)}
          |            }
          |        ]
          |    }
          |""".stripMargin)

      val data = ComposedDeltaTransform(Seq(
          amendLeadTrusteeTransform,
          addTrusteeTransform,
          removeTrusteeTransform
        )
      )

      Json.toJson(data) mustEqual json
      json.as[ComposedDeltaTransform] mustEqual data
    }
  }
}