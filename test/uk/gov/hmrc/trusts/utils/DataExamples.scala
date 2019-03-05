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

import org.joda.time.DateTime
import uk.gov.hmrc.trusts.models._


trait DataExamples extends  JsonRequests {

  val nameType = NameType(
    firstName = "Oliver",
    middleName = None,
    lastName = "Johnson"
  )


  val leadTrusteeIndividual = LeadTrusteeType(
    leadTrusteeInd = Some(LeadTrusteeIndType(
      name = nameType,
      dateOfBirth = new DateTime("1900-01-01"),
      identification = IdentificationType(nino = Some("WA123456A"),None,None),
      phoneNumber = "1234567890",
      email = Some("test@test.com")
    )))

  val leadTrusteeOrganisation = LeadTrusteeType(
    leadTrusteeOrg = Some(LeadTrusteeOrgType(
      name = "company name",
      identification = IdentificationOrgType(utr = Some("1234567890"),None),
      phoneNumber = "1234567890",
      email = Some("test@test.com")
    )))


  def registrationWithStartDate(date : DateTime ) = {
    val trustDetailsType = registrationRequest.details.trust.get.details.copy(startDate = date)
    val trust = registrationRequest.details.trust.get

    Registration(
      details = registrationRequest.details.copy(trust = Some(trust.copy(details = trustDetailsType))),
      matchData = None,
      correspondence = registrationRequest.correspondence,
      yearsReturns = registrationRequest.yearsReturns,
      declaration = registrationRequest.declaration,
      agentDetails = None
    )
  }






}
