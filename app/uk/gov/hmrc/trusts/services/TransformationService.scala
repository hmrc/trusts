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
import play.api.libs.json.JsValue
import play.mvc.BodyParser.Json
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustLeadTrusteeType
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers.{ComposedDeltaTransform, DeltaTransform, SetLeadTrusteeIndTransform}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TransformationService @Inject()(repository: TransformationRepository){
  def applyTransformations(utr: String, internalId: String, json: JsValue): Future[JsValue] = {
    repository.get(utr, internalId).map {
      case None => json
      case Some(transformations) => transformations.applyTransform(json)
    }
  }

  def addAmendLeadTrustee(utr: String, internalId: String, newLeadTrustee: DisplayTrustLeadTrusteeType): Future[Unit] = {
    addNewTransform(utr, internalId, newLeadTrustee match {
      case DisplayTrustLeadTrusteeType(Some(trusteeInd), None) => SetLeadTrusteeIndTransform(trusteeInd)
    })
  }

  private def addNewTransform(utr: String, internalId: String, newTransform: DeltaTransform) = {
    repository.get(utr, internalId).map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    }.flatMap(newTransforms => repository.set(utr, internalId, newTransforms).map(_ => ()))
  }
}
