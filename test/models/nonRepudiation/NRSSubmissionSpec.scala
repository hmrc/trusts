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

package models.nonRepudiation

import base.BaseSpec
import play.api.libs.json.Json
import org.scalatest.matchers.must.Matchers._

import java.time.LocalDateTime

class NRSSubmissionSpec extends BaseSpec {

  "NRSSubmission" must {

    "write to json for a registration" in {
      val payLoad = NRSSubmission(
        "payload",
        MetaData(businessId = "trs",
          notableEvent = "trs-registration",
          payloadContentType = "application/json",
          payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          userSubmissionTimestamp = LocalDateTime.of(2020, 10, 18, 14, 10, 0, 0),
          identityData = Json.obj("internalId" -> "some-id"),
          userAuthToken = "AbCdEf123456",
          headerData = Json.obj(
            "Gov-Client-Public-IP" -> "198.51.100.0",
            "Gov-Client-Public-Port" -> "12345"
          ),
          searchKeys = SearchKeys(SearchKey.TRN, "ABTRUST123456789")
        ))

      Json.toJson(payLoad) mustBe Json.parse(
        """
          |{
          | "payload": "payload",
          | "metadata": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-registration",
          |   "payloadContentType": "application/json",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2020-10-18T14:10:00.000Z",
          |   "identityData": {
          |     "internalId": "some-id"
          |   },
          |   "userAuthToken": "AbCdEf123456",
          |   "headerData": {
          |     "Gov-Client-Public-IP": "198.51.100.0",
          |     "Gov-Client-Public-Port": "12345"
          |   },
          |   "searchKeys": {
          |     "trn": "ABTRUST123456789"
          |   }
          | }
          |}
          |""".stripMargin)
    }

    "write to json for a taxable variation" in {
      val payLoad = NRSSubmission(
        "payload",
        MetaData(businessId = "trs",
          notableEvent = "trs-taxable-update",
          payloadContentType = "application/json",
          payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          userSubmissionTimestamp = LocalDateTime.of(2020, 10, 18, 14, 10, 0, 0),
          identityData = Json.obj("internalId" -> "some-id"),
          userAuthToken = "AbCdEf123456",
          headerData = Json.obj(
            "Gov-Client-Public-IP" -> "198.51.100.0",
            "Gov-Client-Public-Port" -> "12345"
          ),
          searchKeys = SearchKeys(SearchKey.UTR, "1234567890")
        ))

      Json.toJson(payLoad) mustBe Json.parse(
        """
          |{
          | "payload": "payload",
          | "metadata": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-taxable-update",
          |   "payloadContentType": "application/json",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2020-10-18T14:10:00.000Z",
          |   "identityData": {
          |     "internalId": "some-id"
          |   },
          |   "userAuthToken": "AbCdEf123456",
          |   "headerData": {
          |     "Gov-Client-Public-IP": "198.51.100.0",
          |     "Gov-Client-Public-Port": "12345"
          |   },
          |   "searchKeys": {
          |     "utr": "1234567890"
          |   }
          | }
          |}
          |""".stripMargin)
    }

    "write to json for a non-taxable variation" in {
      val payLoad = NRSSubmission(
        "payload",
        MetaData(businessId = "trs",
          notableEvent = "trs-non-taxable-update",
          payloadContentType = "application/json",
          payloadSha256Checksum = "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          userSubmissionTimestamp = LocalDateTime.of(2020, 10, 18, 14, 10, 0, 0),
          identityData = Json.obj("internalId" -> "some-id"),
          userAuthToken = "AbCdEf123456",
          headerData = Json.obj(
            "Gov-Client-Public-IP" -> "198.51.100.0",
            "Gov-Client-Public-Port" -> "12345"
          ),
          searchKeys = SearchKeys(SearchKey.URN, "ABTRUST12345678")
        ))

      Json.toJson(payLoad) mustBe Json.parse(
        """
          |{
          | "payload": "payload",
          | "metadata": {
          |   "businessId": "trs",
          |   "notableEvent": "trs-non-taxable-update",
          |   "payloadContentType": "application/json",
          |   "payloadSha256Checksum": "1cbdeb2d2b003b4d4d639af4bd2e1913f591f74c33940d97fd6a626161c20b67",
          |   "userSubmissionTimestamp": "2020-10-18T14:10:00.000Z",
          |   "identityData": {
          |     "internalId": "some-id"
          |   },
          |   "userAuthToken": "AbCdEf123456",
          |   "headerData": {
          |     "Gov-Client-Public-IP": "198.51.100.0",
          |     "Gov-Client-Public-Port": "12345"
          |   },
          |   "searchKeys": {
          |     "urn": "ABTRUST12345678"
          |   }
          | }
          |}
          |""".stripMargin)
    }

  }

}
