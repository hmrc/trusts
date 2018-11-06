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

import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.trusts.models.ErrorResponse


class TrustsBaseController  extends BaseController{


  protected def alreadyRegisteredResponse = Forbidden(doErrorResponse("The trust is already registered.", "FORBIDDEN"))
  protected def internalServerErrorResponse = InternalServerError(doErrorResponse("Internal server error.", "INTERNAL_SERVER_ERROR"))
  protected def doErrorResponse(message: String, code: String) = Json.toJson(ErrorResponse(message, code))
  protected def matchResponse = """{"match": true}"""
  protected def noMatchResponse = """{"match": false}"""


}
