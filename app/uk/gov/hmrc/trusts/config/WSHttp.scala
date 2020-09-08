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
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.{HttpHook, HttpHooks}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws._


@Singleton
class WSHttp @Inject()(
                        val conf: Configuration, val environment: Environment,
                        override val auditConnector: MicroserviceAuditConnector
                      )
  extends WSGet with HttpGet
    with WSPut with HttpPut
    with WSPost with HttpPost
    with WSDelete with HttpDelete
    with AppName with RunMode
    with HttpHooks with HttpAuditing {

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = conf

  override protected def appNameConfiguration: Configuration = conf

  override protected def actorSystem: akka.actor.ActorSystem = Play.current.actorSystem
  override protected def configuration: Option[com.typesafe.config.Config] = Some(Play.current.configuration.underlying)



}






