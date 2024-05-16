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

package utils

import java.time.LocalDate

import models._
import services.TrustsValidationError
import utils.TypeOfTrust.TypeOfTrust

import scala.annotation.tailrec

trait ValidationUtil {

  def isNotFutureDate(date: LocalDate, path: String, key: String): Option[TrustsValidationError] = {
    if (isAfterToday(date)) {
      Some(TrustsValidationError(s"$key must be today or in the past.", path))
    } else {
      None
    }
  }

  def isNotFutureDate(date: Option[LocalDate], path: String, key: String): Option[TrustsValidationError] = {
    date flatMap {
      d =>
        isNotFutureDate(d, path, key)
    }
  }

  def isAfterToday(date: LocalDate): Boolean = {
    date.isAfter(LocalDate.now)
  }

  type ListOfItemsAtIndex = List[(String, Int)]

  def findDuplicates(ninos: ListOfItemsAtIndex): ListOfItemsAtIndex = {

    @tailrec
    def findDuplicateHelper(remaining: ListOfItemsAtIndex, duplicates: ListOfItemsAtIndex): ListOfItemsAtIndex = {
      remaining match {
        case Nil =>
          duplicates
        case head :: tail =>
          if (tail.exists(x => x._1 == head._1)) {
            findDuplicateHelper(tail, head :: duplicates)
          } else {
            findDuplicateHelper(tail, duplicates)
          }
      }
    }

    findDuplicateHelper(ninos, Nil)
  }

  def getTrusteesUtrWithIndex(trustees: List[TrusteeType]): List[(String, Int)] = {
    val utrList: List[(String, Int)] = trustees.zipWithIndex.flatMap {
      case (trustee, index) =>
        trustee.trusteeOrg.flatMap { x =>
          x.identification.flatMap(_.utr.map { y => (y, index) })
        }
    }
    utrList
  }

  def getTrusteesNinoWithIndex(trustees: List[TrusteeType]): List[(String, Int)] = {
    val ninoList: List[(String, Int)] = trustees.zipWithIndex.flatMap {
      case (trustee, index) =>
        trustee.trusteeInd.flatMap { x =>
          x.identification.flatMap { z => z.nino.map { y => (y, index) } }
        }
    }
    ninoList
  }

  def getIndBenificiaryNinoWithIndex(indBenificiaries: List[IndividualDetailsType]): List[(String, Int)] = {
    val ninoList: List[(String, Int)] = indBenificiaries.zipWithIndex.flatMap {
      case (indv, index) =>
        indv.identification.map { x =>
          x.nino.map { y => (y, index) }
        }
    }.flatten
    ninoList
  }

  def getSettlorNinoWithIndex(settlors: List[Settlor]): List[(String, Int)] = {
    val ninoList: List[(String, Int)] = settlors.zipWithIndex.flatMap {
      case (settlor, index) =>
        settlor.identification.map { x =>
          x.nino.map { y => (y, index) }
        }
    }.flatten
    ninoList
  }

  def getIndBenificiaryPassportNumberWithIndex(indBenificiaries: List[IndividualDetailsType]): List[(String, Int)] = {
    val passportNumberList: List[(String, Int)] = indBenificiaries.zipWithIndex.flatMap {
      case (indv, index) =>
        indv.identification.map { x =>
          x.passport.map { y => (y.number, index) }
        }
    }.flatten
    passportNumberList
  }

  def isNotTrust(currentTypeOfTrust: TypeOfTrust, expectedType: TypeOfTrust): Boolean = currentTypeOfTrust != expectedType

  def getSettlorPassportNumberWithIndex(settlor: List[Settlor]):List[(String, Int)] = {
    val passportNumberList: List[(String, Int)] = settlor.zipWithIndex.flatMap {
      case (indv, index) =>
        indv.identification.map { x =>
          x.passport.map { y => (y.number, index) }
        }
    }.flatten
    passportNumberList
  }

  def getSettlorUtrNumberWithIndex(settlorCompanies: List[SettlorCompany]): List[(String, Int)] = {
    val utrList: List[(String, Int)] = settlorCompanies.zipWithIndex.flatMap {
      case (settlorCompany, index) =>
        settlorCompany.identification.map { x => x.utr map { y => (y, index) } }

    }.flatten
    utrList
  }

  def getDeceasedSettlor(registration: Registration): Option[WillType] = {
    registration.trust.entities.deceased.map(x => x)
  }

  def getSettlorIndividuals(registration: Registration): Option[List[Settlor]] = {
    registration.trust.entities.settlors.flatMap { settlors => settlors.settlor.map(x => x) }
  }

  def getSettlorCompanies(registration: Registration): Option[List[SettlorCompany]] = {
    registration.trust.entities.settlors.flatMap { settlors => settlors.settlorCompany.map(x => x) }
  }

}
