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

import models.variation._
import play.api.libs.json._
import utils.Constants._
import utils.TypeOfTrust.TypeOfTrust

class RequiredEntityDetailsForMigration {

  def areBeneficiariesCompleteForMigration(trust: JsValue): JsResult[Option[Boolean]] = {

    def pickAtPathForBeneficiaryType[T <: Beneficiary[T]](`type`: String)
                                                         (implicit rds: Reads[T]): JsResult[List[T]] =
      pickAtPathForEntityType[T](BENEFICIARIES, `type`, trust)

    for {
      individuals <- pickAtPathForBeneficiaryType[IndividualDetailsType](INDIVIDUAL_BENEFICIARY)
      companies <- pickAtPathForBeneficiaryType[BeneficiaryCompanyType](COMPANY_BENEFICIARY)
      trusts <- pickAtPathForBeneficiaryType[BeneficiaryTrustType](TRUST_BENEFICIARY)
      charities <- pickAtPathForBeneficiaryType[BeneficiaryCharityType](CHARITY_BENEFICIARY)
      others <- pickAtPathForBeneficiaryType[OtherType](OTHER_BENEFICIARY)
      trustType = trustTypePick(trust).asOpt
    } yield {
      if (individuals.isEmpty && companies.isEmpty && trusts.isEmpty && charities.isEmpty && others.isEmpty) {
        None
      } else {
        val haveRequiredDataForMigration =
          individuals.forall(_.hasRequiredDataForMigration(trustType)) &&
            companies.forall(_.hasRequiredDataForMigration(trustType)) &&
            trusts.forall(_.hasRequiredDataForMigration(trustType)) &&
            charities.forall(_.hasRequiredDataForMigration(trustType)) &&
            others.forall(_.hasRequiredDataForMigration(trustType))

        Some(haveRequiredDataForMigration)
      }
    }
  }

  def areSettlorsCompleteForMigration(trust: JsValue): JsResult[Option[Boolean]] = {

    for {
      businesses <- pickAtPathForEntityType[SettlorCompany](SETTLORS, BUSINESS_SETTLOR, trust)
      trustType = trustTypePick(trust).asOpt
    } yield {
      if (businesses.isEmpty) {
        None
      } else {
        val haveRequiredDataForMigration = businesses.forall { business =>
          business.hasRequiredDataForMigration(trustType)
        }

        Some(haveRequiredDataForMigration)
      }
    }
  }

  private def pickAtPathForEntityType[T <: MigrationEntity[T]](entity: String, `type`: String, trust: JsValue)
                                                              (implicit rds: Reads[T]): JsResult[List[T]] = {
    JsonOps.pickAtPath[JsArray](ENTITIES \ entity \ `type`, trust) match {
      case JsSuccess(x, _) => JsSuccess(x.value.map(_.as[T]).filterNot(_.canBeIgnored).toList)
      case _ => JsSuccess(Nil)
    }
  }

  private def trustTypePick(trust: JsValue): JsResult[TypeOfTrust] = trust
    .transform((TRUST \ DETAILS \ TYPE_OF_TRUST).json.pick[JsString])
    .map(_.as[TypeOfTrust])

}
