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

package utils

import play.api.libs.json.{Format, Reads, Writes}

object TypeOfTrust extends Enumeration {

  type TypeOfTrust = Value

  val Will = Value("Will Trust or Intestacy Trust")
  val DeedOfVariationOrFamilyAgreement = Value("Deed of Variation Trust or Family Arrangement")
  val Employment = Value("Employment Related")
  val IntervivosSettlement = Value("Inter vivos Settlement")
  val HeritageMaintenance = Value("Heritage Maintenance Fund")
  val FlatManagement = Value("Flat Management Company or Sinking Fund")

  implicit val reads: Reads[TypeOfTrust] = Reads.enumNameReads(TypeOfTrust)
  implicit val writes: Writes[TypeOfTrust] = Writes.enumNameWrites
  implicit val formats: Format[TypeOfTrust] = Format(reads, writes)

}
