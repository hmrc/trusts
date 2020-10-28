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

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import models.NameType
import models.get_trust.get_trust
import models.get_trust.get_trust.{TrustProcessedResponse, _}
import models.variation.NaturalPersonType
import transformers._
import transformers.remove.RemoveOtherIndividual
import utils.{JsonFixtures, JsonUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OtherIndividualTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonFixtures {

  private implicit val pc: PatienceConfig =
    PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private def otherIndividualJson(endDate: Option[LocalDate] = None) = {
    if (endDate.isDefined) {
      Json.obj("field1" -> "value20", "endDate" -> endDate.get, "lineNo" -> 65)
    } else {
      Json.obj("field1" -> "value20", "lineNo" -> 65)
    }
  }

  object LocalDateMock extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  def buildInputJson(data: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "naturalPerson").json
      .put(JsArray(data))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "The otherIndividual transformation service" - {

    "must add a new transform" - {

      "for new remove otherIndividual using the transformation service" in {
        val transformationService = mock[TransformationService]
        val service = new OtherIndividualTransformationService(transformationService, LocalDateMock)
        val otherIndividual = otherIndividualJson()

        when(transformationService.addNewTransform(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustProcessedResponse(
            buildInputJson(Seq(otherIndividual)),
            ResponseHeader("status", "formBundlNo")
          )))

        val result = service.removeOtherIndividual("utr", "internalId", RemoveOtherIndividual(LocalDate.of(2013, 2, 20), 0))
        whenReady(result) { _ =>
          verify(transformationService).addNewTransform("utr",
            "internalId", RemoveOtherIndividualsTransform(0, otherIndividual, LocalDate.of(2013, 2, 20)))
        }
      }

      "for amend other individual using the transformation service" in {

        val transformationService = mock[TransformationService]
        val service = new OtherIndividualTransformationService(transformationService, LocalDateMock)

        val amendedOtherIndividual = NaturalPersonType(
          name = NameType("First", None, "Last"),
          dateOfBirth = None,
          identification = None,
          lineNo = None,
          bpMatchStatus = None,
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          entityStart = LocalDateMock.now,
          entityEnd = None
        )

        val originalOtherIndividual = Json.parse(
          """
            |{
            |  "lineNo":"1",
            |  "name":{
            |    "firstName":"John",
            |    "middleName":"William",
            |    "lastName":"O'Connor"
            |  },
            |  "dateOfBirth":"1956-02-12",
            |  "identification":{
            |    "nino":"KC287812"
            |  },
            |  "entityStart":"1998-02-12"
            |}
            |""".stripMargin)

        when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

        val desResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(
            Future.successful(
              get_trust.TrustProcessedResponse(desResponse, ResponseHeader("status", "formBundlNo"))
            ))

        val result = service.amendOtherIndividualTransformer("utr", 0, "internalId", amendedOtherIndividual)
        whenReady(result) { _ =>

          verify(transformationService).addNewTransform(
            "utr",
            "internalId",
            AmendOtherIndividualTransform(0, Json.toJson(amendedOtherIndividual), originalOtherIndividual, LocalDateMock.now)
          )
        }

      }
    }

  }
}
