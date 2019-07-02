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

package uk.gov.hmrc.trusts.services

import java.time.format.DateTimeFormatter

import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito.when
import org.mockito.Matchers._
import uk.gov.hmrc.trusts.{BaseSpec, models}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.ExistingCheckResponse._
import uk.gov.hmrc.trusts.models.{get_trust_or_estate, _}
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.EstateFoundResponse
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._

import scala.concurrent.Future

class DesServiceSpec extends BaseSpec {

  lazy val request = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")
  val mockConnector = mock[DesConnector]

  val SUT = new DesServiceImpl(mockConnector)

  ".checkExistingTrust" should {

    "return Matched " when {
      "connector returns Matched." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }

    "return NotMatched " when {
      "connector returns NotMatched." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in {
        when(mockConnector.checkExistingTrust(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingTrust(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }


  ".checkExistingEstate" should {

    "return Matched " when {
      "connector returns Matched." in {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(Matched))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }


    "return NotMatched " when {
      "connector returns NotMatched." in {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(NotMatched))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest " when {
      "connector returns BadRequest." in {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(BadRequest))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered " when {
      "connector returns AlreadyRegistered." in {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(AlreadyRegistered))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable " when {
      "connector returns ServiceUnavailable." in {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(ServiceUnavailable))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }

      }
    }

    "return ServerError " when {
      "connector returns ServerError." in {
        when(mockConnector.checkExistingEstate(request)).
          thenReturn(Future.successful(ServerError))
        val futureResult = SUT.checkExistingEstate(request)
        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  } //checkExistingEstate


  ".registerTrust" should {

    "return SuccessRegistrationResponse " when {
      "connector returns SuccessRegistrationResponse." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.successful(RegistrationTrnResponse("trn123")))
        val futureResult = SUT.registerTrust(registrationRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("trn123")
        }
      }
    }

    "return AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(AlreadyRegisteredException))
        val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in {
        when(mockConnector.registerTrust(registrationRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.registerTrust(registrationRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  } //registerTrust

  ".registerEstate" should {

    "return RegistrationTrnResponse " when {
      "connector returns RegistrationTrnResponse." in {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.successful(RegistrationTrnResponse("trn123")))
        val futureResult = SUT.registerEstate(estateRegRequest)
        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("trn123")
        }
      }
    }

    "return AlreadyRegisteredException " when {
      "connector returns  AlreadyRegisteredException." in {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.failed(AlreadyRegisteredException))
        val futureResult = SUT.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe AlreadyRegisteredException
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in {
        when(mockConnector.registerEstate(estateRegRequest)).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val futureResult = SUT.registerEstate(estateRegRequest)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }

  } //registerTrust

  ".getSubscriptionId" should {

    "return SubscriptionIdResponse " when {
      "connector returns SubscriptionIdResponse." in {
        when(mockConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.successful(SubscriptionIdResponse("123456789")))
        val futureResult = SUT.getSubscriptionId("trn123456789")
        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("123456789")
        }
      }
    }

    "return same Exception " when {
      "connector returns  exception." in {
        when(mockConnector.getSubscriptionId("trn123456789")).
          thenReturn(Future.failed(new InternalServerErrorException("")))
        val futureResult = SUT.getSubscriptionId("trn123456789")

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  } //getSubscriptionId

  ".getTrustInfo" should {
    "return TrustFoundResponse" when {
      "TrustFoundResponse is returned from DES Connector with a Processed flag and a trust body" in {

        val utr = "1234567890"

        val dateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")
        val dateTime = dateTimeFormatter.parseDateTime("01/07/2000 14:50:20")

        val getTrust = Some(GetTrust(get_trust_or_estate.MatchData("2134514321"),
          Correspondence(abroadIndicator = true, "Nelson James",
            AddressType("1010 EASY ST", "OTTAWA", Some("ONTARIO"), Some("ONTARIO"), Some("K1A 0B1"), "GB"), "+10392019203"
          ),
          Declaration(
            NameType("Nelson",
              Some("Nicola"), "James"),
            AddressType("1010 EASY ST", "OTTAWA", Some("ONTARIO"), Some("ONTARIO"), Some("K1A 0B1"), "GB")),
          DisplayTrust(
            TrustDetailsType(dateTime, Some("AD"), Some("GB"),
              Some(ResidentialStatusType(Some(UkType(scottishLaw = false, Some("GB"))), None)), "Will Trust or Intestacy Trust",
              Some("Previously there was only an absolute interest under the will"), Some(true), Some(dateTime)),
            DisplayTrustEntitiesType(
              Some(List(DisplayTrustNaturalPersonType("1", Some("01"), NameType("Nicola", Some("Andrey"), "Jackson"), Some(dateTime),
              Some(DisplayTrustIdentificationType(Some("2222200000000"),
              None, None, None)), "2017-02-28"))),
              DisplayTrustBeneficiaryType(
                Some(List(DisplayTrustIndividualDetailsType("1", Some("01"), NameType("Nicola", Some("Andrey"), "Jackson"), Some(dateTime), vulnerableBeneficiary = true, Some("Director"), Some(true), Some("0"),
              Some(DisplayTrustIdentificationType(Some("2222200000000"), None, None, None)),
              "2017-02-28"))),
              Some(List(
                DisplayTrustCompanyType("1", Some("01"), "", Some(true), Some("0"),
                  DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None), "2017-02-28"))),
              Some(List(
                DisplayTrustBeneficiaryTrustType("1", Some("01"), "Nelson Ltd ", Some(true), Some("0"),
                  DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None), "2017-02-28"))),
              Some(List(
                DisplayTrustCharityType("1", Some("01"), "Nelson Ltd", Some(true), Some("0"),
                  DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None), "2017-02-28"))),
              Some(List(
                DisplayTrustUnidentifiedType("1", Some("01"), "Reserve money", Some(true), Some("0"), "2017-02-28"))),
              Some(List(
                DisplayTrustLargeType("1", Some("01"), "Nelson Ltd", "This is a must", Some("This is a must"), Some("This is a must"), Some("This is a must"), Some("This is a must"), "0",
                  Some(
                    DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None)), Some(true), Some("0"), "2017-02-28"))),
              Some(List(
                DisplayTrustOtherType("1", Some("01"), "Joint Fund", Some(AddressType("1010 EASY ST", "OTTAWA", Some("ONTARIO"), Some("ONTARIO"), Some("K1A 0B1"), "GB")), Some(true), Some("0"), "2017-02-28")))),
              Some(DisplayTrustWillType("1", Some("01"), NameType("James", Some("Kingsley"), "Bond"), Some(dateTime), Some(dateTime),
                Some(DisplayTrustIdentification(Some("2222200000000"), None, None)),
                "2017-02-28")), DisplayTrustLeadTrusteeType(None, None),
              Some(List(
                DisplayTrustTrusteeType(
                  Some(DisplayTrustTrusteeIndividualType("1", Some("01"), NameType("Tamara", Some("Hingis"), "Jones"), Some(dateTime), Some("+447456788112"),
                    Some(DisplayTrustIdentificationType(
                      Some("2222200000000"), None, None, None)), "2017-02-28")), Some(DisplayTrustTrusteeOrgType("1", None, "MyOrg Incorporated", Some("+447456788112"), Some("a"),
                    DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None), "2017-02-28"))))),
              Some(DisplayTrustProtectorsType(Some(List(DisplayTrustProtector("1", Some("01"), NameType("Bruce", Some("Bob"), "Branson"), Some(dateTime),
                Some(DisplayTrustIdentificationType(Some("2222200000000"), None, None, None)), "2017-02-28"))),
                Some(List(DisplayTrustProtectorCompany("1", Some("01"), "Raga Dandy",
                  DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None), "2017-02-28"))))),
              Some(DisplayTrustSettlors(Some(List(DisplayTrustSettlor("1", Some("01"), NameType("Bruce", Some("Bob"), "Branson"), Some(dateTime),
                Some(DisplayTrustIdentificationType(Some("2222200000000"), None, None, None)), "2017-02-28"))),
                Some(List(DisplayTrustSettlorCompany("1", Some("01"), "Completors Limited", Some("Trading"), Some(true), Some(DisplayTrustIdentificationOrgType(Some("2222200000000"), None, None)), "2017-02-28")))))),
            Assets(
              Some(List(AssetMonetaryAmount(0))),
              Some(List(PropertyLandType(Some("Tokyo Campus"), Some(AddressType("10 Enderson Road ", "Cheapside", Some("Riverside "), Some("Boston "), Some("SN8 4DD"), "GB")), 1892090, 1699000))),
              Some(List(SharesType("0", "Smart Estates", "Ordinary shares", "Quoted", 9891828))),
              Some(List(BusinessAssetType("Lone Wolf Ltd", Some("Travel Business"), AddressType("Suite 10", "Wealthy Arena", Some("Trafagar Square"), Some("London"), Some("SE2 2HB"), "GB"), 0))),
              Some(List(PartnershipType("Real Estates partnership", dateTime))), Some(List(OtherAssetType("Jewelries", 781720)))))))

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(TrustFoundResponse(getTrust, ResponseHeader("Processed", "1"))))

        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe TrustFoundResponse(getTrust, ResponseHeader("Processed", "1"))
        }
      }
    }

