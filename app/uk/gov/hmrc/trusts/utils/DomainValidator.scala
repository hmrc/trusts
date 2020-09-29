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

import play.api.Logging
import uk.gov.hmrc.trusts.models.Registration
import uk.gov.hmrc.trusts.services.TrustsValidationError

class DomainValidator(registration : Registration) extends ValidationUtil with Logging {

  val EFRBS_VALIDAION_MESSAGE = "Trusts efrbs start date can be provided for Employment Related trust only."

  def trustStartDateIsNotFutureDate: Option[TrustsValidationError] = {
    isNotFutureDate(registration.trust.details.startDate,
      "/trust/details/startDate", "Trusts start date")
  }

  def validateEfrbsDate: Option[TrustsValidationError] = {
    val isEfrbsDateDefined = registration.trust.details.efrbsStartDate.isDefined

    val isEmploymentRelatedTrust = registration.trust.details.isEmploymentRelatedTrust

    if (isEfrbsDateDefined && !isEmploymentRelatedTrust) {
      Some(TrustsValidationError(EFRBS_VALIDAION_MESSAGE, "/trust/details/efrbsStartDate"))
    } else {
      None
    }
  }

  def trustEfrbsDateIsNotFutureDate: Option[TrustsValidationError] = {
    isNotFutureDate(registration.trust.details.efrbsStartDate,
      "/trust/details/efrbsStartDate", "Trusts efrbs start date")
  }

  def indTrusteesDobIsNotFutureDate: List[Option[TrustsValidationError]] = {
    registration.trust.entities.trustees.map {
      trustees =>
        val errors = trustees.zipWithIndex.map {
          case (trustee, index) =>
            val response = trustee.trusteeInd.flatMap(x => {
              isNotFutureDate(x.dateOfBirth, s"/trust/entities/trustees/$index/trusteeInd/dateOfBirth", "Date of birth")
            })
            response
        }
        errors
    }.toList.flatten
  }

  def indTrusteesDuplicateNino: List[Option[TrustsValidationError]] = {
    registration.trust.entities.trustees.map {
      trustees => {
        val ninoList: List[(String, Int)] = getTrusteesNinoWithIndex(trustees)
        val duplicatesNino = findDuplicates(ninoList).reverse
        logger.info(s"[indTrusteesDuplicateNino] Number of Duplicate Nino found : ${duplicatesNino.size} ")
        duplicatesNino.map {
          case (_, index) =>
            Some(TrustsValidationError(s"NINO is already used for another individual trustee.",
              s"/trust/entities/trustees/$index/trusteeInd/identification/nino"))
        }
      }
    }.toList.flatten

  }

  def indBeneficiariesDuplicateNino: List[Option[TrustsValidationError]] = {
    registration.trust.entities.beneficiary.individualDetails.map {
      indBeneficiary => {
        val ninoList: List[(String, Int)] = getIndBenificiaryNinoWithIndex(indBeneficiary)
        val duplicatesNino = findDuplicates(ninoList).reverse
        logger.info(s"[indBeneficiariesDuplicateNino] Number of Duplicate Nino found : ${duplicatesNino.size} ")
        duplicatesNino.map {
          case (_, index) =>
            Some(TrustsValidationError(s"NINO is already used for another individual beneficiary.",
              s"/trust/entities/beneficiary/individualDetails/$index/identification/nino"))
        }
      }
    }.toList.flatten

  }

  def indBeneficiariesDobIsNotFutureDate: List[Option[TrustsValidationError]] = {
    registration.trust.entities.beneficiary.individualDetails.map {
      indBeneficiary =>
        val errors = indBeneficiary.zipWithIndex.map {
          case (individualBen, index) =>
            val response =
              isNotFutureDate(individualBen.dateOfBirth, s"/trust/entities/beneficiary/individualDetails/$index/dateOfBirth", "Date of birth")
            response
        }
        errors
    }.toList.flatten
  }

