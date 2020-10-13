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

package uk.gov.hmrc.repositories

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json._
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.registration.RegistrationSubmissionDraftData

class SubmissionDraftManagementSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  private val draftData = Json.obj(
    "field1" -> "value1",
    "field2" -> "value2"
  )
  private val amendedDraftData = Json.obj(
    "fieldY" -> "new value1",
    "fieldZ" -> " new value2"
  )
  private val createdAtPath = JsPath() \ 'createdAt
  private val dataPath = JsPath() \ 'data
  private val referencePath = JsPath() \ 'reference

  "working with submission drafts" - {
    "must be CRUDdy" in {

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Helpers.stubControllerComponents().parsers.default, Organisation))
        )
        .build()

      running(application) {
        getConnection(application).map { connection =>
          dropTheDatabase(connection)

          // Initial empty
          {
            val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts")).get
            status(result) mustBe OK
            contentAsJson(result) mustBe Json.parse("[]")
          }

          // Read non-existent draft
          {
            val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/beneficiaries")).get
            status(result) mustBe NOT_FOUND
          }

          // Create draft section
          {
            val draftRequestData = RegistrationSubmissionDraftData(draftData, None, None)
            val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
                        .withBody(Json.toJson(draftRequestData))
                        .withHeaders(CONTENT_TYPE -> "application/json")
            val result = route(application, request).get
            status(result) mustBe OK
          }

          // Read draft section
          {
            val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/main")).get
            status(result) mustBe OK

            val json = contentAsJson(result)

            assert(json.transform(createdAtPath.json.pick).isSuccess)

            json.transform(dataPath.json.pick) mustBe JsSuccess(draftData, dataPath)
            assert(json.transform(referencePath.json.pick).isError)
          }

          // Update draft section
          {
            val amendedDraftRequestData = RegistrationSubmissionDraftData(amendedDraftData, Some("amendedReference"), None)
            val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
              .withBody(Json.toJson(amendedDraftRequestData))
              .withHeaders(CONTENT_TYPE -> "application/json")
            val result = route(application, request).get
            status(result) mustBe OK
          }

          // Read amended draft section
          {
            val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/main")).get
            status(result) mustBe OK

            val resultJson = contentAsJson(result)
            resultJson.transform(dataPath.json.pick) mustBe JsSuccess(amendedDraftData, dataPath)
            resultJson.transform(referencePath.json.pick) mustBe JsSuccess(JsString("amendedReference"), referencePath)
          }

          // Read all drafts
          {
            val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts")).get
            status(result) mustBe OK
            val json = contentAsJson(result)

            val drafts = json.as[JsArray]
            drafts.value.size mustBe 1

            val draft = drafts(0)
            assert(draft.transform(createdAtPath.json.pick).isSuccess)

            val draftIdPath = JsPath() \ 'draftId
            draft.transform(draftIdPath.json.pick) mustBe JsSuccess(JsString("Draft0001"), draftIdPath)
            draft.transform(referencePath.json.pick) mustBe JsSuccess(JsString("amendedReference"), referencePath)
          }

          // Delete draft
          {
            val result = route(application, FakeRequest(DELETE, "/trusts/register/submission-drafts/Draft0001")).get
            status(result) mustBe OK
          }

          // Read all (empty) drafts
          {
            val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts")).get
            status(result) mustBe OK
            contentAsJson(result) mustBe Json.parse("[]")
          }

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}
