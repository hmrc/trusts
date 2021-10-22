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

import play.api.libs.json.{Reads, Writes}

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter => JDateTimeFormatter}
import play.api.libs.json.JsPath

object DateTimeFormatter {

  private val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

  private val formatter = JDateTimeFormatter.ofPattern(dateTimePattern)

  implicit val dateTimeReads: Reads[LocalDateTime] =
    (JsPath).read[String]
      .map(date => LocalDateTime.parse(date, formatter))

  implicit val dateTimeWrites: Writes[LocalDateTime] =
    Writes.temporalWrites[LocalDateTime, JDateTimeFormatter](formatter)

}
