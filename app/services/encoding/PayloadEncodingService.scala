/*
 * Copyright 2024 HM Revenue & Customs
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

package services.encoding

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.json.{JsValue, Json}

import java.security.MessageDigest
import javax.inject.Singleton

@Singleton
class PayloadEncodingService {

  def encode(payload: JsValue): String =
    new String(Base64.encodeBase64(Json.toBytes(payload)))

  def generateChecksum(payload: JsValue): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(Json.stringify(payload).getBytes("UTF-8"))
    hash.map("%02x".format(_)).mkString
  }


}
