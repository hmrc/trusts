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

package utils

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object JsonOps {

  type JsPathNodes = Seq[Either[Int, String]]
  type JsEntry = (JsPathNodes, JsValue)
  type JsTraverse = PartialFunction[JsEntry, JsValue]

  implicit class JsPathOps(path: JsPathNodes) {
    def isEndsWith(field: String): Boolean = path.lastOption.contains(Right(field))
    def isEndsWith(index: Int): Boolean = path.lastOption.contains(Left(index))
    def /(field: String): JsPathNodes = path :+ Right(field)
    def /(index: Int): JsPathNodes = path :+ Left(index)
  }

  implicit class JsValueOps(underlying: JsValue) {

    def traverse(f: JsTraverse): JsValue = {

      def traverseRec(path: JsPathNodes, value: JsValue): JsValue = {

        val lifted: JsValue => JsValue = value => f.lift(path -> value).getOrElse(value)
        value match {
          case JsNull => lifted(JsNull)
          case boolean: JsBoolean => lifted(boolean)
          case number: JsNumber => lifted(number)
          case string: JsString => lifted(string)
          case array: JsArray =>
            val updatedArray = array.value.zipWithIndex.map {
              case (arrayValue, index) => traverseRec(path / index, arrayValue)
            }
            JsArray(updatedArray)

          case obj: JsObject =>
            val updatedFields = obj.fieldSet.toSeq.map {
              case (field, fieldValue) => field -> traverseRec(path / field, fieldValue)
            }
            JsObject(updatedFields)
        }
      }

      traverseRec(Nil, underlying)
    }

    def applyRules: JsValue =
      underlying.traverse {
        case (path, JsString(phone)) if path.isEndsWith("phoneNumber") | path.isEndsWith("agentTelephoneNumber") =>
          JsString(phone.replaceAll("\\(0\\)", ""))
      }
  }

  implicit class RemoveRoleInCompanyFields(initialData: JsValue) {

    import models.RichJsValue

    def removeRoleInCompanyFields(): JsValue = initialData.removeMappedPieces().removeAnswerRows().removeDraftData()

    def removeMappedPieces(): JsValue = {
      val individualBeneficiariesPath = JsPath \ "registration" \ "trust/entities/beneficiary" \ "individualDetails"
      removeRoleInCompanyFromArrayAtPath(individualBeneficiariesPath)
    }

    def removeAnswerRows(): JsValue = {
      val beneficiariesPath = JsPath \ "answerSections" \ "beneficiaries"
      def answerRowsPath(index: Int) = beneficiariesPath \ index \ "rows"
      def answerRowPath(beneficiaryIndex: Int, rowIndex: Int) = answerRowsPath(beneficiaryIndex) \ rowIndex

      getArray(initialData, beneficiariesPath) match {
        case Success(beneficiaries) =>
          beneficiaries.zipWithIndex.foldLeft(initialData)((outerFold, beneficiary) => {
            getArray(outerFold, answerRowsPath(beneficiary._2)) match {
              case Success(answerRows) =>
                answerRows.zipWithIndex.foldLeft(outerFold)((innerFold, row) => {
                  getString(innerFold, answerRowPath(beneficiary._2, row._2) \ "label") match {
                    case Success(label) =>
                      if (label == "individualBeneficiary.roleInCompany.checkYourAnswersLabel") {
                        innerFold.remove(answerRowPath(beneficiary._2, row._2)) match {
                          case JsSuccess(answerRowRemoved, _) => answerRowRemoved
                          case _ => innerFold
                        }
                      } else {
                        innerFold
                      }
                    case _ => innerFold
                  }
                })
              case _ => outerFold
            }
          })
        case _ => initialData
      }
    }

    def removeDraftData(): JsValue = {
      val individualBeneficiariesPath = JsPath \ "beneficiaries" \ "data" \ "beneficiaries" \ "individualBeneficiaries"
      removeRoleInCompanyFromArrayAtPath(individualBeneficiariesPath)
    }

    private def removeRoleInCompanyFromArrayAtPath(path: JsPath): JsValue = {
      getArray(initialData, path) match {
        case Success(individualBeneficiaries) =>
          def roleInCompanyPath(index: Int) = path \ index \ "roleInCompany"
          individualBeneficiaries.zipWithIndex.foldLeft(initialData)((acc, x) => {
            acc.remove(roleInCompanyPath(x._2)) match {
              case JsSuccess(roleInCompanyRemoved, _) => roleInCompanyRemoved
              case _ => acc
            }
          })
        case _ => initialData
      }
    }

    private def getArray(jsValue: JsValue, path: JsPath): Try[List[JsValue]] = {
      jsValue.transform(path.json.pick) match {
        case JsSuccess(value, _) => value match {
          case JsArray(array) => Success(array.toList)
          case _ => Failure(new Throwable(s"JSON at path $path not of type JsArray."))
        }
        case _ => Failure(new Throwable(s"JSON at path $path not found."))
      }
    }

    private def getString(jsValue: JsValue, path: JsPath): Try[String] = {
      jsValue.transform(path.json.pick) match {
        case JsSuccess(value, _) => value match {
          case JsString(string) => Success(string)
          case _ => Failure(new Throwable(s"JSON at path $path not of type JsString."))
        }
        case _ => Failure(new Throwable(s"JSON at path $path not found."))
      }
    }
  }
}
