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

package controllers.transformations.trustees

import controllers.actions.FakeIdentifierAction
import models.NameType
import models.variation._
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{CONTENT_TYPE, _}
import play.api.test.{FakeRequest, Helpers}
import services.TransformationService
import transformers.remove.RemoveTrustee
import transformers.trustees.RemoveTrusteeTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.Constants.{BUSINESS_TRUSTEE, INDIVIDUAL_TRUSTEE}
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class RemoveTrusteeControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Agent)

  private val utr: String = "utr"
  private val index: Int = 0
  private val endDate: LocalDate = LocalDate.parse("2018-02-24")

  private val invalidBody: JsValue = Json.parse("{}")

  private def buildInputJson(trusteeData: Seq[JsValue]): JsObject = {
    val baseJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

    val adder = (__ \ "details" \ "trust" \ "entities" \ "trustees").json.put(JsArray(trusteeData))

    baseJson.as[JsObject](__.json.update(adder))
  }

  "Remove trustee controller" - {

    "individual trustee" - {

      val trustee = TrusteeType(
        trusteeInd = Some(TrusteeIndividualType(
          lineNo = None,
          bpMatchStatus = None,
          name = NameType("Joe", None, "Bloggs"),
          dateOfBirth = None,
          phoneNumber = None,
          identification = None,
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          entityStart = LocalDate.parse("2012-03-14"),
          entityEnd = None
        )),
        trusteeOrg = None
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveTrusteeController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(Seq(Json.toJson(trustee)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = RemoveTrustee(
          endDate = endDate,
          index = index,
          `type` = INDIVIDUAL_TRUSTEE
        )

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveTrusteeTransform(Some(index), Json.toJson(trustee), endDate, "trusteeInd")

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "business trustee" - {

      val trustee = TrusteeType(
        trusteeInd = None,
        trusteeOrg = Some(TrusteeOrgType(
          lineNo = None,
          bpMatchStatus = None,
          name = "Name",
          phoneNumber = None,
          email = None,
          identification = None,
          countryOfResidence = None,
          entityStart = LocalDate.parse("2012-03-14"),
          entityEnd = None
        ))
      )

      "add a new remove transform" in {

        val mockTransformationService = mock[TransformationService]

        val controller = new RemoveTrusteeController(
          identifierAction,
          mockTransformationService
        )(Implicits.global, Helpers.stubControllerComponents())

        when(mockTransformationService.getTransformedTrustJson(any(), any(), any())(any()))
          .thenReturn(Future.successful(buildInputJson(Seq(Json.toJson(trustee)))))

        when(mockTransformationService.addNewTransform(any(), any(), any())(any()))
          .thenReturn(Future.successful(true))

        val body = RemoveTrustee(
          endDate = endDate,
          index = index,
          `type` = BUSINESS_TRUSTEE
        )

        val request = FakeRequest(POST, "path")
          .withBody(Json.toJson(body))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.remove(utr).apply(request)

        status(result) mustBe OK

        val transform = RemoveTrusteeTransform(Some(index), Json.toJson(trustee), endDate, "trusteeOrg")

        verify(mockTransformationService)
          .addNewTransform(equalTo(utr), any(), equalTo(transform))(any())

      }
    }

    "return an error for invalid json" in {

      val mockTransformationService = mock[TransformationService]

      val controller = new RemoveTrusteeController(
        identifierAction,
        mockTransformationService
      )(Implicits.global, Helpers.stubControllerComponents())

      val request = FakeRequest(POST, "path")
        .withBody(invalidBody)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.remove(utr)(request)

      status(result) mustBe BAD_REQUEST

    }
  }
}
