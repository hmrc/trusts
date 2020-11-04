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

package services

import javax.inject.Inject
import models.Success
import transformers.trustDetails._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustDetailsTransformationService @Inject()(transformationService: TransformationService) {

  def setExpressTransformer(utr: String, internalId: String, express: Boolean): Future[Success.type] = {
    transformationService.addNewTransform(
      utr,
      internalId,
      SetExpressTransform(express)
    ).map(_ => Success)
  }

  def setResidentTransformer(utr: String, internalId: String, resident: Boolean): Future[Success.type] = {
    transformationService.addNewTransform(
      utr,
      internalId,
      SetResidentTransform(resident)
    ).map(_ => Success)
  }

  def setTaxableTransformer(utr: String, internalId: String, taxable: Boolean): Future[Success.type] = {
    transformationService.addNewTransform(
      utr,
      internalId,
      SetTaxableTransform(taxable)
    ).map(_ => Success)
  }

  def setPropertyTransformer(utr: String, internalId: String, property: Boolean): Future[Success.type] = {
    transformationService.addNewTransform(
      utr,
      internalId,
      SetPropertyTransform(property)
    ).map(_ => Success)
  }

  def setRecordedTransformer(utr: String, internalId: String, recorded: Boolean): Future[Success.type] = {
    transformationService.addNewTransform(
      utr,
      internalId,
      SetRecordedTransform(recorded)
    ).map(_ => Success)
  }

  def setUKRelationTransformer(utr: String, internalId: String, ukRelation: Boolean): Future[Success.type] = {
    transformationService.addNewTransform(
      utr,
      internalId,
      SetUKRelationTransform(ukRelation)
    ).map(_ => Success)
  }

}