    "return TrustFoundResponse" when {
      "TrustFoundResponse is returned from DES Connector" in {

        val utr = "1234567890"

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(TrustFoundResponse(None, ResponseHeader("In Processing", "1"))))

        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe TrustFoundResponse(None, ResponseHeader("In Processing", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "InvalidUTRResponse is returned from DES Connector" in {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val invalidUtr = "123456789"
        val futureResult = SUT.getTrustInfo(invalidUtr)

        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "InvalidRegimeResponse is returned from DES Connector" in {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "BadRequestResponse is returned from DES Connector" in {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "ResourceNotFoundResponse is returned from DES Connector" in {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "InternalServerErrorResponse is returned from DES Connector" in {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "ServiceUnavailableResponse is returned from DES Connector" in {

        when(mockConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "123456789"
        val futureResult = SUT.getTrustInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  } // getTrustInfo

  ".getEstateInfo" should {
    "return EstateFoundResponse" when {
      "EstateFoundResponse is returned from DES Connector" in {

        val utr = "1234567890"

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(EstateFoundResponse(None, ResponseHeader("In Processing", "1"))))

        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe EstateFoundResponse(None, ResponseHeader("In Processing", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "InvalidUTRResponse is returned from DES Connector" in {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(InvalidUTRResponse))

        val invalidUtr = "123456789"
        val futureResult = SUT.getEstateInfo(invalidUtr)

        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "InvalidRegimeResponse is returned from DES Connector" in {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(InvalidRegimeResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "BadRequestResponse is returned from DES Connector" in {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(BadRequestResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "ResourceNotFoundResponse is returned from DES Connector" in {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(ResourceNotFoundResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "InternalServerErrorResponse is returned from DES Connector" in {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(InternalServerErrorResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "ServiceUnavailableResponse is returned from DES Connector" in {

        when(mockConnector.getEstateInfo(any())(any())).thenReturn(Future.successful(ServiceUnavailableResponse))

        val utr = "123456789"
        val futureResult = SUT.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  } // getEstateInfo
}
