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

package transformers.assets

import play.api.libs.json.{Format, JsValue, Json}
import transformers.AddEntityTransform

case class AddAssetTransform(entity: JsValue,
                             `type`: String) extends AssetTransform with AddEntityTransform

object AddAssetTransform {

  val key = "AddAssetTransform"

  implicit val format: Format[AddAssetTransform] = Json.format[AddAssetTransform]

//  import reactivemongo.api.bson._
//
//  // Overrides BSONReaders for OID/Timestamp/DateTime
//  // so that the BSON representation matches the JSON lax one
//  implicit val laxBsonReader: BSONDocumentReader[AddAssetTransform] = {
//    Macros.reader[AddAssetTransform]
//  }
//  implicit val laxBsonWriter: BSONDocumentWriter[AddAssetTransform] = {
//    Macros.writer[AddAssetTransform]
//  }
//
//  implicit val laxJsonWrites: Writes[AddAssetTransform] = {
//    import reactivemongo.play.json.compat._, bson2json._, lax._
//    laxBsonWriter
//  }
//
//  implicit val laxJsonReads: Reads[AddAssetTransform] = {
//    import reactivemongo.play.json.compat._, bson2json._, lax._
//    laxBsonReader
//  }

//  implicit val format: Format[AddAssetTransform] = Format.apply(laxJsonReads, laxJsonWrites)
}
