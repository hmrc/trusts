/*
 * Copyright 2021 HM Revenue & Customs
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

package utils

import play.api.libs.json._
import utils.Constants._

class RequiredEntityDetailsForMigration {

  def areBeneficiariesCompleteForMigration(trust: JsValue): JsResult[Boolean] = {

    def pickAtPath(`type`: String): JsResult[JsArray] = pickAtPathForEntityType(BENEFICIARIES, `type`, trust)

    for {
      individuals <- pickAtPath(INDIVIDUAL_BENEFICIARY)
      companies <- pickAtPath(COMPANY_BENEFICIARY)
      larges <- pickAtPath(LARGE_BENEFICIARY)
      trusts <- pickAtPath(TRUST_BENEFICIARY)
      charities <- pickAtPath(CHARITY_BENEFICIARY)
      others <- pickAtPath(OTHER_BENEFICIARY)
      trustType = trustTypePick(trust)
    } yield {

      val individualsHaveRequiredInfo = individuals.value.forall(individual => {
        lazy val hasVulnerableBeneficiaryField = individual.transform((__ \ VULNERABLE_BENEFICIARY).json.pick).isSuccess
        lazy val hasRoleInCompanyForEmploymentRelatedTrust =
          individual.hasRequiredInfoForEmploymentRelatedTrust(trustType, ROLE_IN_COMPANY)

        individual.hasRequiredBeneficiaryInfo(hasVulnerableBeneficiaryField && hasRoleInCompanyForEmploymentRelatedTrust)
      })

      individualsHaveRequiredInfo &&
        companies.value.forall(_.hasRequiredBeneficiaryInfo()) &&
        larges.value.forall(_.hasRequiredBeneficiaryInfo()) &&
        trusts.value.forall(_.hasRequiredBeneficiaryInfo()) &&
        charities.value.forall(_.hasRequiredBeneficiaryInfo()) &&
        others.value.forall(_.hasRequiredBeneficiaryInfo())
    }
  }

  def areSettlorsCompleteForMigration(trust: JsValue): JsResult[Boolean] = {

    for {
      businesses <- pickAtPathForEntityType(SETTLORS, BUSINESS_SETTLOR, trust)
      trustType = trustTypePick(trust)
    } yield {
      businesses.value.forall(business => {
        lazy val hasCompanyTypeAndTimeForEmploymentRelatedTrust =
          business.hasRequiredInfoForEmploymentRelatedTrust(trustType, COMPANY_TYPE, COMPANY_TIME)

        business.hasRequiredSettlorInfo(hasCompanyTypeAndTimeForEmploymentRelatedTrust)
      })
    }
  }

  private def pickAtPathForEntityType(entity: String, `type`: String, trust: JsValue): JsResult[JsArray] =
    JsonOps.pickAtPath[JsArray](ENTITIES \ entity \ `type`, trust)

  private def trustTypePick(trust: JsValue): JsResult[JsString] = trust.transform((TRUST \ DETAILS \ TYPE_OF_TRUST).json.pick[JsString])

  implicit class RequiredInfo(entity: JsValue) {
    def hasRequiredBeneficiaryInfo(obeysAdditionalRules: Boolean = true): Boolean =
      hasEndDate || (hasDiscretionOrShareOfIncome && obeysAdditionalRules)

    def hasRequiredSettlorInfo(obeysAdditionalRules: Boolean): Boolean =
      hasEndDate || obeysAdditionalRules

    def hasEndDate: Boolean = entity.transform((__ \ ENTITY_END).json.pick).isSuccess

    def hasRequiredInfoForEmploymentRelatedTrust(trustType: JsResult[JsString], fields: String*): Boolean = {
      trustType match {
        case JsSuccess(JsString(EMPLOYMENT_RELATED_TRUST), _) => fields.forall(field => entity.transform((__ \ field).json.pick).isSuccess)
        case JsSuccess(JsString(_), _) => true
        case _ => false
      }
    }

    private def hasDiscretionOrShareOfIncome: Boolean = {
      entity.transform((__ \ HAS_DISCRETION).json.pick[JsBoolean]) match {
        case JsSuccess(JsBoolean(true), _) => true
        case JsSuccess(JsBoolean(false), _) => entity.transform((__ \ SHARE_OF_INCOME).json.pick).isSuccess
        case _ => false
      }
    }
  }
}
