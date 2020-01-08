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

package uk.gov.hmrc.trusts.utils

import uk.gov.hmrc.trusts.models.{EstateRegistration, Registration}
import uk.gov.hmrc.trusts.services.TrustsValidationError


class EstateDomainValidator(estateRegistration: EstateRegistration) extends ValidationUtil {

  def perRepDobIsNotFutureDate: Option[TrustsValidationError] = {
    val response = estateRegistration.estate.entities.personalRepresentative.estatePerRepInd.map {
      personalRepInd =>
        isNotFutureDate(personalRepInd.dateOfBirth,
          "/estate/entities/personalRepresentative/estatePerRepInd/dateOfBirth", "Date of birth")
    }
    response.getOrElse(None)
  }


  def personalRepOrgUtrIsNotSameEstateUtr: Option[TrustsValidationError] = {
    val estateUtr = estateRegistration.matchData.map(x => x.utr)
    estateRegistration.estate.entities.personalRepresentative.estatePerRepOrg.map {
      estatePerRepOrg =>
        if (estateUtr.isDefined && (estateUtr == estatePerRepOrg.identification.utr)) {
          Some(TrustsValidationError(s"Personal representative organisation utr is same as estate utr.",
            s"/estate/entities/personalRepresentative/estatePerRepOrg/identification/utr"))
        } else {
          None
        }

    }.flatten
  }
}

object EstateBusinessValidation {

  def check(estateRegistration: EstateRegistration): List[TrustsValidationError] = {
    val estateValidator = new EstateDomainValidator(estateRegistration)

    val errorsList = List(
      estateValidator.perRepDobIsNotFutureDate,
      estateValidator.personalRepOrgUtrIsNotSameEstateUtr
    ).flatten

    errorsList
  }
}
