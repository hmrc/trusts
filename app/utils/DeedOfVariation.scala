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

object DeedOfVariation extends Enumeration {

  type DeedOfVariation = Value

  val AbsoluteInterestUnderWill = Value("Previously there was only an absolute interest under the will")
  val ReplacedWill = Value("Replaced the will trust")
  val AdditionToWill = Value("Addition to the will trust")

  implicit val reads = Reads.enumNameReads(DeedOfVariation)
  implicit val writes = Writes.enumNameWrites
  implicit val formats = Format.apply(reads, writes)

}