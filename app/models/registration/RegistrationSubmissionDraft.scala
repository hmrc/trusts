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

package models.registration

import java.time.LocalDateTime

import play.api.libs.json._
import models.MongoDateTimeFormats

object RegistrationSubmission {
  // Piece to be inserted into final registration data. data == JsNull means remove value.
  case class MappedPiece(elementPath: String, data: JsValue)

  object MappedPiece {

    val path = JsPath \ "registration"

    implicit lazy val format: OFormat[MappedPiece] = Json.format[MappedPiece]
  }

  // Answer row and section, for display in print summary.
  case class AnswerRow(label: String, answer: String, labelArg: String)

  object AnswerRow {
    implicit lazy val format: OFormat[AnswerRow] = Json.format[AnswerRow]
  }

  case class AnswerSection(headingKey: Option[String],
                                                 rows: Seq[AnswerRow],
                                                 sectionKey: Option[String])

  object AnswerSection {

    val path = JsPath \ "answerSections"

    implicit lazy val format: OFormat[AnswerSection] = Json.format[AnswerSection]
  }

  // Set of data sent by sub-frontend, with user answers, status, any mapped pieces and answer sections.
  case class DataSet(data: JsValue,
                     status: Option[Status],
                     registrationPieces: List[MappedPiece],
                     answerSections: List[AnswerSection])

  object DataSet {
    implicit lazy val format: OFormat[DataSet] = Json.format[DataSet]
  }
}

// Primary front end draft data (e.g, trusts-frontend), including reference and in-progress.
case class RegistrationSubmissionDraftData(data: JsValue, reference: Option[String], inProgress: Option[Boolean])

object RegistrationSubmissionDraftData {
  implicit lazy val format: OFormat[RegistrationSubmissionDraftData] = Json.format[RegistrationSubmissionDraftData]
}

// Full draft data as stored in database.
case class RegistrationSubmissionDraft(
                                        draftId: String,
                                        internalId: String,
                                        createdAt: LocalDateTime,
                                        draftData: JsValue,
                                        reference: Option[String],
                                        inProgress: Option[Boolean])

object RegistrationSubmissionDraft {

  implicit lazy val reads: Reads[RegistrationSubmissionDraft] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "draftId").read[String] and
        (__ \ "internalId").read[String] and
        (__ \ "createdAt").read(MongoDateTimeFormats.localDateTimeRead) and
        (__ \ "draftData").read[JsValue] and
        (__ \ "reference").readNullable[String] and
        (__ \ "inProgress").readNullable[Boolean]
      ) (RegistrationSubmissionDraft.apply _)
  }

  implicit lazy val writes: OWrites[RegistrationSubmissionDraft] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "draftId").write[String] and
        (__ \ "internalId").write[String] and
        (__ \ "createdAt").write(MongoDateTimeFormats.localDateTimeWrite) and
        (__ \ "draftData").write[JsValue] and
        (__ \ "reference").writeNullable[String] and
        (__ \ "inProgress").writeNullable[Boolean]
      ) (unlift(RegistrationSubmissionDraft.unapply))
  }
}
