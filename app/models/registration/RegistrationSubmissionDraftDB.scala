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

package models.registration

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsValue, OWrites, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.{localDateTimeReads, localDateTimeWrites}

import java.time.LocalDateTime

case class RegistrationSubmissionDraftDB(draftId: String,
                                       internalId: String,
                                       createdAt: LocalDateTime,
                                       draftData: JsValue,
                                       reference: Option[String],
                                       inProgress: Option[Boolean])

object RegistrationSubmissionDraftDB {

  implicit lazy val reads: Reads[RegistrationSubmissionDraftDB] = (
    (__ \ "draftId").read[String] and
      (__ \ "internalId").read[String] and
      (__ \ "createdAt").read(localDateTimeReads) and
      (__ \ "draftData").read[JsValue] and
      (__ \ "reference").readNullable[String] and
      (__ \ "inProgress").readNullable[Boolean]
    ) (RegistrationSubmissionDraftDB.apply _)

  implicit lazy val writes: OWrites[RegistrationSubmissionDraftDB] = (
    (__ \ "draftId").write[String] and
      (__ \ "internalId").write[String] and
      (__ \ "createdAt").write(localDateTimeWrites) and
      (__ \ "draftData").write[JsValue] and
      (__ \ "reference").writeNullable[String] and
      (__ \ "inProgress").writeNullable[Boolean]
    ) (unlift(RegistrationSubmissionDraftDB.unapply))
}
