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

package config

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

@Singleton
class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) extends Logging {

  val registrationBaseUrl: String       = servicesConfig.baseUrl("registration")
  val subscriptionBaseUrl: String       = servicesConfig.baseUrl("subscription")
  val taxEnrolmentsUrl: String          = servicesConfig.baseUrl("tax-enrolments")
  val taxEnrolmentsMigrationUrl: String = servicesConfig.baseUrl("tax-enrolments-migration")
  val getTrustOrEstateUrl: String       = servicesConfig.baseUrl("playback")
  val varyTrustOrEstateUrl: String      = servicesConfig.baseUrl("variation")
  val orchestratorUrl: String           = servicesConfig.baseUrl("orchestrator")
  val nonRepudiationUrl: String         = s"${servicesConfig.baseUrl("non-repudiation")}/nrs-orchestrator/submission"

  val registrationEnvironment: String = configuration.get[String]("microservice.services.registration.environment")
  val registrationToken: String       = configuration.get[String]("microservice.services.registration.token")

  val subscriptionEnvironment: String = configuration.get[String]("microservice.services.subscription.environment")
  val subscriptionToken: String       = configuration.get[String]("microservice.services.subscription.token")

  val trustsApiRegistrationSchema5MLD: String =
    "/resources/schemas/5MLD/trusts-api-registration-schema-1.4.2.json"

  val variationsApiSchema5MLD: String =
    "/resources/schemas/5MLD/variations-api-schema-4.8.1.json"

  private def insertTRN(url: String, trn: String) = url.replace(":trn", trn)

  val taxEnrolmentsPayloadBodyServiceNameTaxable: String =
    configuration.get[String]("microservice.services.tax-enrolments.taxable.serviceName")

  private val taxEnrolmentsPayloadBodyCallbackTaxableTemplate: String =
    configuration.get[String]("microservice.services.tax-enrolments.taxable.callback")

  def taxEnrolmentsPayloadBodyCallbackTaxable(trn: String): String =
    insertTRN(taxEnrolmentsPayloadBodyCallbackTaxableTemplate, trn)

  val taxEnrolmentsPayloadBodyServiceNameNonTaxable: String =
    configuration.get[String]("microservice.services.tax-enrolments.non-taxable.serviceName")

  private val taxEnrolmentsPayloadBodyCallbackNonTaxableTemplate: String =
    configuration.get[String]("microservice.services.tax-enrolments.non-taxable.callback")

  def taxEnrolmentsPayloadBodyCallbackNonTaxable(trn: String): String =
    insertTRN(taxEnrolmentsPayloadBodyCallbackNonTaxableTemplate, trn)

  val taxEnrolmentsMigrationPayloadServiceName: String =
    configuration.get[String]("microservice.services.tax-enrolments-migration.to-taxable.serviceName")

  private val taxEnrolmentsMigrationPayloadCallbackTemplate: String =
    configuration.get[String]("microservice.services.tax-enrolments-migration.to-taxable.callback")

  def taxEnrolmentsMigrationPayloadBodyCallback(subscriptionId: String, urn: String): String =
    taxEnrolmentsMigrationPayloadCallbackTemplate
      .replace(":urn", urn)
      .replace(":subscriptionId", subscriptionId)

  val delayToConnectTaxEnrolment: Int =
    configuration.get[String]("microservice.services.trusts.delayToConnectTaxEnrolment").toInt

  val maxRetry: Int = configuration.get[Int]("microservice.services.trusts.maxRetry")

  val ttlInSeconds: Int = configuration.get[Int]("mongodb.ttlSeconds")

  val dropIndexesEnabled: Boolean = configuration.getOptional[Boolean]("features.mongo.dropIndexes").getOrElse(false)

  val registrationTtlInSeconds: Int = configuration.get[Int]("mongodb.registration.ttlSeconds")

  val removeSavedRegistrations: Boolean = configuration.get[Boolean]("features.removeSavedRegistrations")

  val xApiKey = configuration.get[String]("microservice.services.non-repudiation.token")

  val nonRepudiate: Boolean = configuration.get[Boolean]("features.nonRepudiate")

  val nrsRetryWaitMs     = configuration.get[Int]("nrs-orchestrator.retryWaitMs")
  val nrsRetryWaitFactor = configuration.get[Int]("nrs-orchestrator.retryWaitFactor")
  val nrsTotalAttempts   = configuration.get[Int]("nrs-orchestrator.totalAttempts")

  val registrationValidationInterval: FiniteDuration = Duration.apply(
    configuration.get[Int]("mongodb.registration.validation.interval"),
    TimeUnit.MINUTES
  )

  logger.info(s"""=============== FEATURE FLAGS ===============
                 |            nonRepudiate = $nonRepudiate
                 |      dropIndexesEnabled = $dropIndexesEnabled
                 |removeSavedRegistrations = $removeSavedRegistrations
                 |=============== ============= ===============""".stripMargin)

}