  def indBeneficiariesDuplicatePassportNumber: List[Option[TrustsValidationError]] = {
    registration.trust.entities.beneficiary.individualDetails.map {
      indBeneficiary => {
        val passportNumberList: List[(String, Int)] = getIndBenificiaryPassportNumberWithIndex(indBeneficiary)
        val duplicatePassportNumberList = findDuplicates(passportNumberList).reverse
        logger.info(s"[indBeneficiariesDuplicatePassportNumber] Number of Duplicate passport number found : ${duplicatePassportNumberList.size} ")
        duplicatePassportNumberList.map {
          case (passport, index) =>
            Some(TrustsValidationError(s"Passport number is already used for another individual beneficiary.",
              s"/trust/entities/beneficiary/individualDetails/$index/identification/passport/number"))
        }
      }
    }.toList.flatten

  }

  def indBeneficiariesBeneficiaryType: List[Option[TrustsValidationError]] = {

    registration.trust.details.typeOfTrust  match {

      case Some(TypeOfTrust.Employment) =>
        registration.trust.entities.beneficiary.individualDetails.map {
          indBeneficiary => {

            val beneficiaryTypeNotSet = indBeneficiary
              .zipWithIndex
              .filter(_._1.beneficiaryType.isEmpty)

            logger.info(s"[IndBeneficiariesBeneficiaryType] Number of Beneficiary Types not set found : ${beneficiaryTypeNotSet.size} ")

            beneficiaryTypeNotSet.map {
              case (_, index) =>
                Some(TrustsValidationError(s"Beneficiary Type must be set if Trust Type is Employment.",
                  s"/trust/entities/beneficiary/individualDetails/$index/beneficiaryType/individualDetails/beneficiaryType"))
            }

          }
        }.toList.flatten

      case _ => List.empty

    }

  }

  def businessTrusteesDuplicateUtr: List[Option[TrustsValidationError]] = {
    registration.trust.entities.trustees.map {
      trustees => {
        val utrList: List[(String, Int)] = getTrusteesUtrWithIndex(trustees)
        val duplicatesUtr = findDuplicates(utrList).reverse
        logger.info(s"[businessTrusteesDuplicateUtr] Number of Duplicate utr found : ${duplicatesUtr.size} ")
        duplicatesUtr.map {
          case (utr, index) =>
            Some(TrustsValidationError(s"Utr is already used for another business trustee.",
              s"/trust/entities/trustees/$index/trusteeOrg/identification/utr"))
        }
      }
    }.toList.flatten

  }

  def businessTrusteeUtrIsNotTrustUtr: List[Option[TrustsValidationError]] = {
    val trustUtr = registration.matchData.map(x => x.utr)
    registration.trust.entities.trustees.map {
      trustees => {
        val utrList: List[(String, Int)] = getTrusteesUtrWithIndex(trustees)
        utrList.map {
          case (utr, index) if trustUtr.contains(utr) =>
            Some(TrustsValidationError(s"Business trustee utr is same as trust utr.",
              s"/trust/entities/trustees/$index/trusteeOrg/identification/utr"))
          case _ =>
            None
        }
      }
    }.toList.flatten
  }

}


object BusinessValidation {

  def check(registration : Registration): List[TrustsValidationError]  = {

    val domainValidator = new DomainValidator(registration)

    val errorsList = List(
      domainValidator.trustStartDateIsNotFutureDate,
      domainValidator.validateEfrbsDate,
      domainValidator.trustEfrbsDateIsNotFutureDate
    ).flatten

    errorsList ++ domainValidator.indTrusteesDuplicateNino.flatten ++
      domainValidator.indTrusteesDobIsNotFutureDate.flatten ++
      domainValidator.businessTrusteesDuplicateUtr.flatten ++
      domainValidator.businessTrusteeUtrIsNotTrustUtr.flatten ++
      domainValidator.indBeneficiariesDobIsNotFutureDate.flatten ++
      domainValidator.indBeneficiariesDuplicateNino.flatten ++
      domainValidator.indBeneficiariesDuplicatePassportNumber.flatten ++
      domainValidator.indBeneficiariesBeneficiaryType.flatten ++
      SettlorDomainValidation.check(registration) ++
      AssetsDomainValidation.check(registration)

  }
}
