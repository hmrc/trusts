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

package controllers

import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorResponses._

import scala.concurrent.Future

class TrustsBaseController(cc: ControllerComponents) extends BackendController(cc) {

  protected def matchResponse = """{"match": true}"""

  protected def noMatchResponse = """{"match": false}"""

  override protected def withJsonBody[T](f: T => Future[Result])
                                        (implicit request: Request[JsValue],
                                         m: Manifest[T],
                                         reads: Reads[T]) : Future[Result] =
    request.body.validate[T] match {
      case JsSuccess(payload, _) =>
        f(payload)
      case JsError(errs) =>
        val response = handleErrorResponseByField(errs)
        Future.successful(response)
    }


  def handleErrorResponseByField(field: Seq[(JsPath, Seq[JsonValidationError])]): Result = {

    val fields = field.map { case (key, validationError) =>
      (key.toString.stripPrefix("/"), validationError.head.message)
    }
    getErrorResponse(fields.head._1, fields.head._2)
  }

  def getErrorResponse(key: String, error: String): Result = {
    error match {
      case "error.path.missing" =>
        invalidRequestErrorResponse
      case _ =>
        errors(key)
    }
  }

  protected val errors: Map[String, Result] = Map(
    "name" -> invalidNameErrorResponse,
    "utr" -> invalidUtrErrorResponse,
    "postcode" -> invalidPostcodeErrorResponse

  ).withDefaultValue(invalidRequestErrorResponse)

}
