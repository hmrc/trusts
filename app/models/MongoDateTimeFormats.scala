/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import play.api.libs.json._

import java.time.{Instant, LocalDateTime, ZoneOffset}

trait MongoDateTimeFormats {

  implicit val localDateTimeRead: Reads[LocalDateTime] = {
    case JsObject(map) if map.contains("$date") =>
      map("$date") match {
        case JsNumber(bigDecimal) =>
          JsSuccess(LocalDateTime.ofInstant(Instant.ofEpochMilli(bigDecimal.toLong), ZoneOffset.UTC))
        case JsObject(stringObject) =>
          if (stringObject.contains("$numberLong")) {
            JsSuccess(LocalDateTime.ofInstant(Instant.ofEpochMilli(BigDecimal(stringObject("$numberLong").as[JsString].value).toLong), ZoneOffset.UTC))
          } else {
            JsError("Unexpected LocalDateTime Format")
          }
        case JsString(dateValue) =>
          try {
            JsSuccess(LocalDateTime.parse(dateValue))
          } catch {
            case _: Throwable => JsError("Unexpected LocalDateTime Format")
          }
        case _ => JsError("Unexpected LocalDateTime Format")
      }
    case _ => JsError("Unexpected LocalDateTime Format")
  }

  implicit val localDateTimeWrite: Writes[LocalDateTime] = (dateTime: LocalDateTime) => Json.obj(
    "$date" -> dateTime.atZone(ZoneOffset.UTC).toInstant.toEpochMilli
  )

}

object MongoDateTimeFormats extends MongoDateTimeFormats
