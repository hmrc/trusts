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

import play.api.libs.json._

object JsonOps {

  type JsPath = Seq[Either[Int,String]]
  type JsEntry = (JsPath, JsValue)
  type JsTraverse = PartialFunction[JsEntry, JsValue]

  implicit class JsPathOps(underlying: JsPath) {
    def isEndsWith(field: String): Boolean = underlying.lastOption.contains(Right(field))
    def isEndsWith(index: Int): Boolean = underlying.lastOption.contains(Left(index))
    def /(field: String): JsPath = underlying :+ Right(field)
    def /(index: Int): JsPath = underlying :+ Left(index)
  }

  implicit class JsValueOps(underlying: JsValue) {

    def traverse(f: JsTraverse): JsValue = {

      def traverseRec(prefix: JsPath, value: JsValue): JsValue = {

        val lifted: JsValue => JsValue = value => f.lift(prefix -> value).getOrElse(value)
        value match {
          case JsNull => lifted(JsNull)
          case boolean: JsBoolean => lifted(boolean)
          case number: JsNumber => lifted(number)
          case string: JsString => lifted(string)
          case array: JsArray =>
            val updatedArray = array.value.zipWithIndex.map {
              case (arrayValue, index) => traverseRec(prefix / index, arrayValue)
            }
            JsArray(updatedArray)

          case obj: JsObject =>
            val updatedFields = obj.fieldSet.toSeq.map {
              case (field, fieldValue) => field -> traverseRec(prefix / field, fieldValue)
            }
            JsObject(updatedFields)
        }
      }

      traverseRec(Nil, underlying)
    }

    def applyRules(): JsValue = underlying.traverse {
        case (path, JsString(phone)) if path.isEndsWith("phoneNumber") => JsString(phone.replaceAll("(0)", ""))
      }
  }
}
