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

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class DeltaTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "DeltaTransform" - {

    "serialise and deserialise json correctly" in {

      val json = Json.parse(
        """{
          |        "deltaTransforms" : [
          |            {
          |                "SetLeadTrusteeIndTransform" : {
          |                    "leadTrustee" : {
          |                        "lineNo" : "",
          |                        "name" : {
          |                            "firstName" : "New",
          |                            "middleName" : "lead",
          |                            "lastName" : "Trustee"
          |                        },
          |                        "dateOfBirth" : "2000-01-01",
          |                        "phoneNumber" : "",
          |                        "identification" : {},
          |                        "entityStart" : "now"
          |                    }
          |                }
          |            },
          |            {
          |                "AddTrusteeIndTransform" : {
          |                    "trustee" : {
          |                        "lineNo" : "lineNo",
          |                        "bpMatchStatus" : "bpMatchStatus",
          |                        "name" : {
          |                            "firstName" : "New",
          |                            "lastName" : "Trustee"
          |                        },
          |                        "dateOfBirth" : "2000-01-01",
          |                        "phoneNumber" : "phoneNumber",
          |                        "identification" : {
          |                            "nino" : "nino"
          |                        },
          |                        "entityStart" : "entityStart"
          |                    }
          |                }
          |            }
          |        ]
          |    }
          |""".stripMargin)

      val data = ComposedDeltaTransform(Seq(SetLeadTrusteeIndTransform(
        DisplayTrustLeadTrusteeIndType(
          "",
          None,
          NameType("New", Some("lead"), "Trustee"),
          DateTime.parse("2000-01-01"),
          "",
          None,
          DisplayTrustIdentificationType(None, None, None, None),
          "now"
        )),
        AddTrusteeIndTransform(DisplayTrustTrusteeIndividualType(
          "lineNo",
          Some("bpMatchStatus"),
          NameType("New", None, "Trustee"),
          Some(DateTime.parse("2000-01-01")),
          Some("phoneNumber"),
          Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
          "entityStart"
        ))
      )
      )
      Json.toJson(data) mustEqual json
      json.as[ComposedDeltaTransform] mustEqual data
    }


  }
}