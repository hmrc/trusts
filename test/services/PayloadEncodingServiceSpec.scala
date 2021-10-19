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

package services

import base.BaseSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import utils.JsonFixtures

class PayloadEncodingServiceSpec extends BaseSpec with JsonFixtures with Matchers {

  val service = new PayloadEncodingService

  ".encode" must {

    "return a base64 encoded string when given a Json payload" in {

      val payload = Json.toJson(registrationRequest)

      val result = service.encode(payload)

      result mustBe ""
    }
  }

  ".generateChecksum" must {

    "return a Sha256 checksum when given a Json payload" in {

      val payload = Json.toJson(registrationRequest)

      val result = service.generateChecksum(payload)

      result mustBe ""

    }
  }
}
