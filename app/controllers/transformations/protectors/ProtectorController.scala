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

package controllers.transformations.protectors

import play.api.Logging
import play.api.libs.json.JsPath
import utils.Constants._

trait ProtectorController extends Logging {

  def path(`type`: String, index: Option[Int]): JsPath = {
    index match {
      case Some(i) =>
        ENTITIES \ PROTECTORS \ `type` \ i
      case _ =>
        logger.warn(s"Index should not be None for protector type ${`type`}.")
        JsPath
    }
  }
}
