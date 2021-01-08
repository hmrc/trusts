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

package controllers.transformations

import exceptions.InternalServerErrorException
import play.api.libs.json.{JsObject, JsValue, __}

import scala.util.{Failure, Try}

trait TransformationController {

  val section: String

  def findJson(json: JsValue, key: String, index: Int): Try[JsObject] = {
    val path = __ \ 'details \ 'trust \ section \ key \ index
    json.transform(path.json.pick).fold(
      _ => Failure(InternalServerErrorException(s"Could not locate json at $path")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

}
