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

import play.api.libs.json._

// Primary front end draft data (e.g, trusts-frontend), including reference and in-progress.
case class RegistrationSubmissionDraftData(data: JsValue, reference: Option[String], inProgress: Option[Boolean])

object RegistrationSubmissionDraftData {
  implicit lazy val format: OFormat[RegistrationSubmissionDraftData] = Json.format[RegistrationSubmissionDraftData]
}

// Piece to be inserted into final registration data. JsNull means remove value.
case class RegistrationSubmissionDraftPiece(elementPath: String, data: JsValue)

object RegistrationSubmissionDraftPiece {
  implicit lazy val format: OFormat[RegistrationSubmissionDraftPiece] = Json.format[RegistrationSubmissionDraftPiece]
}

// In-progress or completed status for a particular section (front end).
case class RegistrationSubmissionDraftStatus(section: String, status: Option[Status])

object RegistrationSubmissionDraftStatus {
  implicit lazy val format: OFormat[RegistrationSubmissionDraftStatus] = Json.format[RegistrationSubmissionDraftStatus]
}

// Set of data sent by sub-frontend, with user answers, status and any registration pieces.
case class RegistrationSubmissionDraftSetData(data: JsValue,
                                              status: Option[RegistrationSubmissionDraftStatus],
                                              registrationPieces: List[RegistrationSubmissionDraftPiece])

object RegistrationSubmissionDraftSetData {
  implicit lazy val format: OFormat[RegistrationSubmissionDraftSetData] = Json.format[RegistrationSubmissionDraftSetData]
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
