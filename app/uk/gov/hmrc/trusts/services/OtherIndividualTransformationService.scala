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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustNaturalPersonType
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.{ExecutionContext, Future}

class OtherIndividualTransformationService @Inject()(
                                                  transformationService: TransformationService,
                                                  localDateService: LocalDateService
                                                )
                                                    (implicit ec:ExecutionContext)
  extends JsonOperations {

  def addOtherIndividualTransformer(utr: String, internalId: String, newOtherIndividual: DisplayTrustNaturalPersonType): Future[Boolean] = {
    transformationService.addNewTransform(utr, internalId, AddOtherIndividualTransform(newOtherIndividual))
  }

}
