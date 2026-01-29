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

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object JsonOps {

  def doNothing(): Reads[JsObject] =
    __.json.pick[JsObject]

  def putNewValue(path: JsPath, value: JsValue): Reads[JsObject] =
    __.json.update(path.json.put(value))

  // Play 2.5 throws if the path to be pruned does not exist.
  // So we do this hacky thing to keep it all self-contained.
  // If upgraded to play 2.6, this can turn into simply "path.json.prune".
  def prunePath(path: JsPath): Reads[JsObject] =
    __.json.update {
      path.json.put(Json.obj())
    } andThen path.json.prune

  def prunePathAndPutNewValue(path: JsPath, value: JsValue): Reads[JsObject] =
    prunePath(path) andThen putNewValue(path, value)

  def pickAtPath[A <: JsValue](path: JsPath, value: JsValue)(implicit rds: Reads[A]): JsResult[A] =
    value.transform(path.json.pick[A])

  type JsPathNodes = Seq[Either[Int, String]]
  type JsEntry     = (JsPathNodes, JsValue)
  type JsTraverse  = PartialFunction[JsEntry, JsValue]

  implicit class JsPathOps(path: JsPathNodes) {
    def isEndsWith(field: String): Boolean = path.lastOption.contains(Right(field))
    def isEndsWith(index: Int): Boolean    = path.lastOption.contains(Left(index))
    def /(field: String): JsPathNodes      = path :+ Right(field)
    def /(index: Int): JsPathNodes         = path :+ Left(index)
  }

  implicit class JsValueOps(underlying: JsValue) {

    def traverse(f: JsTraverse): JsValue = {

      def traverseRec(path: JsPathNodes, value: JsValue): JsValue = {

        val lifted: JsValue => JsValue = value => f.lift(path -> value).getOrElse(value)
        value match {
          case JsNull             => lifted(JsNull)
          case boolean: JsBoolean => lifted(boolean)
          case number: JsNumber   => lifted(number)
          case string: JsString   => lifted(string)
          case array: JsArray     =>
            val updatedArray = array.value.zipWithIndex.map { case (arrayValue, index) =>
              traverseRec(path / index, arrayValue)
            }
            JsArray(updatedArray)

          case obj: JsObject =>
            val updatedFields = obj.fieldSet.toSeq.map { case (field, fieldValue) =>
              field -> traverseRec(path / field, fieldValue)
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
      removeRoleInCompanyFromArrayAtPath(individualBeneficiariesPath, "beneficiaryType")
    }

    def removeAnswerRows(): JsValue = {

      val beneficiariesPath                                   = JsPath \ "answerSections" \ "beneficiaries"
      def answerRowsPath(index: Int)                          = beneficiariesPath \ index \ "rows"
      def answerRowPath(beneficiaryIndex: Int, rowIndex: Int) = answerRowsPath(beneficiaryIndex) \ rowIndex

      def removeAnswerRow(jsValue: JsValue, beneficiaryIndex: Int, rowIndex: Int, label: String): JsValue =
        if (label == "individualBeneficiaryRoleInCompany.checkYourAnswersLabel") {
          jsValue.remove(answerRowPath(beneficiaryIndex, rowIndex)) match {
            case JsSuccess(answerRowRemoved, _) => answerRowRemoved
            case _                              => jsValue
          }
        } else {
          jsValue
        }

      get[JsArray](initialData, beneficiariesPath) match {
        case Success(beneficiaries) =>
          beneficiaries.value.toList.zipWithIndex.foldLeft(initialData)((outerFold, beneficiary) =>
            get[JsArray](outerFold, answerRowsPath(beneficiary._2)) match {
              case Success(answerRows) =>
                answerRows.value.toList.zipWithIndex.foldLeft(outerFold)((innerFold, row) =>
                  get[JsString](innerFold, answerRowPath(beneficiary._2, row._2) \ "label") match {
                    case Success(label) => removeAnswerRow(innerFold, beneficiary._2, row._2, label.value)
                    case _              => innerFold
                  }
                )
              case _                   => outerFold
            }
          )
        case _                      => initialData
      }
    }

    def removeDraftData(): JsValue = {
      val individualBeneficiariesPath = JsPath \ "beneficiaries" \ "data" \ "beneficiaries" \ "individualBeneficiaries"
      removeRoleInCompanyFromArrayAtPath(individualBeneficiariesPath, "roleInCompany")
    }

    private def removeRoleInCompanyFromArrayAtPath(path: JsPath, key: String): JsValue =
      get[JsArray](initialData, path) match {
        case Success(individualBeneficiaries) =>
          def roleInCompanyPath(index: Int) = path \ index \ key
          individualBeneficiaries.value.toList.zipWithIndex.foldLeft(initialData)((acc, x) =>
            acc.remove(roleInCompanyPath(x._2)) match {
              case JsSuccess(roleInCompanyRemoved, _) => roleInCompanyRemoved
              case _                                  => acc
            }
          )
        case _                                => initialData
      }

    private def get[A <: JsValue](jsValue: JsValue, path: JsPath)(implicit rds: Reads[A]): Try[A] =
      jsValue.transform(path.json.pick[A]) match {
        case JsSuccess(value, _) => Success(value)
        case _                   => Failure(new Throwable(s"Error picking JSON at path $path."))
      }

  }

}
