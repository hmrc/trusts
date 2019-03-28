/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.utils

import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.{EstateRegistration, Registration}

import scala.io.Source


trait JsonRequests extends JsonUtils {


  lazy val validRegistrationRequestJson  =  getJsonFromFile("valid-trusts-registration-api.json")
  lazy val invalidRegistrationRequestJson  =  getJsonFromFile("invalid-payload-trusts-registration.json")
  lazy val invalidTrustBusinessValidation  =  getJsonFromFile("trust-business-validation-fail.json")

  lazy val estateRegistration01  =  getJsonFromFile("valid-estate-registration-01.json")


  lazy val registrationRequest = getJsonValueFromFile("valid-trusts-registration-api.json").validate[Registration].get

  lazy val invalidRegistrationRequest = getJsonValueFromFile("invalid-payload-trusts-registration.json").validate[Registration].get

  lazy val estateRegRequest = getJsonValueFromFile("valid-estate-registration-01.json").validate[EstateRegistration].get
}
