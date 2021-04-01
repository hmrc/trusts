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

package models.variation

import models.AddressType
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class Assets(monetary: Option[List[AssetMonetaryAmount]],
                  propertyOrLand: Option[List[PropertyLandType]],
                  shares: Option[List[SharesType]],
                  business: Option[List[BusinessAssetType]],
                  partnerShip: Option[List[PartnershipType]],
                  other: Option[List[OtherAssetType]],
                  nonEEABusiness: Option[List[NonEEABusinessType]])

object Assets {
  implicit val assetsFormat: Format[Assets] = Json.format[Assets]
}

case class AssetMonetaryAmount(assetMonetaryAmount: Long)

object AssetMonetaryAmount {
  implicit val assetMonetaryAmountFormat: Format[AssetMonetaryAmount] = Json.format[AssetMonetaryAmount]
}

case class PropertyLandType(buildingLandName: Option[String],
                            address: Option[AddressType],
                            valueFull: Long,
                            valuePrevious: Option[Long])

object PropertyLandType {
  implicit val propertyLandTypeFormat: Format[PropertyLandType] = Json.format[PropertyLandType]
}

case class SharesType(numberOfShares: Option[String],
                      orgName: String,
                      utr: Option[String],
                      shareClass: Option[String],
                      typeOfShare: Option[String],
                      value: Option[Long])

object SharesType {
  implicit val sharesTypeFormat: Format[SharesType] = Json.format[SharesType]
}

case class BusinessAssetType(utr: Option[String],
                             orgName: String,
                             businessDescription: String,
                             address: Option[AddressType],
                             businessValue: Option[Long])

object BusinessAssetType {
  implicit val businessAssetTypeFormat: Format[BusinessAssetType] = Json.format[BusinessAssetType]
}

case class PartnershipType(utr: Option[String],
                           description: String,
                           partnershipStart: Option[LocalDate])

object PartnershipType {
  implicit val partnershipTypeFormat: Format[PartnershipType] = Json.format[PartnershipType]
}

case class OtherAssetType(description: String,
                          value: Option[Long])

object OtherAssetType {
  implicit val otherAssetTypeFormat: Format[OtherAssetType] = Json.format[OtherAssetType]
}

case class NonEEABusinessType(lineNo: Option[String],
                              orgName: String,
                              address: AddressType,
                              govLawCountry: String,
                              startDate: LocalDate,
                              endDate: Option[LocalDate])

object NonEEABusinessType {
  implicit val format: Format[NonEEABusinessType] = Json.format[NonEEABusinessType]
}
