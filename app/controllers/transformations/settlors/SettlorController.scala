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

package controllers.transformations.settlors

import play.api.Logging
import play.api.libs.json.JsPath
import utils.Constants._

trait SettlorController extends Logging {

  def path(`type`: String, index: Option[Int]): JsPath =
    index match {
      case Some(i) =>
        logger.info(s"[SettlorController][path] Index defined. Settlor is living and of type ${`type`}.")
        ENTITIES \ SETTLORS \ `type` \ i
      case _       =>
        logger.info(s"[SettlorController][path] Index not defined. Settlor is deceased.")
        ENTITIES \ `type`
    }

}
