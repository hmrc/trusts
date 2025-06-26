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

package controllers.transformations

import cats.data.EitherT
import errors.ServerError
import play.api.libs.json._
import utils.Constants._
import utils.TrustEnvelope.TrustEnvelope

import scala.concurrent.Future

trait TransformationHelper {

  def path(`type`: String, index: Option[Int]): JsPath

  def findJson(json: JsValue, `type`: String, index: Option[Int]): TrustEnvelope[JsObject] = EitherT {
    val p = path(`type`, index)
    println("==============================p========================"+p+"::::::::::::::; json "+json)
    json.transform(p.json.pick).fold(
      _ => Future.successful(Left(ServerError(s"Could not locate json at $p"))),
      value => Future.successful(Right(value.as[JsObject]))
    )
  }

}

object TransformationHelper {

  def isTrustTaxable(json: JsObject): TrustEnvelope[Boolean] = EitherT {
    json.transform((TRUST \ DETAILS \ TAXABLE).json.pick[JsBoolean]) match {
      case JsSuccess(JsBoolean(value), _) => Future.successful(Right(value))
      case _ => Future.successful(Right(true))
    }
  }

}
