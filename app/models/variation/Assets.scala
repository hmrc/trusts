/*
 * Copyright 2022 HM Revenue & Customs
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
import models.JsonWithoutNulls.JsonWithoutNullValues
import play.api.libs.json.{Format, Json, Writes}
import java.time.LocalDate

trait Asset[T] extends Entity[T]

case class Assets(monetary: Option[List[AssetMonetaryAmountType]],
                  propertyOrLand: Option[List[PropertyLandType]],
                  shares: Option[List[SharesType]],
                  business: Option[List[BusinessAssetType]],
                  partnerShip: Option[List[PartnershipType]],
                  other: Option[List[OtherAssetType]],
                  nonEEABusiness: Option[List[NonEEABusinessType]])

object Assets {
  implicit val assetsFormat: Format[Assets] = Json.format[Assets]
}

case class AssetMonetaryAmountType(assetMonetaryAmount: Long) extends Asset[AssetMonetaryAmountType] {
  override val writeToMaintain: Writes[AssetMonetaryAmountType] = AssetMonetaryAmountType.assetMonetaryAmountFormat
}

object AssetMonetaryAmountType {
  implicit val assetMonetaryAmountFormat: Format[AssetMonetaryAmountType] = Json.format[AssetMonetaryAmountType]
}

case class PropertyLandType(buildingLandName: Option[String],
                            address: Option[AddressType],
                            valueFull: Long,
                            valuePrevious: Option[Long]) extends Asset[PropertyLandType] {

  override val writeToMaintain: Writes[PropertyLandType] = PropertyLandType.propertyLandTypeFormat
}

object PropertyLandType {
  implicit val propertyLandTypeFormat: Format[PropertyLandType] = Json.format[PropertyLandType]
}

case class SharesType(numberOfShares: Option[String],
                      orgName: String,
                      utr: Option[String],
                      shareClass: Option[String],
                      typeOfShare: Option[String],
                      value: Option[Long],
                      isPortfolio: Option[Boolean] = None,
                      shareClassDisplay: Option[String] = None) extends Asset[SharesType] {

  override val writeToMaintain: Writes[SharesType] = SharesType.sharesTypeFormat
}

object SharesType {
  implicit val sharesTypeFormat: Format[SharesType] = Json.format[SharesType]
}

case class BusinessAssetType(utr: Option[String],
                             orgName: String,
                             businessDescription: String,
                             address: Option[AddressType],
                             businessValue: Option[Long]) extends Asset[BusinessAssetType]{

  override val writeToMaintain: Writes[BusinessAssetType] = BusinessAssetType.businessAssetTypeFormat
}

object BusinessAssetType {
  implicit val businessAssetTypeFormat: Format[BusinessAssetType] = Json.format[BusinessAssetType]
}

case class PartnershipType(utr: Option[String],
                           description: String,
                           partnershipStart: Option[LocalDate]) extends Asset[PartnershipType] {

  override val writeToMaintain: Writes[PartnershipType] = PartnershipType.partnershipTypeFormat
}

object PartnershipType {
  implicit val partnershipTypeFormat: Format[PartnershipType] = Json.format[PartnershipType]
}

case class OtherAssetType(description: String,
                          value: Option[Long]) extends Asset[OtherAssetType] {

  override val writeToMaintain: Writes[OtherAssetType] = OtherAssetType.otherAssetTypeFormat
}

object OtherAssetType {
  implicit val otherAssetTypeFormat: Format[OtherAssetType] = Json.format[OtherAssetType]
}

case class NonEEABusinessType(lineNo: Option[String],
                              orgName: String,
                              address: AddressType,
                              govLawCountry: String,
                              startDate: LocalDate,
                              endDate: Option[LocalDate]) extends Asset[NonEEABusinessType] {

  override val writeToMaintain: Writes[NonEEABusinessType] = NonEEABusinessType.writeToMaintain
}

object NonEEABusinessType {
  implicit val nonEEABusinessTypeFormat: Format[NonEEABusinessType] = Json.format[NonEEABusinessType]

  val writeToMaintain: Writes[NonEEABusinessType] = (o: NonEEABusinessType) => Json.obj(
    "lineNo" -> o.lineNo,
    "orgName" -> o.orgName,
    "address" -> o.address,
    "govLawCountry" -> o.govLawCountry,
    "startDate" -> o.startDate,
    "endDate" -> o.endDate,
    "provisional" -> o.lineNo.isEmpty
  ).withoutNulls
}

