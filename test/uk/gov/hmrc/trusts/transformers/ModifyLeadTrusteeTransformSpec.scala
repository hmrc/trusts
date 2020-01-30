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
import play.api.libs.json.{JsValue, Json, __}
import uk.gov.hmrc.trusts.models.IdentificationOrgType
import uk.gov.hmrc.trusts.models.variation.{LeadTrusteeOrgType, LeadTrusteeType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class ModifyLeadTrusteeTransform(leadTrustee: LeadTrusteeType) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsValue = {
    val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)

    input.transform(
      leadTrusteesPath.json.prune andThen
      leadTrusteesPath.json.put(Json.toJson(leadTrustee.leadTrusteeOrg))
    ).asOpt.get
  }
}

class ModifyLeadTrusteeTransformSpec extends FreeSpec with MustMatchers with OptionValues {
  "the modify lead transformer should" - {

    "successfully modify a lead trustee's details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-after-org.json")
      val orgAddress = None
      val newTrusteeOrgInfo = LeadTrusteeOrgType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = "newName",
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationOrgType(Some("newUtr"), orgAddress),
        entityStart = new DateTime(2012, 3, 14, 12, 30),
        entityEnd = None
      )
      val newTrusteeInfo = LeadTrusteeType(
        leadTrusteeInd = None,
        leadTrusteeOrg = Some(newTrusteeOrgInfo)
      )
      val transformer = new ModifyLeadTrusteeTransform(newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson)
      result mustBe afterJson
    }
  }
}
