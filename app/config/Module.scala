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

package config

import com.google.inject.AbstractModule
import connector.{TaxEnrolmentConnector, TaxEnrolmentConnectorImpl}
import controllers.actions.{AuthenticatedIdentifierAction, IdentifierAction}
import repositories._
import retry.{NrsRetryHelper, RetryHelper}
import services.rosm.{RosmPatternService, RosmPatternServiceImpl, TaxEnrolmentsService, TaxEnrolmentsServiceImpl}

class Module extends AbstractModule {

  override def configure(): Unit = {
    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()

    bind(classOf[TransformationRepository]).to(classOf[TransformationRepositoryImpl]).asEagerSingleton()
    bind(classOf[CacheRepository]).to(classOf[CacheRepositoryImpl]).asEagerSingleton()
    bind(classOf[RegistrationSubmissionRepository]).to(classOf[RegistrationSubmissionRepositoryImpl]).asEagerSingleton()
    bind(classOf[TaxableMigrationRepository]).to(classOf[TaxableMigrationRepositoryImpl]).asEagerSingleton()

    bind(classOf[TaxEnrolmentConnector]).to(classOf[TaxEnrolmentConnectorImpl]).asEagerSingleton()
    bind(classOf[TaxEnrolmentsService]).to(classOf[TaxEnrolmentsServiceImpl]).asEagerSingleton()
    bind(classOf[RosmPatternService]).to(classOf[RosmPatternServiceImpl]).asEagerSingleton()

    bind(classOf[RetryHelper]).to(classOf[NrsRetryHelper]).asEagerSingleton()
  }
}
