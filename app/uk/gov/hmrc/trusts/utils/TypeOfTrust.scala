/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.utils


object TypeOfTrust extends  Enumeration {
  val WILL_TRUST = Value("Will Trust or Intestacy Trust")
  val DOV_TRUST = Value("Deed of Variation Trust or Family Arrangement")
  val INTER_VIVOS_SETTLEMENT =  Value("Inter vivos Settlement")
  val EMPLOYMENT_RELATED_TRUST =   Value("Employment Related")
  val HERITAGE_MAINTENANCE_FUND_TRUST =  Value("Heritage Maintenance Fund")
  val FLAT_MANAGEMENT_TRUST =   Value("Flat Management Company or Sinking Fund")



}
