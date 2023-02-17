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

package utils

import play.api.libs.json.{JsPath, __}

object Constants {

  val GB = "GB"

  val ALREADY_REGISTERED_CODE = "ALREADY_REGISTERED"
  val ALREADY_REGISTERED_TRUSTS_MESSAGE = "The trust is already registered."
  val ALREADY_REGISTERED_ESTATE_MESSAGE = "The estate is already registered."

  val NO_MATCH_CODE = "NO_MATCH"
  val NO_MATCH_MESSAGE = "No match has been found in HMRC's records."
  val INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR"
  val INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error."

  val INSUFFICIENT_ENROLMENT_MESSAGE = "Insufficient enrolment for authorised user."
  val UNAUTHORISED = "UNAUTHORISED"

  val NO_DRAFT_ID = "NO_DRAFT_ID"
  val NO_DRAFT_ID_MESSAGE = "No draft registration identifier provided."

  val CONTENT_TYPE = "Content-Type"
  val CONTENT_TYPE_JSON = "application/json; charset=utf-8"

  val X_API_KEY = "X-API-Key"

  val INVALID_UTR_CODE = "INVALID_UTR"
  val INVALID_UTR_MESSAGE = "The UTR provided is invalid."

  val TRUST: JsPath = __ \ "details" \ "trust"
  val ENTITIES: JsPath = TRUST \ "entities"
  val MATCH_DATA: JsPath = __ \ "matchData"
  val CORRESPONDENCE: JsPath = __ \ "correspondence"
  val DECLARATION: JsPath = __ \ "declaration"
  val SUBMISSION_DATE: JsPath = __ \ "submissionDate"
  val YEARS_RETURNS: JsPath = __ \ "yearsReturns"

  val ASSETS = "assets"
  val BENEFICIARIES = "beneficiary"
  val OTHER_INDIVIDUALS = "naturalPerson"
  val PROTECTORS = "protectors"
  val SETTLORS = "settlors"
  val TRUSTEES = "trustees"
  val LEAD_TRUSTEE = "leadTrustees"

  val DETAILS = "details"

  val MONEY_ASSET = "monetary"
  val PROPERTY_OR_LAND_ASSET = "propertyOrLand"
  val SHARES_ASSET = "shares"
  val BUSINESS_ASSET = "business"
  val PARTNERSHIP_ASSET = "partnerShip"
  val OTHER_ASSET = "other"
  val NON_EEA_BUSINESS_ASSET = "nonEEABusiness"

  val INDIVIDUAL_BENEFICIARY = "individualDetails"
  val COMPANY_BENEFICIARY = "company"
  val TRUST_BENEFICIARY = "trust"
  val CHARITY_BENEFICIARY = "charity"
  val UNIDENTIFIED_BENEFICIARY = "unidentified"
  val LARGE_BENEFICIARY = "large"
  val OTHER_BENEFICIARY = "other"

  val INDIVIDUAL_PROTECTOR = "protector"
  val BUSINESS_PROTECTOR = "protectorCompany"

  val DECEASED_SETTLOR = "deceased"
  val INDIVIDUAL_SETTLOR = "settlor"
  val BUSINESS_SETTLOR = "settlorCompany"

  val INDIVIDUAL_LEAD_TRUSTEE = "leadTrusteeInd"
  val BUSINESS_LEAD_TRUSTEE = "leadTrusteeOrg"
  val INDIVIDUAL_TRUSTEE = "trusteeInd"
  val BUSINESS_TRUSTEE = "trusteeOrg"

  val EXPRESS = "expressTrust"
  val UK_RESIDENT = "trustUKResident"
  val TAXABLE = "trustTaxable"
  val UK_PROPERTY = "trustUKProperty"
  val RECORDED = "trustRecorded"
  val UK_RELATION = "trustUKRelation"
  val START_DATE = "startDate"
  val SCHEDULE_3A_EXEMPT = "schedule3aExempt"

  val LAW_COUNTRY = "lawCountry"
  val ADMINISTRATION_COUNTRY = "administrationCountry"
  val TYPE_OF_TRUST = "typeOfTrust"
  val DEED_OF_VARIATION = "deedOfVariation"
  val INTER_VIVOS = "interVivos"
  val EFRBS_START_DATE = "efrbsStartDate"
  val RESIDENTIAL_STATUS = "residentialStatus"

  val LINE_NUMBER = "lineNo"
  val BP_MATCH_STATUS = "bpMatchStatus"
  val ENTITY_START = "entityStart"
  val ENTITY_END = "entityEnd"
  val DATE_OF_DEATH = "dateOfDeath"
  val LEGALLY_INCAPABLE = "legallyIncapable"

  val TRUST_OR_ESTATE_DISPLAY = "trustOrEstateDisplay"
  val GET_TRUST = "getTrust"
  val RESPONSE_HEADER = "responseHeader"
  val DFMCA_RETURN_USER_STATUS = "dfmcaReturnUserStatus"
  val FORM_BUNDLE_NUMBER = "formBundleNo"

  val CREATED_AT = "createdAt"
  val DRAFT_ID = "draftId"
  val REFERENCE = "reference"
  val DATA = "data"

  val TRUST_NAME: JsPath = CORRESPONDENCE \ "name"

  val VULNERABLE_BENEFICIARY = "vulnerableBeneficiary"
  val HAS_DISCRETION = "beneficiaryDiscretion"
  val SHARE_OF_INCOME = "beneficiaryShareOfIncome"
  val ROLE_IN_COMPANY = "beneficiaryType"

  val COMPANY_TYPE = "companyType"
  val COMPANY_TIME = "companyTime"

  val IDENTIFICATION = "identification"
  val UTR = "utr"
  val PASSPORT = "passport"

  val EMPLOYMENT_RELATED_TRUST = "Employment Related"

  val IS_PORTFOLIO = "isPortfolio"
  val SHARE_CLASS_DISPLAY = "shareClassDisplay"
  val DETAILS_TYPE = "detailsType"
}
