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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.Constants.GB

import java.time.LocalDate
import scala.language.implicitConversions

case class AgentDetailsBC(internalReference: String,
                          name: String,
                          hasUkAddress: Boolean,
                          ukAddress: Option[AddressType],
                          internationalAddress: Option[AddressType],
                          telephoneNumber: String,
                          agentARN: String)

object AgentDetailsBC {

  implicit val oldReads: Reads[AgentDetailsBC] = {

    val ukAddressReads: Reads[AddressType] = {
      (
        (__ \ "line1").read[String] and
          (__ \ "line2").read[String] and
          (__ \ "line3").readNullable[String] and
          (__ \ "line4").readNullable[String] and
          (__ \ "postcode").readNullable[String]).tupled.map {
        case (line1, line2, line3, line4, postCode) =>
          AddressType(line1, line2, line3, line4, postCode, GB)
      }
    }

    (
      (__ \ "internalReference").read[String] and
        (__ \ "name").read[String] and
        (__ \ "addressYesNo").read[Boolean] and
        (__ \ "ukAddress").readNullable[AddressType](ukAddressReads) and
        (__ \ "internationalAddress").readNullable[AddressType] and
        (__ \ "telephoneNumber").read[String] and
        (__ \ "agentARN").read[String]
      )(AgentDetailsBC.apply _)
  }

  implicit val newWrites: Writes[AgentDetailsBC] = {

    val ukAddressWrites: Writes[AddressType] = {
      (
        (__ \ "line1").write[String] and
          (__ \ "line2").write[String] and
          (__ \ "line3").writeNullable[String] and
          (__ \ "line4").writeNullable[String] and
          (__ \ "postCode").writeNullable[String] and
          (__ \ "country").write[String]
        )(unlift(AddressType.unapply))
    }

    (
      (__ \ "internalReference").write[String] and
        (__ \ "name").write[String] and
        (__ \ "addressUKYesNo").write[Boolean] and
        (__ \ "ukAddress").writeNullable[AddressType](ukAddressWrites) and
        (__ \ "internationalAddress").writeNullable[AddressType] and
        (__ \ "telephoneNumber").write[String] and
        (__ \ "agentARN").write[String]
      )(unlift(AgentDetailsBC.unapply))
  }
}

case class AddressTypeNoCountry(line1: String,
                                line2: String,
                                line3: Option[String],
                                line4: Option[String],
                                postcode: Option[String])

object AddressTypeNoCountry {
  implicit val formats: Format[AddressTypeNoCountry] = Json.format[AddressTypeNoCountry]
}

sealed trait AssetBC

object AssetBC {

  implicit class ReadsWithContravariantOr[A](a: Reads[A]) {
    def or[B >: A](b: Reads[B]): Reads[B] =
      a.map[B](identity).orElse(b)
  }

  implicit def convertToSupertype[A, B >: A](a: Reads[A]): Reads[B] = a.map(identity)

  implicit lazy val reads: Reads[AssetBC] = {
    MoneyAssetBC.oldReads or
      PropertyOrLandAssetBC.oldReads or
      SharesAssetBC.oldReads or
      BusinessAssetBC.oldReads or
      PartnershipAssetBC.oldReads or
      OtherAssetBC.oldReads
  }

}

sealed trait AssetBCFormats {

  implicit class StringToLong(reads: Reads[String]) {
    def toLong: Reads[Long] = reads.map(_.toLong)
  }

  implicit class OptionalStringToLong(reads: Reads[Option[String]]) {
    def toLong: Reads[Option[Long]] = reads.map(_.map(_.toLong))
  }

}

case class MoneyAssetBC(`type`: String,
                        value: Option[Long],
                        status: Option[String]) extends AssetBC

object MoneyAssetBC extends AssetBCFormats {

  implicit val oldReads: Reads[MoneyAssetBC] = {
    (
      (__ \ "whatKindOfAsset").read[String].filter(_ == "Money") and
        (__ \ "assetMoneyValue").readNullable[String].toLong and
        (__ \ "status").readNullable[String]
      )(MoneyAssetBC.apply _)
  }

  implicit val newWrites: Writes[MoneyAssetBC] = {
    (
      (__ \ "whatKindOfAsset").write[String] and
        (__ \ "moneyValue").writeNullable[Long] and
        (__ \ "status").writeNullable[String]
      )(unlift(MoneyAssetBC.unapply))
  }
}

