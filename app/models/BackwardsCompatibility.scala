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

import java.time.LocalDate

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
          AddressType(line1, line2, line3, line4, postCode, "GB")
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
          (__ \ "postcode").writeNullable[String] and
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

sealed trait AssetBC

object AssetBC {

  implicit class ReadsWithContravariantOr[A](a: Reads[A]) {
    def or[B >: A](b: Reads[B]): Reads[B] =
      a.map[B](identity).orElse(b)
  }

  implicit def convertToSupertype[A, B >: A](a: Reads[A]): Reads[B] = a.map(identity)

  implicit lazy val reads: Reads[AssetBC] = {
    MoneyAssetBC.oldReads or
      SharesAssetBC.oldReads or
      BusinessAssetBC.oldReads or
      PartnershipAssetBC.formats or
      OtherAssetBC.oldReads
  }

}

case class MoneyAssetBC(value: Long,
                        `type`: String,
                        status: String) extends AssetBC

object MoneyAssetBC {

  implicit val oldReads: Reads[MoneyAssetBC] = {
    (
      (__ \ "assetMoneyValue").read[String].map(_.toLong) and
        (__ \ "whatKindOfAsset").read[String] and
        (__ \ "status").read[String]
      )(MoneyAssetBC.apply _)
  }

  implicit val newWrites: Writes[MoneyAssetBC] = {
    (
      (__ \ "moneyValue").write[Long] and
        (__ \ "whatKindOfAsset").write[String] and
        (__ \ "status").write[String]
      )(unlift(MoneyAssetBC.unapply))
  }
}

case class SharesAssetBC(sharesInPortfolio: Boolean,
                         portfolioSharesName: Option[String],
                         portfolioSharesOnStockExchangeYesNo: Option[Boolean],
                         portfolioSharesQuantity: Option[Long],
                         portfolioSharesValue: Option[Long],
                         nonPortfolioSharesName:Option[String],
                         nonPortfolioSharesOnStockExchangeYesNo: Option[Boolean],
                         nonPortfolioSharesClass: Option[String],
                         nonPortfolioSharesQuantity: Option[Long],
                         nonPortfolioSharesValue: Option[Long],
                         `type`: String,
                         status: String) extends AssetBC

object SharesAssetBC {

  implicit class StringToLong(reads: Reads[Option[String]]) {
    def toLong: Reads[Option[Long]] = reads.map(_.map(_.toLong))
  }

  implicit val oldReads: Reads[SharesAssetBC] = {
    (
      (__ \ "sharesInAPortfolio").read[Boolean] and
        (__ \ "name").readNullable[String] and
        (__ \ "portfolioListedOnTheStockExchange").readNullable[Boolean] and
        (__ \ "portfolioQuantityInTheTrust").readNullable[String].toLong and
        (__ \ "portfolioValue").readNullable[String].toLong and
        (__ \ "shareCompanyName").readNullable[String] and
        (__ \ "listedOnTheStockExchange").readNullable[Boolean] and
        (__ \ "class").readNullable[String] and
        (__ \ "quantityInTheTrust").readNullable[String].toLong and
        (__ \ "value").readNullable[String].toLong and
        (__ \ "whatKindOfAsset").read[String] and
        (__ \ "status").read[String]
      )(SharesAssetBC.apply _)
  }

  implicit val newWrites: Writes[SharesAssetBC] = {
    (
      (__ \ "sharesInPortfolioYesNo").write[Boolean] and
        (__ \ "portfolioSharesName").writeNullable[String] and
        (__ \ "portfolioSharesOnStockExchangeYesNo").writeNullable[Boolean] and
        (__ \ "portfolioSharesQuantity").writeNullable[Long] and
        (__ \ "portfolioSharesValue").writeNullable[Long] and
        (__ \ "nonPortfolioSharesName").writeNullable[String] and
        (__ \ "nonPortfolioSharesOnStockExchangeYesNo").writeNullable[Boolean] and
        (__ \ "nonPortfolioSharesClass").writeNullable[String] and
        (__ \ "nonPortfolioSharesQuantity").writeNullable[Long] and
        (__ \ "nonPortfolioSharesValue").writeNullable[Long] and
        (__ \ "whatKindOfAsset").write[String] and
        (__ \ "status").write[String]
      )(unlift(SharesAssetBC.unapply))
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

case class BusinessAssetBC(name: String,
                           description: String,
                           hasUkAddress: Boolean,
                           ukAddress: Option[AddressTypeNoCountry],
                           internationalAddress: Option[AddressType],
                           value: Long,
                           `type`: String,
                           status: String) extends AssetBC

object BusinessAssetBC {

  implicit val oldReads: Reads[BusinessAssetBC] = {
    (
      (__ \ "name").read[String] and
        (__ \ "description").read[String] and
        (__ \ "addressUkYesNo").read[Boolean] and
        (__ \ "ukAddress").readNullable[AddressTypeNoCountry] and
        (__ \ "internationalAddress").readNullable[AddressType] and
        (__ \ "value").read[String].map(_.toLong) and
        (__ \ "whatKindOfAsset").read[String] and
        (__ \ "status").read[String]
      )(BusinessAssetBC.apply _)
  }

  implicit val newWrites: Writes[BusinessAssetBC] = {
    (
      (__ \ "businessName").write[String] and
        (__ \ "businessDescription").write[String] and
        (__ \ "businessAddressUkYesNo").write[Boolean] and
        (__ \ "businessUkAddress").writeNullable[AddressTypeNoCountry] and
        (__ \ "businessInternationalAddress").writeNullable[AddressType] and
        (__ \ "businessValue").write[Long] and
        (__ \ "whatKindOfAsset").write[String] and
        (__ \ "status").write[String]
      )(unlift(BusinessAssetBC.unapply))
  }
}

case class PartnershipAssetBC(partnershipDescription: String,
                              partnershipStartDate: LocalDate,
                              whatKindOfAsset: String,
                              status: String) extends AssetBC

// partnership keys haven't changed so only need formats
object PartnershipAssetBC {
  implicit val formats: Format[PartnershipAssetBC] = Json.format[PartnershipAssetBC]
}

case class OtherAssetBC(description: String,
                        value: Long,
                        `type`: String,
                        status: String) extends AssetBC

object OtherAssetBC {

  implicit val oldReads: Reads[OtherAssetBC] = {
    (
      (__ \ "otherAssetDescription").read[String] and
        (__ \ "otherAssetValue").read[String].map(_.toLong) and
        (__ \ "whatKindOfAsset").read[String] and
        (__ \ "status").read[String]
      )(OtherAssetBC.apply _)
  }

  implicit val newWrites: Writes[OtherAssetBC] = {
    (
      (__ \ "otherDescription").write[String] and
        (__ \ "otherValue").write[Long] and
        (__ \ "whatKindOfAsset").write[String] and
        (__ \ "status").write[String]
      )(unlift(OtherAssetBC.unapply))
  }
}
