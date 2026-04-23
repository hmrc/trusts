/*
 * Copyright 2026 HM Revenue & Customs
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

package transformers.mdtp.beneficiaries

import models.variation.LargeType
import play.api.libs.json._
import utils.Constants._

object EmploymentRelated extends Beneficiaries[LargeType] {

  override val path: JsPath = ENTITIES \ BENEFICIARIES \ LARGE_BENEFICIARY

}
