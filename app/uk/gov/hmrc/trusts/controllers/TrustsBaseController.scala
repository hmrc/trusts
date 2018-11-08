/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.controllers

import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.trusts.models.ErrorResponse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


class TrustsBaseController extends BaseController {


  protected def alreadyRegisteredResponse = Forbidden(doErrorResponse("The trust is already registered.", "FORBIDDEN"))

  protected def internalServerErrorResponse = InternalServerError(doErrorResponse("Internal server error.", "INTERNAL_SERVER_ERROR"))

  protected def doErrorResponse(message: String, code: String) = Json.toJson(ErrorResponse(message, code))

  protected def invalidNameErrorResponse = BadRequest(doErrorResponse("Provided name is invalid.", "INVALID_NAME"))
  protected def invalidUtrErrorResponse = BadRequest(doErrorResponse("Provided utr is invalid.", "INVALID_UTR"))
  protected def invalidPostcodeErrorResponse = BadRequest(doErrorResponse("Provided postcode is invalid.", "INVALID_POSTCODE"))
  protected def invalidRequestErrorResponse = BadRequest(doErrorResponse("Provided request is invalid.","BAD_REQUEST"))

  protected def matchResponse = """{"match": true}"""
  protected def noMatchResponse = """{"match": false}"""

  override protected def withJsonBody[T](
                                          f: (T) => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]) =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) => {
        val response = handleErrorResponseByField(errs)
        Future.successful(response)
      }
      case Failure(e) => {
        Future.successful(BadRequest(s"could not parse body due to ${e.getMessage}"))
      }
    }


  def handleErrorResponseByField(field: Seq[(JsPath, Seq[ValidationError])]): Result = {

    val fields = field.map { case (key, validationError) =>
      (key.toString.stripPrefix("/"), validationError.head.message)
    }

    getErrorResponse(fields.head._1, fields.head._2)
  }

  def getErrorResponse(key: String, error: String): Result = {
    error match {
      case "error.path.missing" =>
        invalidRequestErrorResponse
      case e =>
        errors(key)
    }
  }

  protected val errors: Map[String, Result] = Map(
    "name" -> invalidNameErrorResponse,
    "utr" -> invalidUtrErrorResponse,
    "postcode" -> invalidPostcodeErrorResponse

  ).withDefaultValue(invalidRequestErrorResponse)


}
