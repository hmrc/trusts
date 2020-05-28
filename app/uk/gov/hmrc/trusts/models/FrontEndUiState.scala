/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.models

import java.time.LocalDateTime

import play.api.libs.json.{JsValue, OWrites, Reads, __}

case class FrontEndUiState(
                            draftId: String,
                            internalId: String,
                            createdAt: LocalDateTime,
                            state: JsValue
                          )

object FrontEndUiState {

  implicit lazy val reads: Reads[FrontEndUiState] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "draftId").read[String] and
        (__ \ "internalId").read[String] and
        (__ \ "createdAt").read(MongoDateTimeFormats.localDateTimeRead) and
        (__ \ "state").read[JsValue]
      ) (FrontEndUiState.apply _)
  }

  implicit lazy val writes: OWrites[FrontEndUiState] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "draftId").write[String] and
        (__ \ "internalId").write[String] and
        (__ \ "createdAt").write(MongoDateTimeFormats.localDateTimeWrite) and
        (__ \ "state").write[JsValue]
      ) (unlift(FrontEndUiState.unapply))
  }
}
