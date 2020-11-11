/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import base.BaseSpec
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.IntegrationPatience
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.CONTENT_TYPE
import utils.WireMockHelper

class ConnectorSpecHelper extends BaseSpec with WireMockHelper with IntegrationPatience {

  override def applicationBuilder(): GuiceApplicationBuilder = {
    super.applicationBuilder()
      .configure(
        Seq(
          "microservice.services.des-trusts.port" -> server.port(),
          "microservice.services.des-display-trust-or-estate.port" -> server.port(),
          "microservice.services.des-vary-trust-or-estate.port" -> server.port(),
          "microservice.services.tax-enrolments.port" -> server.port(),
          "microservice.services.trusts-store.port" -> server.port()
        ): _*)
  }

  val jsonResponse400: JsValue = Json.parse(
    s"""
       |{
       | "code": "INVALID_PAYLOAD",
       | "reason": "Submission has not passed validation. Invalid Payload."
       |}""".stripMargin)

  val jsonResponse4005mld: JsValue = Json.parse(
    s"""
       |{
       |  "failures": [
       |    {
       |      "code" : "INVALID_IDTYPE",
       |      "reason" : "Submission has not passed validation. Invalid parameter idType."
       |    }
       |  ]
       |}
     """.stripMargin
  )

  val jsonResponseAlreadyRegistered: JsValue = Json.parse(
    s"""
       |{
       | "code": "ALREADY_REGISTERED",
       | "reason": "Trust/ Estate is already registered."
       |}""".stripMargin)

  val jsonResponse403NoMatch: JsValue = Json.parse(
    s"""
       |{
       | "code": "NO_MATCH",
       | "reason": "There is no match in HMRC records."
       |}""".stripMargin)

  val jsonResponse503: JsValue = Json.parse(
    s"""
       |{
       | "code": "SERVICE_UNAVAILABLE",
       | "reason": "Dependent systems are currently not responding"
       |}""".stripMargin)

  val jsonResponse500: JsValue = Json.parse(
    s"""
       |{
       | "code": "SERVER_ERROR",
       | "reason": "DES is currently experiencing problems that require live service intervention"
       |}""".stripMargin)

  //subscription id
  val jsonResponse400GetSubscriptionId: JsValue = Json.parse(
    s"""
       |{
       | "code": "INVALID_TRN",
       | "reason": "Submission has not passed validation. Invalid parameter TRN."
       |}""".stripMargin)

  val jsonResponse404GetSubscriptionId: JsValue = Json.parse(
    s"""
       |{
       | "code": "NOT_FOUND",
       | "reason": "The remote endpoint has indicated that no data can be found for given TRN."
       |}""".stripMargin)

  //get trust

  val jsonResponse400InvalidUTR: JsValue = Json.parse(
    s"""
       |{
       |  "code" : "INVALID_UTR",
       |  "reason" : "Submission has not passed validation. Invalid parameter UTR."
       |}
     """.stripMargin
  )


  val jsonResponse400InvalidRegime: JsValue = Json.parse(
    s"""
       |{
       |  "code" : "INVALID_REGIME",
       |  "reason" : "The remote endpoint has indicated that the REGIME provided is invalid."
       |}
     """.stripMargin
  )

  val jsonResponse409DuplicateCorrelation: JsValue = Json.parse(
    s"""
       |{
       | "code": "DUPLICATE_SUBMISSION",
       | "reason": "Duplicate Correlation Id was submitted."
       |}""".stripMargin)


  val jsonResponse400CorrelationId: JsValue = Json.parse(
    s"""
       |{
       | "code": "INVALID_CORRELATIONID",
       | "reason": "Submission has not passed validation. Invalid CorrelationId."
       |}""".stripMargin)

  val jsonResponse204: JsValue = Json.parse(
    s"""
       |{
       | "code": "NO_CONTENT",
       | "reason": "No Content."
       |}""".stripMargin)


  def stubForPost(server: WireMockServer,
                  url: String,
                  requestBody: String,
                  returnStatus: Int,
                  responseBody: String,
                  delayResponse: Int = 0) = {

    server.stubFor(post(urlEqualTo(url))
      .withHeader(CONTENT_TYPE, containing("application/json"))
      .withHeader("Environment", containing("dev"))
      .withRequestBody(equalTo(requestBody))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(responseBody).withFixedDelay(delayResponse)))
  }


  def stubForGet(server: WireMockServer,
                 url: String, returnStatus: Int,
                 responseBody: String,
                 delayResponse: Int = 0): StubMapping = {
    server.stubFor(get(urlEqualTo(url))
      .withHeader("content-Type", containing("application/json"))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(responseBody).withFixedDelay(delayResponse)))
  }

  def stubForHeaderlessGet(server: WireMockServer,
                 url: String, returnStatus: Int,
                 responseBody: String,
                 delayResponse: Int = 0): StubMapping = {
    server.stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(responseBody).withFixedDelay(delayResponse)))
  }

  def stubForPut(server: WireMockServer,
                 url: String,
                 returnStatus: Int,
                 delayResponse: Int = 0): StubMapping = {
    server.stubFor(put(urlEqualTo(url))
      .withHeader("content-Type", containing("application/json"))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withFixedDelay(delayResponse)))
  }


}
