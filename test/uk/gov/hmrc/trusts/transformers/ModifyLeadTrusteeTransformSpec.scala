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
import play.api.libs.json.{JsValue, Json, Writes, __}
import uk.gov.hmrc.trusts.models.{IdentificationOrgType, NameType}
import uk.gov.hmrc.trusts.models.variation.{IdentificationType, LeadTrusteeIndType, LeadTrusteeOrgType, LeadTrusteeType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class ModifyLeadTrusteeTransform(leadTrustee: LeadTrusteeType) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsValue = {
    leadTrustee match {
      case LeadTrusteeType(None, Some(lead)) => setLeadTrustee(input, lead)
      case LeadTrusteeType(Some(lead), None) => setLeadTrustee(input, lead)
    }
  }

  private def setLeadTrustee[A](input: JsValue, lead: A)(implicit writes: Writes[A]) = {
    val leadTrusteesPath = (__ \ 'details \ 'trust \ 'entities \ 'leadTrustees)

    input.transform(
      leadTrusteesPath.json.prune andThen
        leadTrusteesPath.json.put(Json.toJson(lead))
    ).asOpt.get
  }
}

class ModifyLeadTrusteeTransformSpec extends FreeSpec with MustMatchers with OptionValues {
  "the modify lead transformer should" - {

    "successfully set a new org lead trustee's details" in {
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
    "successfully set a new ind lead trustee's details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-after-ind.json")
      val newTrusteeIndInfo = LeadTrusteeIndType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
        identification = IdentificationType(Some("newNino"), None, None, None),
        entityStart = new DateTime(2012, 3, 14, 12, 30),
        entityEnd = None
      )
      val newTrusteeInfo = LeadTrusteeType(
        leadTrusteeInd = Some(newTrusteeIndInfo),
        leadTrusteeOrg = None
      )
      val transformer = new ModifyLeadTrusteeTransform(newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson)
      result mustBe afterJson
    }

  }
}
