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

package controllers.transformations

import controllers.TrustsBaseController
import controllers.actions.IdentifierAction
import controllers.transformations.TransformationHelper.isTrustTaxable
import errors.ServerError
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.{TaxableMigrationService, TransformationService}
import transformers.DeltaTransform
import utils.TrustEnvelope.TrustEnvelope
import utils.{Session, TrustEnvelope}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

abstract class AddTransformationController @Inject()(identify: IdentifierAction,
                                                     transformationService: TransformationService,
                                                     taxableMigrationService: TaxableMigrationService)
                                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with TransformationHelper with Logging {

  private val className = this.getClass.getSimpleName

  def transform[T](value: T, `type`: String, isTaxable: Boolean, migratingFromNonTaxableToTaxable: Boolean)
                  (implicit wts: Writes[T]): DeltaTransform

  def addNewTransform[T](identifier: String, `type`: String = "", addMultipleTransforms: Boolean = false, index: Option[Int] = None)
                        (implicit rds: Reads[T], wts: Writes[T]): Action[JsValue] = {
    identify.async(parse.json) {
      implicit request => {
        request.body.validate[T] match {

          case JsSuccess(entityToAdd, _) =>
            println("=========================== JsSuccess =================================" + entityToAdd + "=========================== JsSuccess =================================")
            val expectedResult = for {
              trust <- transformationService.getTransformedTrustJson(identifier, request.internalId, Session.id(hc))
              isTaxable <- isTrustTaxable(trust)
//              originalEntity <- findJson(trust, `type`, index)
              migratingFromNonTaxableToTaxable <- taxableMigrationService.migratingFromNonTaxableToTaxable(identifier, request.internalId, Session.id(hc))
              _ <- addTransformOrTransforms(entityToAdd, identifier, `type`, isTaxable, migratingFromNonTaxableToTaxable, addMultipleTransforms)
            } yield {
              Ok
            }

            expectedResult.value.map {
              case Right(status) => status
              case Left(ServerError(message)) if message.nonEmpty =>
                logger.warn(s"[$className][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
                  s"Failed to add new transform. Message: $message")
                InternalServerError
              case Left(_) =>
                logger.warn(s"[$className][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
                  s"Failed to add new transform")
                InternalServerError
            }

          case JsError(errors) =>
            logger.warn(s"[$className][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
              s"Supplied json did not pass validation - $errors")
            Future.successful(BadRequest)
        }
      }
    }
  }

  private def addTransformOrTransforms[A](entityToAdd: A,
                                          identifier: String,
                                          `type`: String,
                                          isTaxable: Boolean,
                                          migratingFromNonTaxableToTaxable: Boolean,
                                          addMultipleTransforms: Boolean)
                                         (implicit wts: Writes[A], request: IdentifierRequest[JsValue]): TrustEnvelope[Boolean] = {
println("=============================" + entityToAdd + "=============================")
    println("=========================== addTransformOrTransforms method =================================")
    def addTransform[B](value: B, `type`: String)(implicit wts: Writes[B]): TrustEnvelope[Boolean] = {
      transformationService.addNewTransform(
        identifier = identifier,
        internalId = request.internalId,
        newTransform = transform(value, `type`, isTaxable, migratingFromNonTaxableToTaxable)
      )
    }

    if (addMultipleTransforms) {
      Json.toJson(entityToAdd).as[JsObject].fields.foldLeft(TrustEnvelope(true))((x, field) => {
        x.flatMap(_ => addTransform(field._2, field._1))
      })
    } else {
      addTransform(entityToAdd, `type`)
    }
  }

}
