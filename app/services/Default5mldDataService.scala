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

package services

import java.time.LocalDate

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json._
import utils.JsonOps.{JsValueOps, putNewValue}

class Default5mldDataService @Inject()(localDateService: LocalDateService) extends Logging {

  def addDefault5mldData(fiveMldEnabled: Boolean, json: JsValue): String = {
    if (fiveMldEnabled) {
      json.applyRules.transform(
        putNewValue(__ \ 'submissionDate, Json.toJson(localDateService.now))
      ).fold(
        _ => {
          logger.error("[Submission Date] could not add submission date")
          json.applyRules.toString
        },
        value => value.toString)
    } else {
      json.applyRules.toString
    }
  }
}
