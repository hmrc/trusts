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

    def pickAtPath(`type`: String): JsResult[JsArray] = JsonOps.pickAtPath[JsArray](ENTITIES \ BENEFICIARIES \ `type`, trust)

    for {
      individuals <- pickAtPath(INDIVIDUAL_BENEFICIARY)
      companies <- pickAtPath(COMPANY_BENEFICIARY)
      larges <- pickAtPath(LARGE_BENEFICIARY)
      trusts <- pickAtPath(TRUST_BENEFICIARY)
      charities <- pickAtPath(CHARITY_BENEFICIARY)
      others <- pickAtPath(OTHER_BENEFICIARY)
      trustType = trust.transform((TRUST \ DETAILS \ TYPE_OF_TRUST).json.pick[JsString])
    } yield {

      implicit class RequiredInfo(beneficiary: JsValue) {
        def hasRequiredInfo(obeysAdditionalRules: Boolean = true): Boolean = hasEndDate || (hasDiscretionOrShareOfIncome && obeysAdditionalRules)

        private def hasEndDate: Boolean = beneficiary.transform((__ \ ENTITY_END).json.pick).isSuccess

        private def hasDiscretionOrShareOfIncome: Boolean = {
          beneficiary.transform((__ \ HAS_DISCRETION).json.pick[JsBoolean]) match {
            case JsSuccess(JsBoolean(true), _) => true
            case JsSuccess(JsBoolean(false), _) => beneficiary.transform((__ \ SHARE_OF_INCOME).json.pick).isSuccess
            case _ => false
          }
        }
      }

      val individualsHaveRequiredInfo = individuals.value.foldLeft(true)((acc, individual) => {
        lazy val hasVulnerableBeneficiaryField = individual.transform((__ \ VULNERABLE_BENEFICIARY).json.pick).isSuccess
        lazy val hasRoleInCompanyForEmploymentRelatedTrust = trustType match {
          case JsSuccess(JsString(EMPLOYMENT_RELATED_TRUST), _) => individual.transform((__ \ ROLE_IN_COMPANY).json.pick).isSuccess
          case JsSuccess(JsString(_), _) => true
          case _ => false
        }
        acc && individual.hasRequiredInfo(hasVulnerableBeneficiaryField && hasRoleInCompanyForEmploymentRelatedTrust)
      })

      individualsHaveRequiredInfo &&
        companies.value.forall(_.hasRequiredInfo()) &&
        larges.value.forall(_.hasRequiredInfo()) &&
        trusts.value.forall(_.hasRequiredInfo()) &&
        charities.value.forall(_.hasRequiredInfo()) &&
        others.value.forall(_.hasRequiredInfo())
    }
  }
}
