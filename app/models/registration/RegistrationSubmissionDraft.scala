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

package models.registration

import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.Instant

object RegistrationSubmission {
  // Piece to be inserted into final registration data. data == JsNull means remove value.
  case class MappedPiece(elementPath: String, data: JsValue)

  object MappedPiece {

    val path: JsPath = JsPath \ "registration"

    implicit lazy val format: Format[MappedPiece] = Json.format[MappedPiece]
  }

  // Answer row and section, for display in print summary.
  case class AnswerRow(label: String,
                       answer: String,
                       labelArgs: Seq[String])

  object AnswerRow {

    lazy val labelArgReads: Reads[Seq[String]] =
      (JsPath \ "labelArg").read[String].map[Seq[String]] {
        case x if x.isEmpty => Nil
        case x => x :: Nil
      } orElse
        (JsPath \ "labelArgs").readWithDefault[Seq[String]](Nil)

    implicit lazy val reads: Reads[AnswerRow] = (
      (JsPath \ "label").read[String] and
        (JsPath \ "answer").read[String] and
        labelArgReads
      )(AnswerRow.apply _)

    implicit lazy val writes: Writes[AnswerRow] = Json.writes[AnswerRow]

    implicit lazy val format: Format[AnswerRow] = Format(reads, writes)
  }

  case class AnswerSection(headingKey: Option[String],
                           rows: Seq[AnswerRow],
                           sectionKey: Option[String],
                           headingArgs: Seq[String])

  object AnswerSection {

    val path: JsPath = JsPath \ "answerSections"

    implicit val reads: Reads[AnswerSection] = (
      (JsPath \ "headingKey").readNullable[String] and
        (JsPath \ "rows").read[Seq[AnswerRow]] and
        (JsPath \ "sectionKey").readNullable[String] and
        (JsPath \ "headingArgs").readWithDefault[Seq[String]](Nil)
      )(AnswerSection.apply _)

    implicit lazy val writes: Writes[AnswerSection] = Json.writes[AnswerSection]

    implicit lazy val format: Format[AnswerSection] = Format(reads, writes)
  }

  // Set of data sent by sub-frontend, with user answers, any mapped pieces and answer sections.
  case class DataSet(data: JsValue,
                     registrationPieces: List[MappedPiece],
                     answerSections: List[AnswerSection])

  object DataSet {
    implicit lazy val format: Format[DataSet] = Json.format[DataSet]
  }
}

// Primary front end draft data (e.g, trusts-frontend), including reference and in-progress.
case class RegistrationSubmissionDraftData(data: JsValue,
                                           reference: Option[String],
                                           inProgress: Option[Boolean])

object RegistrationSubmissionDraftData {
  implicit lazy val format: Format[RegistrationSubmissionDraftData] = Json.format[RegistrationSubmissionDraftData]
}

// Full draft data as stored in database.
case class RegistrationSubmissionDraft(draftId: String,
                                       internalId: String,
                                       createdAt: Instant,
                                       draftData: JsValue,
                                       reference: Option[String],
                                       inProgress: Option[Boolean])

object RegistrationSubmissionDraft {

  implicit lazy val reads: Reads[RegistrationSubmissionDraft] = (
    (__ \ "draftId").read[String] and
      (__ \ "internalId").read[String] and
      (__ \ "createdAt").read(MongoJavatimeFormats.instantReads) and
      (__ \ "draftData").read[JsValue] and
      (__ \ "reference").readNullable[String] and
      (__ \ "inProgress").readNullable[Boolean]
    ) (RegistrationSubmissionDraft.apply _)

  implicit lazy val writes: OWrites[RegistrationSubmissionDraft] = (
    (__ \ "draftId").write[String] and
      (__ \ "internalId").write[String] and
      (__ \ "createdAt").write(MongoJavatimeFormats.instantWrites) and
      (__ \ "draftData").write[JsValue] and
      (__ \ "reference").writeNullable[String] and
      (__ \ "inProgress").writeNullable[Boolean]
    ) (unlift(RegistrationSubmissionDraft.unapply))
}
