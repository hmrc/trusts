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

package models.requests

import play.api.libs.json._
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes}

case class CredentialData(
                         groupIdentifier: Option[String],
                         loginTimes: LoginTimes,
                         credentials: Option[Credentials],
                         email: Option[String]
                         )

object CredentialData {

  import utils.JodaDateTimeFormatter._

  implicit val loginTimesFormats: OFormat[LoginTimes] = Json.format[LoginTimes]

  implicit val credentialsWrites: Writes[Credentials] = Json.writes[Credentials]

  implicit val optionCredentialsWrites: Writes[Option[Credentials]] = {
    case Some(value) =>
      Json.toJson(value)
    case None =>
      Json.obj(
        "providerId" -> JsString("No provider id"),
        "providerType" -> JsString("No provider type")
      )
  }

  implicit val credentialWrites : OWrites[CredentialData] = { o =>
    Json.obj(
      "groupIdentifier" -> JsString(o.groupIdentifier.getOrElse("No group identifier")),
      "loginTimes" -> Json.toJson(o.loginTimes),
      "credentials" -> Json.toJson(o.credentials),
      "email" -> JsString(o.email.getOrElse("No email"))
    )
  }

}

case class IdentifierRequest[A](request: Request[A],
                                internalId: String,
                                sessionId: String,
                                affinityGroup: AffinityGroup,
                                credentialData: CredentialData
                               )
  extends WrappedRequest[A](request)