case class PropertyOrLandAssetBC(`type`: String,
                                 hasAddress: Option[Boolean],
                                 hasUkAddress: Option[Boolean],
                                 ukAddress: Option[AddressTypeNoCountry],
                                 internationalAddress: Option[AddressType],
                                 description: Option[String],
                                 totalValue: Option[Long],
                                 trustOwnsPropertyOrLand: Option[Boolean],
                                 valueInTrust: Option[Long],
                                 status: Option[String]) extends AssetBC

object PropertyOrLandAssetBC extends AssetBCFormats {

  implicit val oldReads: Reads[PropertyOrLandAssetBC] = {
    (
      (__ \ "whatKindOfAsset").read[String].filter(_ == "PropertyOrLand") and
        (__ \ "propertyOrLandAddressYesNo").readNullable[Boolean] and
        (__ \ "propertyOrLandAddressUKYesNo").readNullable[Boolean] and
        (__ \ "ukAddress").readNullable[AddressTypeNoCountry] and
        (__ \ "internationalAddress").readNullable[AddressType] and
        (__ \ "propertyOrLandDescription").readNullable[String] and
        (__ \ "propertyOrLandTotalValue").readNullable[Long] and
        (__ \ "trustOwnAllThePropertyOrLand").readNullable[Boolean] and
        (__ \ "propertyOrLandValueTrust").readNullable[Long] and
        (__ \ "status").readNullable[String]
      )(PropertyOrLandAssetBC.apply _)
  }

  implicit val newWrites: Writes[PropertyOrLandAssetBC] = {
    (
      (__ \ "whatKindOfAsset").write[String] and
        (__ \ "propertyOrLandAddressYesNo").writeNullable[Boolean] and
        (__ \ "propertyOrLandAddressUkYesNo").writeNullable[Boolean] and
        (__ \ "propertyOrLandUkAddress").writeNullable[AddressTypeNoCountry] and
        (__ \ "propertyOrLandInternationalAddress").writeNullable[AddressType] and
        (__ \ "propertyOrLandDescription").writeNullable[String] and
        (__ \ "propertyOrLandTotalValue").writeNullable[Long] and
        (__ \ "propertyOrLandTrustOwnsAllYesNo").writeNullable[Boolean] and
        (__ \ "propertyOrLandValueInTrust").writeNullable[Long] and
        (__ \ "status").writeNullable[String]
      )(unlift(PropertyOrLandAssetBC.unapply))
  }
}

case class SharesAssetBC(`type`: String,
                         sharesInPortfolio: Option[Boolean],
                         portfolioSharesName: Option[String],
                         portfolioSharesOnStockExchangeYesNo: Option[Boolean],
                         portfolioSharesQuantity: Option[Long],
                         portfolioSharesValue: Option[Long],
                         nonPortfolioSharesName: Option[String],
                         nonPortfolioSharesOnStockExchangeYesNo: Option[Boolean],
                         nonPortfolioSharesClass: Option[String],
                         nonPortfolioSharesQuantity: Option[Long],
                         nonPortfolioSharesValue: Option[Long],
                         status: Option[String]) extends AssetBC

object SharesAssetBC extends AssetBCFormats {

  implicit val oldReads: Reads[SharesAssetBC] = {
    (
      (__ \ "whatKindOfAsset").read[String].filter(_ == "Shares") and
        (__ \ "sharesInAPortfolio").readNullable[Boolean] and
        (__ \ "name").readNullable[String] and
        (__ \ "portfolioListedOnTheStockExchange").readNullable[Boolean] and
        (__ \ "portfolioQuantityInTheTrust").readNullable[String].toLong and
        (__ \ "portfolioValue").readNullable[String].toLong and
        (__ \ "shareCompanyName").readNullable[String] and
        (__ \ "listedOnTheStockExchange").readNullable[Boolean] and
        (__ \ "class").readNullable[String] and
        (__ \ "quantityInTheTrust").readNullable[String].toLong and
        (__ \ "value").readNullable[String].toLong and
        (__ \ "status").readNullable[String]
      )(SharesAssetBC.apply _)
  }

