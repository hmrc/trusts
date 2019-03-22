/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Logger
import uk.gov.hmrc.trusts.models.Registration
import uk.gov.hmrc.trusts.services.TrustsValidationError


class SettlorDomainValidation(registration: Registration) extends ValidationUtil {





  def deceasedSettlorDobIsNotFutureDate: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        isNotFutureDate(deceased.dateOfBirth, s"/trust/entities/deceased/dateOfBirth", "Date of birth")
    }
  }

  def deceasedSettlorDoDIsNotFutureDate: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        isNotFutureDate(deceased.dateOfDeath, s"/trust/entities/deceased/dateOfDeath", "Date of death")
    }
  }

  def deceasedSettlorDoDIsNotAfterDob: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        val result = deceased.dateOfBirth.map(x => deceased.dateOfDeath.map(_.isBefore(x)))
        if (result.flatten.isDefined && result.flatten.get) {
          Some(TrustsValidationError(s"Date of death is after date of birth",
            s"/trust/entities/deceased/dateOfDeath"))
        } else None
    }
  }


  def deceasedSettlorIsNotTrustee: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        val deceasedNino = deceased.identification.map(_.nino).flatten
        registration.trust.entities.trustees.flatMap {
          trustees => {
            val trusteeNino = trustees.flatMap(_.trusteeInd.map(_.identification.nino))
            if (deceasedNino.isDefined && trusteeNino.contains(deceasedNino)) {
              Some(TrustsValidationError(s"Deceased NINO is same as trustee NINO.",
                s"/trust/entities/deceased/identification/nino"))
            } else None
          }
        }
    }
  }

  def validateSettlor: Option[TrustsValidationError] = {
    val currentTrust = registration.trust.details.typeOfTrust
    val settlorDefined = registration.trust.entities.settlors.isDefined

    if (isNotTrust(currentTrust, TypeOfTrust.WILL_TRUST) && !settlorDefined) {
      Some(TrustsValidationError(s"Settlor is mandatory for provided type of trust.",
        s"/trust/entities/settlors"))
    } else None
  }

  def livingSettlorDuplicateNino: List[Option[TrustsValidationError]] = {
    getSettlorIndividuals(registration).map{
          settlorIndividuals =>
            val ninoList: List[(String, Int)] = getSettlorNinoWithIndex(settlorIndividuals)
            val duplicatesNino = findDuplicates(ninoList).reverse
            Logger.info(s"[livingSettlorDuplicateNino] Number of Duplicate Nino found : ${duplicatesNino.size} ")
            duplicatesNino.map {
              case (nino, index) =>
                Some(TrustsValidationError(s"NINO is already used for another individual settlor.",
                  s"/trust/entities/settlors/settlor/$index/identification/nino"))
            }
    }.toList.flatten
  }


  def livingSettlorDobIsNotFutureDate: List[Option[TrustsValidationError]] = {
    getSettlorIndividuals(registration).map{
      settlorIndividuals =>
            val errors = settlorIndividuals.zipWithIndex.map {
              case (individualSettlor, index) =>
                isNotFutureDate(individualSettlor.dateOfBirth, s"/trust/entities/settlors/settlor/$index/dateOfBirth", "Date of birth")
            }
            errors
    }.toList.flatten
  }


  def livingSettlorDuplicatePassportNumber: List[Option[TrustsValidationError]] = {
    getSettlorIndividuals(registration).map{
          settlorIndividuals =>
            val utrList: List[(String, Int)] = getSettlorPassportNumberWithIndex(settlorIndividuals)
            val duplicateUtrList = findDuplicates(utrList).reverse
            Logger.info(s"[livingSettlorDuplicatePassportNumber] Number of Duplicate passport number found : ${duplicateUtrList.size} ")
            duplicateUtrList.map {
              case (utr, index) =>
                Some(TrustsValidationError(s"Passport number is already used for another individual settlor.",
                  s"/trust/entities/settlors/settlor/$index/identification/passport/number"))
            }
    }.toList.flatten
  }


  def livingSettlorDuplicateUtr: List[Option[TrustsValidationError]] = {
    getSettlorCompanies(registration).map{
      settlorCompanies =>
            val passportNumberList: List[(String, Int)] = getSettlorUtrNumberWithIndex(settlorCompanies)
            val duplicatePassportNumberList = findDuplicates(passportNumberList).reverse
            Logger.info(s"[livingSettlorDuplicateUtr] Number of Duplicate utr found : ${duplicatePassportNumberList.size} ")
            duplicatePassportNumberList.map {
              case (passport, index) =>
                Some(TrustsValidationError(s"Utr is already used for another settlor company.",
                  s"/trust/entities/settlors/settlorCompany/$index/identification/utr"))
            }
    }.toList.flatten
  }


  def companySettlorUtrIsNotTrustUtr: List[Option[TrustsValidationError]] = {
    val trustUtr = registration.matchData.map(x => x.utr)
    getSettlorCompanies(registration).map {
      settlorCompanies => {
        val utrList: List[(String, Int)] = getSettlorUtrNumberWithIndex(settlorCompanies)
        utrList.map {
          case (utr, index) if trustUtr == Some(utr) =>
            Some(TrustsValidationError(s"Settlor company utr is same as trust utr.",
              s"/trust/entities/settlors/settlorCompany/$index/identification/utr"))
          case _ =>
            None
        }
      }
    }.toList.flatten
  }
}


object SettlorDomainValidation {

  def check(registration: Registration): List[TrustsValidationError] = {

    val domainValidator = new SettlorDomainValidation(registration)

    val errorsList = List(
      domainValidator.deceasedSettlorDobIsNotFutureDate,
      domainValidator.deceasedSettlorDoDIsNotFutureDate,
      domainValidator.deceasedSettlorDoDIsNotAfterDob,
      domainValidator.deceasedSettlorIsNotTrustee,
      domainValidator.validateSettlor
    ).flatten

    errorsList ++
      domainValidator.livingSettlorDuplicateNino.flatten ++
      domainValidator.livingSettlorDobIsNotFutureDate.flatten ++
      domainValidator.livingSettlorDuplicatePassportNumber.flatten ++
      domainValidator.livingSettlorDuplicateUtr.flatten ++
      domainValidator.companySettlorUtrIsNotTrustUtr.flatten

  }
}
