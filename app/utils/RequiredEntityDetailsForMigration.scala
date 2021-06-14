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

  def areBeneficiariesCompleteForMigration(trust: JsValue): JsResult[Option[Boolean]] = {

    implicit class EntityArray(array: JsArray) {
      def isEmpty: Boolean = array.value.isEmpty || array.value.forall(_.canIgnoreBeneficiary)
    }

    def pickAtPathForType(`type`: String): JsResult[JsArray] = pickAtPathForEntityType(BENEFICIARIES, `type`, trust)

    for {
      individuals <- pickAtPathForType(INDIVIDUAL_BENEFICIARY)
      companies <- pickAtPathForType(COMPANY_BENEFICIARY)
      trusts <- pickAtPathForType(TRUST_BENEFICIARY)
      charities <- pickAtPathForType(CHARITY_BENEFICIARY)
      others <- pickAtPathForType(OTHER_BENEFICIARY)
      trustType = trustTypePick(trust)
    } yield {
      if (individuals.isEmpty && companies.isEmpty && trusts.isEmpty && charities.isEmpty && others.isEmpty) {
        None
      } else {

        val individualsHaveRequiredInfo = individuals.value.forall(individual => {
          lazy val hasVulnerableBeneficiaryField = individual.transform((__ \ VULNERABLE_BENEFICIARY).json.pick).isSuccess
          lazy val hasRoleInCompanyForEmploymentRelatedTrust =
            individual.hasRequiredInfoForEmploymentRelatedTrust(trustType, ROLE_IN_COMPANY)

          individual.hasRequiredBeneficiaryInfo(hasVulnerableBeneficiaryField && hasRoleInCompanyForEmploymentRelatedTrust)
        })

        val haveRequiredInfo = individualsHaveRequiredInfo &&
          companies.value.forall(_.hasRequiredBeneficiaryInfo()) &&
          trusts.value.forall(_.hasRequiredBeneficiaryInfo()) &&
          charities.value.forall(_.hasRequiredBeneficiaryInfo()) &&
          others.value.forall(_.hasRequiredBeneficiaryInfo())

        Some(haveRequiredInfo)
      }
    }
  }

  def areSettlorsCompleteForMigration(trust: JsValue): JsResult[Option[Boolean]] = {

    implicit class EntityArray(array: JsArray) {
      def isEmpty: Boolean = array.value.isEmpty || array.value.forall(_.canIgnoreSettlor)
    }

    for {
      businesses <- pickAtPathForEntityType(SETTLORS, BUSINESS_SETTLOR, trust)
      trustType = trustTypePick(trust)
    } yield {
      if (businesses.isEmpty) {
        None
      } else {
        val haveRequiredInfo = businesses.value.forall(business => {
          lazy val hasCompanyTypeAndTimeForEmploymentRelatedTrust =
            business.hasRequiredInfoForEmploymentRelatedTrust(trustType, COMPANY_TYPE, COMPANY_TIME)

          business.hasRequiredSettlorInfo(hasCompanyTypeAndTimeForEmploymentRelatedTrust)
        })

        Some(haveRequiredInfo)
      }
    }
  }

  private def pickAtPathForEntityType(entity: String, `type`: String, trust: JsValue): JsResult[JsArray] =
    JsonOps.pickAtPath[JsArray](ENTITIES \ entity \ `type`, trust) match {
      case x @ JsSuccess(_, _) => x
      case _ => JsSuccess(JsArray())
    }

  private def trustTypePick(trust: JsValue): JsResult[JsString] = trust.transform((TRUST \ DETAILS \ TYPE_OF_TRUST).json.pick[JsString])

  implicit class RequiredInfo(entity: JsValue) {

    def canIgnoreBeneficiary: Boolean = hasEndDate || hasUtr

    def hasRequiredBeneficiaryInfo(obeysAdditionalRules: Boolean = true): Boolean =
      canIgnoreBeneficiary || (hasDiscretionOrShareOfIncome && obeysAdditionalRules)

    def canIgnoreSettlor: Boolean = hasEndDate

    def hasRequiredSettlorInfo(obeysAdditionalRules: Boolean): Boolean =
      canIgnoreSettlor || obeysAdditionalRules

    private def hasEndDate: Boolean = entity.transform((__ \ ENTITY_END).json.pick).isSuccess

    private def hasUtr: Boolean = entity.transform((__ \ IDENTIFICATION \ UTR).json.pick).isSuccess

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
