/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import play.api.Logging

case class UpdatedCounterValues(matched: Long = 0L, updated: Long = 0L, errors: Long = 0L) extends Logging {

  def +(other: UpdatedCounterValues): UpdatedCounterValues =
    copy(
      matched = matched + other.matched,
      updated = updated + other.updated,
      errors = errors + other.errors
    )

  def report(name: String): Unit =
    logger.info(s"[UpdatedCounterValues] matched=$matched updated=$updated errors=$errors name = $name  ")
}
