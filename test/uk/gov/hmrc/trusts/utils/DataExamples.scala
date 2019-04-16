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
import play.api.libs.json.JsValue
import uk.gov.hmrc.trusts.models._
import play.api.libs.json._

trait DataExamples extends  JsonRequests {

  val nameType = NameType(
    firstName = "Oliver",
    middleName = None,
    lastName = "Johnson"
  )

  def passport(passportNumber : String = "AB123456789C") = Some(PassportType(passportNumber,new DateTime(), countryOfIssue= "IN"))

  val nino = IdentificationType(nino = Some("WA123456A"),None,None)
  val nino2 = IdentificationType(nino = Some("WA123457A"),None,None)
  def passportIdentification(passportNumber : String = "AB123456789C") = IdentificationType(nino = None,passport= passport(passportNumber),None)
  val utr = IdentificationOrgType(utr = Some("5454541615"),None)
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
    dateOfBirth = new DateTime(dateOfBirthStr),None,identification = Some(nino)))


  def indBenficiary(identification :IdentificationType = nino,dateOfBirthStr :String= "1500-01-01") =
    IndividualDetailsType(
      nameType,Some(new DateTime(dateOfBirthStr)),false,None,None,None, Some(identification))



  def trusteeOrg  = Some(TrusteeOrgType(
    name = "trustee as company",
    identification = Some(utr),
    phoneNumber = None,
    email = email))


  def registrationWithStartDate(date : DateTime ) = {
    val trustDetailsType = defaultTrustDetails.copy(startDate = date)
    registration(Some(trustDetailsType))
  }


  def registrationWithEfrbsStartDate(date : DateTime, typeOfTrust: TypeOfTrust.Value) = {
    val trustDetailsType = registrationRequest.trust.details.copy(efrbsStartDate = Some(date),
      typeOfTrust = typeOfTrust.toString)
    registration(Some(trustDetailsType))
  }

  def listOfIndividualTrustees = List(TrusteeType(trusteeIndividual(),None),TrusteeType(trusteeIndividual("2030-01-01"),None))
  def listOfOrgTrustees = List(TrusteeType(None,trusteeOrg),TrusteeType(None,trusteeOrg))
  def listOfIndAndOrgTrustees = List(TrusteeType(trusteeIndividual("2030-01-01"),trusteeOrg))
  def listOfDuplicateIndAndOrgTrustees = List(TrusteeType(None,trusteeOrg),TrusteeType(trusteeIndividual("2030-01-01"),trusteeOrg),TrusteeType(trusteeIndividual("2030-01-01"),None),TrusteeType(trusteeIndividual("2030-01-01"),None),TrusteeType(trusteeIndividual("2030-01-01"),None),TrusteeType(trusteeIndividual("2030-01-01"),None))


  def registrationWithTrustess(updatedTrustees : Option[List[TrusteeType]] ) = {
    val trustEntities = defaultTrustEntities.copy(trustees = updatedTrustees)
    registration(trustEntities =Some(trustEntities))
  }

  def beneficiaryTypeEntity(individualDetails: Option[List[IndividualDetailsType]] = Some(List(indBenficiary(),indBenficiary(nino2))))
  = BeneficiaryType( individualDetails, None,None,None,None,None,None)

  def registrationWithBeneficiary(beneficiaryType: BeneficiaryType = beneficiaryTypeEntity()  ) = {
    val trustEntities = defaultTrustEntities.copy(beneficiary = beneficiaryType)
    registration(trustEntities =Some(trustEntities))
  }

  def defaultTrustDetails = registrationRequest.trust.details
  def defaultTrustEntities = registrationRequest.trust.entities

  private def registration(trustDetailsType: Option[TrustDetailsType]= Some(defaultTrustDetails),
                           trustEntities: Option[TrustEntitiesType]= Some(defaultTrustEntities)) = {

    val trust = registrationRequest.trust
    Registration(
      trust =  trust.copy(details = trustDetailsType.get, entities = trustEntities.get),
      matchData = registrationRequest.matchData,
      correspondence = registrationRequest.correspondence,
      yearsReturns = registrationRequest.yearsReturns,
      declaration = registrationRequest.declaration,
      agentDetails = None)
  }


  def trustWithoutBeneficiary : String = {
    val json = getJsonValueFromFile("valid-trusts-registration-api.json")
    val jsonTransformer = (__  \ 'trust \  'entities \ 'beneficiary ).json.prune
    json.transform(jsonTransformer).get.toString()
  }

  def trustWithValues(indBenficiaryDob : String ="2001-01-01",
                      settlorNino :String = "ST019092",
                      settlorDob :String = "2001-01-01",
                      settlorUtr :String = "1234561235" ,
                      typeOfTrust :String= TypeOfTrust.EMPLOYMENT_RELATED_TRUST.toString
  ) : String = {
    val json = getJsonValueFromFile("trusts-dynamic.json")
    json.toString().replace("{indBeneficiaryDob}", indBenficiaryDob)
      .replace("{settlorNino}", settlorNino).replace("{settlorDob}", settlorDob)
      .replace("{settlorUtr}", settlorUtr).replace("{typeOfTrust}", typeOfTrust)
  }

  def willTrustWithValues(
                           deceasedDateOfBirth : String ="2001-01-01",
                           deceasedDateOfDeath : String ="2016-01-01",
                           deceasedNino :String = "KC456736",
                           typeOfTrust :String= TypeOfTrust.WILL_TRUST.toString,
                           protectorNino :String = "AB123456K") : Registration = {
    val json = getJsonValueFromFile("will-trust-dynamic.json")
    getJsonValueFromString(json.toString().
      replace("{deceasedDateOfBirth}", deceasedDateOfBirth).
      replace("{deceasedDateOfDeath}", deceasedDateOfDeath).
      replace("{typeOfTrust}", typeOfTrust).
      replace("{protectorNino}", protectorNino).
      replace("{deceasedNino}", deceasedNino)).
      validate[Registration].get
  }

  def heritageFundWithValues(settlorPassportNumber : String ="AB123456789D",
                             valueFull:String = "999999999999"
                     ) : Registration = {
    val json = getJsonValueFromFile("trusts-dynamic-1.json")
    getJsonValueFromString(
    json.toString().
      replace("{settlorPassportNumber}", settlorPassportNumber).
      replace("\"{valueFull}\"", valueFull))
      .validate[Registration].get

  }


  def trustWithoutAssets : String = {
    val json = getJsonValueFromFile("employment-related-trusts-1.json")
    val jsonTransformer = (__  \ 'trust \  'assets \ 'monetary ).json.prune
    json.transform(jsonTransformer).get.toString()
  }





}
