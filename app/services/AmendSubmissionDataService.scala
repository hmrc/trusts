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

package services

import play.api.Logging
import play.api.libs.json._
import services.dates.LocalDateService
import utils.JsonOps.{JsValueOps, putNewValue}

import javax.inject.{Inject, Singleton}

@Singleton
class AmendSubmissionDataService @Inject()(localDateService: LocalDateService) extends Logging {

  def applyRulesAndAddSubmissionDate(json: JsValue): JsValue = {
    val amendedJson = json.applyRules
    amendedJson.transform {
      putNewValue(__ \ Symbol("submissionDate"), Json.toJson(localDateService.now))
    } match {
      case JsSuccess(value, _) =>
        value
      case JsError(errors) =>
        logger.error(s"[AmendSubmissionDataService][applyRulesAndAddSubmissionDate] could not add submission date: $errors")
        amendedJson
    }
  }
}
