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
  val nino = IdentificationType(nino = Some("WA123456A"),None,None)
  val utr = IdentificationOrgType(utr = Some("1234567890"),None)
  val phoneNumber = "1234567890"
  val email = Some("test@test.com")

  val leadTrusteeIndividual = LeadTrusteeType(
    leadTrusteeInd = Some(LeadTrusteeIndType(
      name = nameType,
      dateOfBirth = new DateTime("1900-01-01"),
      identification = nino,
      phoneNumber = "1234567890",
      email = Some("test@test.com")
    )))

  val leadTrusteeOrganisation = LeadTrusteeType(
    leadTrusteeOrg = Some(LeadTrusteeOrgType(
      name = "company name",
      identification = utr,
      phoneNumber = phoneNumber,
      email = email
    )))

  def trusteeIndividual(dateOfBirthStr :String= "1500-01-01") = Some(TrusteeIndividualType(name = nameType,
    dateOfBirth = new DateTime(dateOfBirthStr),None,identification = nino))

  def trusteeOrg  = Some(TrusteeOrgType(
    name = "trustee as company",
    identification = utr,
    phoneNumber = None,
    email = email))


  def registrationWithStartDate(date : DateTime ) = {
    val trustDetailsType = defaultTrustDetails.copy(startDate = date)
    registration(Some(trustDetailsType))
  }


  def registrationWithEfrbsStartDate(date : DateTime, typeOfTrust: TypeOfTrust.Value) = {
    val trustDetailsType = registrationRequest.details.trust.details.copy(efrbsStartDate = Some(date),
      typeOfTrust = typeOfTrust.toString)
    registration(Some(trustDetailsType))
  }

  def listOfIndividualTrustees = List(TrusteeType(trusteeIndividual(),None),TrusteeType(trusteeIndividual("2030-01-01"),None))
  def listOfOrgTrustees = List(TrusteeType(None,trusteeOrg),TrusteeType(None,trusteeOrg))
  def listOfIndAndOrgTrustees = List(TrusteeType(trusteeIndividual("2030-01-01"),trusteeOrg))

  def registrationWithTrustess(updatedTrustees : Option[List[TrusteeType]] ) = {
    val trustEntities = defaultTrustEntities.copy(trustees = updatedTrustees)
    registration(trustEntities =Some(trustEntities))
  }

  def defaultTrustDetails = registrationRequest.details.trust.details
  def defaultTrustEntities = registrationRequest.details.trust.entities

  private def registration(trustDetailsType: Option[TrustDetailsType]= Some(defaultTrustDetails),
                           trustEntities: Option[TrustEntitiesType]= Some(defaultTrustEntities)) = {

    val trust = registrationRequest.details.trust
    Registration(
      details = registrationRequest.details.copy(trust = trust.copy(details = trustDetailsType.get, entities = trustEntities.get)),
      matchData = None,
      correspondence = registrationRequest.correspondence,
      yearsReturns = registrationRequest.yearsReturns,
      declaration = registrationRequest.declaration,
      agentDetails = None

    )
  }
}
