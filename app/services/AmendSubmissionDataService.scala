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

import play.api.Logging
import play.api.libs.json._
import utils.JsonOps.{JsValueOps, doNothing, putNewValue}

import javax.inject.Inject

class AmendSubmissionDataService @Inject()(localDateService: LocalDateService) extends Logging {

  def applyRulesAndAddSubmissionDate(is5mldEnabled: Boolean, json: JsValue): JsValue = {
    val amendedJson = json.applyRules
    amendedJson.transform {
      if (is5mldEnabled) {
        putNewValue(__ \ 'submissionDate, Json.toJson(localDateService.now))
      } else {
        doNothing()
      }
    } match {
      case JsSuccess(value, _) =>
        value
      case JsError(errors) =>
        logger.error(s"[Submission Date] could not add submission date: $errors")
        amendedJson
    }
  }
}
