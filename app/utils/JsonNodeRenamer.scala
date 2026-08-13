/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.json.{JsObject, Json}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object JsonNodeRenamer {

  def renameNode(in: JsObject, dotNotatationPath: String, newName: String)(implicit logger: Logger): JsObject = {

    val pathIn = dotNotatationPath.split("\\.").toList

    logger.warn(s"#####! got ${Json.prettyPrint(in)}")

    @tailrec
    def getNode(js: JsObject, path: List[String]): JsObject = path match {
      case _ :: Nil => js
      case _ :: _   => getNode((js \ path.head).as[JsObject], path.tail)
      case Nil      => throw new IllegalArgumentException("path must not be empty")
    }

    @tailrec
    def rename(js: JsObject, path: List[String], newName: String): JsObject = path match {
      case h :: Nil => updateAncestry(js - h + (newName -> js(h)), pathIn.init)
      case h :: t   => rename((js \ h).as[JsObject], t, newName)
      case Nil      => throw new IllegalArgumentException("path must not be empty")
    }

    @tailrec
    def updateAncestry(js: JsObject, path: List[String]): JsObject = path match {
      case h :: Nil => in - h + (h -> js)
      case _ :: _   => updateAncestry(getNode(in, path) - path.last + (path.last, js), path.init)
      case Nil      => throw new IllegalArgumentException("path must not be empty")
    }

    Try(rename(in, pathIn, newName)) match {
      case Success(v) =>
        logger.warn(s"#####! returning ${Json.prettyPrint(v)}")
        v
      case Failure(_) =>
        logger.info(s"#####! $dotNotatationPath not found when trying to rename target to $newName")
        in
    }

  }

}
