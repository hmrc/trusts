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

import play.api.Logging
import models.Registration
import services.TrustsValidationError
import utils.TypeOfTrust.TypeOfTrust


class SettlorDomainValidation(registration: Registration) extends ValidationUtil with Logging {

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
        } else {
          None
        }
    }
  }

  def deceasedSettlorIsNotTrustee: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        val deceasedNino = deceased.identification.flatMap(_.nino)
        registration.trust.entities.trustees.flatMap {
          trustees => {
            val trusteeNino = trustees.flatMap(_.trusteeInd.flatMap(_.identification.map(_.nino)))
            if (deceasedNino.isDefined && trusteeNino.contains(deceasedNino)) {
              Some(TrustsValidationError(s"Deceased NINO is same as trustee NINO.",
                s"/trust/entities/deceased/identification/nino"))
            } else {
              None
            }
          }
        }
    }
  }

  def deceasedSettlorIsNotBeneficiary: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        val deceasedNino = deceased.identification.flatMap(_.nino)
        registration.trust.entities.beneficiary.individualDetails.flatMap {
          indBeneficiary => {
            val beneficiaryNino = indBeneficiary.flatMap{x=>x.identification.map{y=>y.nino}}
            if (deceasedNino.isDefined && beneficiaryNino.contains(deceasedNino)) {
              Some(TrustsValidationError(s"Deceased NINO is same as beneficiary NINO.",
                s"/trust/entities/deceased/identification/nino"))
            } else {
              None
            }
          }
        }
    }
  }

  def deceasedSettlorIsNotProtector: Option[TrustsValidationError] = {
    getDeceasedSettlor(registration).flatMap {
      deceased =>
        val deceasedNino = deceased.identification.flatMap(_.nino)
        registration.trust.entities.protectors.flatMap {
          protectors => {
            val protectorNino = protectors.protector.map{x=>x.flatMap{y=>y.identification.map{z=>z.nino}}}.toList.flatten
            if (deceasedNino.isDefined && protectorNino.contains(deceasedNino)) {
              Some(TrustsValidationError(s"Deceased NINO is same as individual protector NINO.",
                s"/trust/entities/deceased/identification/nino"))
            } else {
              None
            }
          }
        }
    }
  }

  private def validateDeedOfVariation(trustType: TypeOfTrust): Option[TrustsValidationError] = {
    registration.trust.details.deedOfVariation match {
      case None =>
        Some(TrustsValidationError(
          message = s"Trust is $trustType so must have reason for deed of variation",
          location = "/trust/deedOfVariation"
        ))
      case Some(reasonForVariation) =>

        reasonForVariation match {
          case DeedOfVariation.AdditionToWill =>
            val noDeceasedSettlor = registration.trust.entities.deceased.isEmpty
            if (noDeceasedSettlor) {
              Some(TrustsValidationError(
                message = s"$trustType, $reasonForVariation, must have a deceased settlor",
                location = "/trust/entities/deceased"
              ))
            } else {
              None
            }
          case _ =>
            registration.trust.entities.settlors match {
              case Some(_) =>
                if (registration.trust.entities.deceased.isDefined) {
                  Some(TrustsValidationError(
                    message = s"$trustType, $reasonForVariation, must not have a deceased settlor",
                    location = "/trust/entities/deceased"
                  ))
                } else {
                  None
                }
              case None =>
                Some(TrustsValidationError(
                  message = s"$trustType, $reasonForVariation, must have a living settlor",
                  location = "/trust/entities/settlors"
                ))
            }
        }

    }
  }

  private def validateTrustHasLivingSettlor: Option[TrustsValidationError] = {
    val livingSettlors = registration.trust.entities.settlors
    livingSettlors match {
      case Some(_) => None
      case None =>
        Some(
          TrustsValidationError(
            s"Settlor is mandatory for provided type of trust.",
            s"/trust/entities/settlors"
          )
        )
    }
  }

  private def validateDeceasedSettlor: Option[TrustsValidationError] = {
    registration.trust.entities.deceased match {
      case Some(_) => None
      case None =>
        Some(
          TrustsValidationError(
            s"Deceased Settlor is required for ${TypeOfTrust.Will}",
            s"/trust/entities/deceased"
          )
        )
    }
  }

  def validateSettlor: Option[TrustsValidationError] = {
    val trustType = registration.trust.details.typeOfTrust

    trustType match {
      case None => None
      case Some(TypeOfTrust.DeedOfVariationOrFamilyAgreement) => validateDeedOfVariation(TypeOfTrust.DeedOfVariationOrFamilyAgreement)
      case Some(TypeOfTrust.Will) => validateDeceasedSettlor
      case _ => validateTrustHasLivingSettlor
    }
  }

  def livingSettlorDuplicateNino: List[Option[TrustsValidationError]] = {
    getSettlorIndividuals(registration).map{
      settlorIndividuals =>
        val ninoList: List[(String, Int)] = getSettlorNinoWithIndex(settlorIndividuals)
        val duplicatesNino = findDuplicates(ninoList).reverse
        logger.info(s"Number of Duplicate Nino found: ${duplicatesNino.size} ")
        duplicatesNino.map {
          case (_, index) =>
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
        logger.info(s"Number of Duplicate passport number found: ${duplicateUtrList.size} ")
        duplicateUtrList.map {
          case (_, index) =>
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
        logger.info(s"Number of Duplicate utr found: ${duplicatePassportNumberList.size} ")
        duplicatePassportNumberList.map {
          case (_, index) =>
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
          case (utr, index) if trustUtr.contains(utr) =>
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

    val sValidator = new SettlorDomainValidation(registration)

    val errorsList = List(
      sValidator.deceasedSettlorDobIsNotFutureDate,
      sValidator.deceasedSettlorDoDIsNotFutureDate,
      sValidator.deceasedSettlorDoDIsNotAfterDob,
      sValidator.deceasedSettlorIsNotTrustee,
      sValidator.validateSettlor,
      sValidator.deceasedSettlorIsNotBeneficiary,
      sValidator.deceasedSettlorIsNotProtector
    ).flatten

    errorsList ++
      sValidator.livingSettlorDuplicateNino.flatten ++
      sValidator.livingSettlorDobIsNotFutureDate.flatten ++
      sValidator.livingSettlorDuplicatePassportNumber.flatten ++
      sValidator.livingSettlorDuplicateUtr.flatten ++
      sValidator.companySettlorUtrIsNotTrustUtr.flatten
  }
}
