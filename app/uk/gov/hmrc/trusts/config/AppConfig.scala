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

package uk.gov.hmrc.trusts.config

import javax.inject.{Inject, Singleton}

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig


@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment) extends ServicesConfig {

  override protected def mode: Mode = playEnv.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(
    throw new Exception(s"Missing configuration key : $key")
  )

  val desTrustsUrl : String = baseUrl("des-trusts")
  val desEstatesUrl : String = baseUrl("des-estates")

  val getTrustOrEstateUrl : String = baseUrl("des-display-trust-or-estate")

  val varyTrustOrEstateUrl : String = baseUrl("des-vary-trust-or-estate")

  val trustsStoreUrl : String = baseUrl("trusts-store")

  val desEnvironment : String = loadConfig("microservice.services.des-trusts.environment")
  val desToken : String = loadConfig("microservice.services.des-trusts.token")

  val trustsApiRegistrationSchema4MLD : String  = "/resources/schemas/4MLD/trusts-api-registration-schema-5.0.0.json"
  val trustsApiRegistrationSchema5MLD : String  = "/resources/schemas/5MLD/trusts-api-registration-schema-1.3.0.json"
  val estatesApiRegistrationSchema : String  = "/resources/schemas/estates-api-schema-5.0.json"
  val variationsApiSchema: String = "/resources/schemas/variations-api-schema-4.0.json"

  val taxEnrolmentsUrl : String = baseUrl("tax-enrolments")
  val taxEnrolmentsPayloadBodyServiceName : String = loadConfig("microservice.services.tax-enrolments.serviceName")
  val taxEnrolmentsPayloadBodyCallback : String = loadConfig("microservice.services.tax-enrolments.callback")
  val delayToConnectTaxEnrolment : Int = loadConfig("microservice.services.trusts.delayToConnectTaxEnrolment").toInt
  val maxRetry : Int = loadConfig("microservice.services.trusts.maxRetry").toInt

  val auditingEnabled : Boolean = loadConfig("microservice.services.trusts.features.auditing.enabled").toBoolean

  val ttlInSeconds: Int = runModeConfiguration.getInt("mongodb.ttlSeconds").getOrElse(4*60*60)
  val registrationTtlInSeconds: Int = runModeConfiguration.getInt("mongodb.registration.ttlSeconds").getOrElse(28*24*60*60)
}