  implicit val newWrites: Writes[SharesAssetBC] = {
    (
      (__ \ "whatKindOfAsset").write[String] and
        (__ \ "sharesInPortfolioYesNo").writeNullable[Boolean] and
        (__ \ "portfolioSharesName").writeNullable[String] and
        (__ \ "portfolioSharesOnStockExchangeYesNo").writeNullable[Boolean] and
        (__ \ "portfolioSharesQuantity").writeNullable[Long] and
        (__ \ "portfolioSharesValue").writeNullable[Long] and
        (__ \ "nonPortfolioSharesName").writeNullable[String] and
        (__ \ "nonPortfolioSharesOnStockExchangeYesNo").writeNullable[Boolean] and
        (__ \ "nonPortfolioSharesClass").writeNullable[String] and
        (__ \ "nonPortfolioSharesQuantity").writeNullable[Long] and
        (__ \ "nonPortfolioSharesValue").writeNullable[Long] and
        (__ \ "status").writeNullable[String]
      )(unlift(SharesAssetBC.unapply))
  }
}

case class BusinessAssetBC(`type`: String,
                           name: Option[String],
                           description: Option[String],
                           hasUkAddress: Option[Boolean],
                           ukAddress: Option[AddressTypeNoCountry],
                           internationalAddress: Option[AddressType],
                           value: Option[Long],
                           status: Option[String]) extends AssetBC

object BusinessAssetBC extends AssetBCFormats {

  implicit val oldReads: Reads[BusinessAssetBC] = {
    (
      (__ \ "whatKindOfAsset").read[String].filter(_ == "Business") and
        (__ \ "name").readNullable[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "addressUkYesNo").readNullable[Boolean] and
        (__ \ "ukAddress").readNullable[AddressTypeNoCountry] and
        (__ \ "internationalAddress").readNullable[AddressType] and
        (__ \ "value").readNullable[String].toLong and
        (__ \ "status").readNullable[String]
      )(BusinessAssetBC.apply _)
  }

  implicit val newWrites: Writes[BusinessAssetBC] = {
    (
      (__ \ "whatKindOfAsset").write[String] and
        (__ \ "businessName").writeNullable[String] and
        (__ \ "businessDescription").writeNullable[String] and
        (__ \ "businessAddressUkYesNo").writeNullable[Boolean] and
        (__ \ "businessUkAddress").writeNullable[AddressTypeNoCountry] and
        (__ \ "businessInternationalAddress").writeNullable[AddressType] and
        (__ \ "businessValue").writeNullable[Long] and
        (__ \ "status").writeNullable[String]
      )(unlift(BusinessAssetBC.unapply))
  }
}

case class PartnershipAssetBC(`type`: String,
                              description: Option[String],
                              startDate: Option[LocalDate],
                              status: Option[String]) extends AssetBC

object PartnershipAssetBC {

  implicit val oldReads: Reads[PartnershipAssetBC] = {
    (
      (__ \ "whatKindOfAsset").read[String].filter(_ == "Partnership") and
        (__ \ "partnershipDescription").readNullable[String] and
        (__ \ "partnershipStartDate").readNullable[LocalDate] and
        (__ \ "status").readNullable[String]
      )(PartnershipAssetBC.apply _)
  }

  implicit val newWrites: Writes[PartnershipAssetBC] = {
    (
      (__ \ "whatKindOfAsset").write[String] and
        (__ \ "partnershipDescription").writeNullable[String] and
        (__ \ "partnershipStartDate").writeNullable[LocalDate] and
        (__ \ "status").writeNullable[String]
      )(unlift(PartnershipAssetBC.unapply))
  }
}

case class OtherAssetBC(`type`: String,
                        description: Option[String],
                        value: Option[Long],
                        status: Option[String]) extends AssetBC

object OtherAssetBC extends AssetBCFormats {

  implicit val oldReads: Reads[OtherAssetBC] = {
    (
      (__ \ "whatKindOfAsset").read[String].filter(_ == "Other") and
        (__ \ "otherAssetDescription").readNullable[String] and
        (__ \ "otherAssetValue").readNullable[String].toLong and
        (__ \ "status").readNullable[String]
      )(OtherAssetBC.apply _)
  }

  implicit val newWrites: Writes[OtherAssetBC] = {
    (
      (__ \ "whatKindOfAsset").write[String] and
        (__ \ "otherDescription").writeNullable[String] and
        (__ \ "otherValue").writeNullable[Long] and
        (__ \ "status").writeNullable[String]
      )(unlift(OtherAssetBC.unapply))
  }
}
