/*
 * Copyright 2024 HM Revenue & Customs
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

package models.mapping.registration

import base.BaseSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsValue, Json}
import utils.DataExamples

import java.time.LocalDate


class LeadTrusteeMapperSpec extends BaseSpec with DataExamples {

  "Lead Trustee" should {

    "map to a valid json payload for a DES lead trustee" when {
      "it is individual type" when {

        val domainLeadTrustee = leadTrusteeIndividual
        val json: JsValue = Json.toJson(domainLeadTrustee)
        "containing a name" in {
          (json \ "name" \ "firstName").get.as[String] mustBe domainLeadTrustee.leadTrusteeInd.get.name.firstName
        }

        "containing a date of birth" in {
          (json \ "dateOfBirth").get.as[LocalDate] mustBe domainLeadTrustee.leadTrusteeInd.get.dateOfBirth
        }

        "containing an identification" in {
          (json \ "identification" \ "nino").get.as[String] mustBe domainLeadTrustee.leadTrusteeInd.get.identification.nino.get
        }

        "containing a phoneNumber" in {
          (json \ "phoneNumber").get.as[String] mustBe domainLeadTrustee.leadTrusteeInd.get.phoneNumber
        }

        "containing an email" in {
          (json \ "email").get.asOpt[String] mustBe domainLeadTrustee.leadTrusteeInd.get.email
        }
      }

      "it is an organisation type" when {

        val domainLeadTrustee = leadTrusteeOrganisation
        val json: JsValue = Json.toJson(domainLeadTrustee)

        "containing a name" in {
          (json \ "name").get.as[String] mustBe domainLeadTrustee.leadTrusteeOrg.get.name
        }

        "containing a phone number" in {
          (json \ "phoneNumber").get.as[String] mustBe domainLeadTrustee.leadTrusteeOrg.get.phoneNumber
        }

        "containing an email" in {
          (json \ "email").get.asOpt[String] mustBe domainLeadTrustee.leadTrusteeOrg.get.email
        }

        "containing identification with UTR" in {
          val json: JsValue = Json.toJson(domainLeadTrustee)

          (json \ "identification" \ "utr").get.as[String] mustBe domainLeadTrustee.leadTrusteeOrg.get.identification.utr.get
        }
      }
    }
  }
}
