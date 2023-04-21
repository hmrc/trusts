/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import errors.ServerError
import models.auditing.TrustAuditing
import models.get_trust.{GetTrustResponse, TrustProcessedResponse}
import play.api.Logging
import play.api.libs.json._
import repositories.TransformationRepository
import services.auditing.AuditService
import transformers._
import transformers.beneficiaries.{AddBeneficiaryTransform, AmendBeneficiaryTransform}
import transformers.settlors.{AddSettlorTransform, AmendSettlorTransform}
import transformers.trustdetails.SetTrustDetailTransform
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
import utils.Session
import utils.TrustEnvelope.TrustEnvelope

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransformationService @Inject()(repository: TransformationRepository,
                                      trustsService: TrustsService,
                                      auditService: AuditService)(implicit ec: ExecutionContext) extends Logging {

  private val className = this.getClass.getSimpleName

  def getTransformedTrustJson(identifier: String, internalId: String, sessionId: String)
                             (implicit hc: HeaderCarrier): TrustEnvelope[JsObject] = EitherT {
    getTransformedData(identifier, internalId, sessionId).value.flatMap {
      case Right(TrustProcessedResponse(json, _)) => Future.successful(Right(json.as[JsObject]))
      case Right(_) =>
        Future.successful(Left(ServerError("Trust is not in a processed state.")))
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][getTransformedTrustJson][SessionId: $sessionId] failed to get transformed trust data - $message")
        Future.successful(Left(ServerError()))
      case Left(_) =>
        logger.warn(s"[$className][getTransformedTrustJson][SessionId: $sessionId] failed to get transformed trust data.")
        Future.successful(Left(ServerError()))
    }
  }

  def getTransformedData(identifier: String, internalId: String, sessionId: String)
                        (implicit hc: HeaderCarrier): TrustEnvelope[GetTrustResponse] = EitherT {
    trustsService.getTrustInfo(identifier, internalId, sessionId).value.flatMap {
      case Right(response: TrustProcessedResponse) =>
        populateLeadTrusteeAddress(response.getTrust) match {
          case JsSuccess(fixed, _) =>

            applyTransformations(identifier, internalId, fixed).value.map {
              case Right(JsSuccess(transformed, _)) =>
                Right(TrustProcessedResponse(transformed, response.responseHeader))
              case Right(JsError(errors)) =>
                logger.warn(s"[$className][getTransformedData][SessionId: $sessionId] failed to apply transformations - $errors")
                Left(ServerError(errors.toString))
              case Left(trustErrors) => Left(trustErrors)
            }
          case JsError(errors) =>
            logger.warn(s"[$className][getTransformedData][SessionId: $sessionId] cannot populate lead trustee address - $errors")
            Future.successful(Left(ServerError(errors.toString)))
        }
      case Right(response) => Future.successful(Right(response))
      case Left(ServerError(message)) if message.nonEmpty =>
        logger.warn(s"[$className][getTransformedData][SessionId: $sessionId] failed to get trust info - $message")
        Future.successful(Left(ServerError()))
      case Left(_) =>
        logger.warn(s"[$className][getTransformedData][SessionId: $sessionId] failed to get trust info.")
        Future.successful(Left(ServerError()))
    }
  }

  private def applyTransformations(identifier: String, internalId: String, json: JsValue)
                                  (implicit hc: HeaderCarrier): TrustEnvelope[JsResult[JsValue]] = EitherT {
    repository.get(identifier, internalId, Session.id(hc)).value.map {
      case Right(None) =>
        Right(JsSuccess(json))
      case Right(Some(transformations)) =>
        Right(transformations.applyTransform(json))
      case Left(trustErrors) => Left(trustErrors)
    }
  }

  def applyDeclarationTransformations(identifier: String, internalId: String, json: JsValue)
                                     (implicit hc: HeaderCarrier): TrustEnvelope[JsResult[JsValue]] = EitherT {
    repository.get(identifier, internalId, Session.id(hc)).value.map {
      case Right(None) =>
        logger.info(s"[$className][applyDeclarationTransformations][Session ID: ${Session.id(hc)}]" +
          s" no transformations to apply")
        Right(JsSuccess(json))
      case Right(Some(transformations)) =>

        auditService.audit(
          event = TrustAuditing.TRUST_TRANSFORMATIONS,
          request = Json.toJson(Json.obj()),
          internalId = internalId,
          response = Json.obj(
            "transformations" -> transformations,
            "data" -> json
          )
        )

        val expectedTransformedResult = for {
          initial <- {
            logger.info(s"[$className][applyDeclarationTransformations][Session ID: ${Session.id(hc)}]" +
              s" applying transformations")
            transformations.applyTransform(json)
          }
          transformed <- {
            logger.info(s"[$className][applyDeclarationTransformations][Session ID: ${Session.id(hc)}]" +
              s" applying declaration transformations")
            transformations.applyDeclarationTransform(initial)
          }
        } yield transformed

        Right(expectedTransformedResult)

      case Left(trustErrors) => Left(trustErrors)
    }
  }


  def populateLeadTrusteeAddress(beforeJson: JsValue): JsResult[JsValue] = {
    val pathToLeadTrusteeAddress = __ \ Symbol("details") \ Symbol("trust") \ Symbol("entities") \
      Symbol("leadTrustees") \ Symbol("identification") \ Symbol("address")
    if (beforeJson.transform(pathToLeadTrusteeAddress.json.pick).isSuccess) {
      JsSuccess(beforeJson)
    } else {
      val pathToCorrespondenceAddress = __ \ Symbol("correspondence") \ Symbol("address")
      val copyAddress = __.json.update(pathToLeadTrusteeAddress.json.copyFrom(pathToCorrespondenceAddress.json.pick))
      beforeJson.transform(copyAddress)
    }
  }

  def addNewTransform(identifier: String, internalId: String, newTransform: DeltaTransform)
                     (implicit hc: HeaderCarrier): TrustEnvelope[Boolean] = EitherT {
    repository.get(identifier, internalId, Session.id(hc)).value.map {
      case Right(None) => Right(ComposedDeltaTransform(Seq(newTransform)))
      case Right(Some(composedTransform)) => Right(composedTransform :+ newTransform)
      case Left(_) =>
        logger.warn(s"[$className][addNewTransform] Exception from mongo, failed to get data from repository.")
        Left(ServerError())
    }.flatMap {
      case Left(trustErrors) => Future.successful(Left(trustErrors))
      case Right(newTransforms) =>
          repository.set(identifier, internalId, Session.id(hc), newTransforms).value.flatMap {
            case Right(value) => Future.successful(Right(value))
            case Left(_) =>
              logger.warn(s"[$className][addNewTransform] Exception from mongo, failed to add new transform.")
              Future.successful(Left(ServerError()))
          }
    }
  }

  def removeAllTransformations(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Boolean] = EitherT {
    repository.resetCache(identifier, internalId, sessionId).value.map {
      case Left(_) =>
        logger.warn(s"[$className][removeAllTransformations][SessionId: $sessionId] failed to remove transform.")
        Left(ServerError())
      case Right(value) => Right(value)
    }
  }

  def removeTrustTypeDependentTransformFields(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Boolean] = EitherT {
    val expectedResult = for {
      transforms <- repository.get(identifier, internalId, sessionId)
      updatedTransforms = handleTransformsForRemoveTrustTypeDependent(transforms)
      result <- repository.set(identifier, internalId, sessionId, updatedTransforms)
    } yield {
      result
    }

    expectedResult.value.map {
      case Right(result) => Right(result)
      case Left(_) =>
        logger.warn(s"[$className][removeTrustTypeDependentTransformFields][SessionId: $sessionId] " +
          s"an error occurred, failed to transform json and remove fields.")
        Left(ServerError())
    }
  }

  private def handleTransformsForRemoveTrustTypeDependent(transforms: Option[ComposedDeltaTransform]): ComposedDeltaTransform = {
    transforms match {
      case Some(value) => ComposedDeltaTransform(value.deltaTransforms.map {
        case x: AddBeneficiaryTransform => x.copy(entity = x.removeTrustTypeDependentFields(x.entity))
        case x: AmendBeneficiaryTransform => x.copy(amended = x.removeTrustTypeDependentFields(x.amended))
        case x: AddSettlorTransform => x.copy(entity = x.removeTrustTypeDependentFields(x.entity))
        case x: AmendSettlorTransform => x.copy(amended = x.removeTrustTypeDependentFields(x.amended))
        case x => x
      })
      case None => ComposedDeltaTransform()
    }
  }

  /**
   * Need to remove any <i>SetTrustDetailTransform</i> transforms corresponding to optional trust detail fields
   * before we call <i>setMigratingTrustDetails</i> from the frontend. This is done to avoid a few scenarios:
   * <ol>
   *   <li>so we don't end up with transforms for setting more than one of <i>deedOfVariation</i>, <i>interVivos</i>, and
   *  <i>efrbsStartDate</i>, as these fields are mutually exclusive</li>
   *  <li>so we don't end up with a transform for <i>lawCountry</i> if the user changes their answer to the 'governed by UK law'
   *  question from 'No' to 'Yes'</li>
   *  <li>so we don't end up with a transform for <i>trustUKRelation</i> if the user changes their answers from being that of a
   *  non-UK-resident trust to that of a UK-resident trust</li>
   * </ol>
   */
  def removeOptionalTrustDetailTransforms(identifier: String, internalId: String, sessionId: String): TrustEnvelope[Boolean] = EitherT {

    val optionalTrustDetails = Seq(LAW_COUNTRY, UK_RELATION, DEED_OF_VARIATION, INTER_VIVOS, EFRBS_START_DATE)

    val expectedResult = for {
      transforms <- repository.get(identifier, internalId, sessionId)
      updatedTransforms = transforms match {
        case Some(value) => ComposedDeltaTransform(value.deltaTransforms.filter {
          case x: SetTrustDetailTransform if optionalTrustDetails.contains(x.`type`) => false
          case _ => true
        })
        case None => ComposedDeltaTransform()
      }
      result <- repository.set(identifier, internalId, sessionId, updatedTransforms)
    } yield {
      result
    }

    expectedResult.value.flatMap {
      case Left(_) =>
        logger.warn(s"[$className][removeOptionalTrustDetailTransforms][SessionId: $sessionId] " +
          s"an error occurred, failed to retrieve trusts details from repository.")
        Future.successful(Left(ServerError()))
      case Right(result) => Future.successful(Right(result))
    }
  }
}
