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

package models.taxable_migration

import play.api.libs.json.{Format, Reads, Writes}

object MigrationStatus extends Enumeration {

  type MigrationStatus = Value

  def of(updated: Boolean): MigrationStatus = if (updated) Updated else NeedsUpdating

  val NothingToUpdate: Value = Value("nothing-to-update")
  val NeedsUpdating: Value = Value("needs-updating")
  val Updated: Value = Value("updated")

  implicit val reads: Reads[Value] = Reads.enumNameReads(MigrationStatus)
  implicit val writes: Writes[Value] = Writes.enumNameWrites
  implicit val formats: Format[Value] = Format.apply(reads, writes)
}
