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
import play.api.libs.json.{JsObject, JsPath, JsValue}

import scala.util.{Failure, Try}

trait TransformationController {

  def path(`type`: String, index: Option[Int]): JsPath

  def findJson(json: JsValue, `type`: String, index: Option[Int]): Try[JsObject] = {
    val p = path(`type`, index)
    json.transform(p.json.pick).fold(
      _ => Failure(InternalServerErrorException(s"Could not locate json at $p")),
      value => scala.util.Success(value.as[JsObject])
    )
  }

}
