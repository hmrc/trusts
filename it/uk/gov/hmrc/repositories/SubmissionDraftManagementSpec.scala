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
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.itbase.IntegrationTestBase
import models.registration.RegistrationSubmissionDraftData
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers._

class SubmissionDraftManagementSpec extends AsyncFreeSpec with MockitoSugar with IntegrationTestBase {

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

    "must read an empty document" in assertMongoTest(createApplication) { application =>
      // Initial empty
      val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    }

    "must read non-existent draft" in assertMongoTest(createApplication) { application =>
      // Read non-existent draft
      val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/beneficiaries")).get
      status(result) mustBe NOT_FOUND
    }

    "must create a draft section" in assertMongoTest(createApplication) { application =>
      // Create draft section
      val draftRequestData = RegistrationSubmissionDraftData(draftData, None, None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
                  .withBody(Json.toJson(draftRequestData))
                  .withHeaders(CONTENT_TYPE -> "application/json")
      val result = route(application, request).get
      status(result) mustBe OK
    }

    "must read a draft section" in assertMongoTest(createApplication) { application =>
      // Read draft section
      val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/main")).get
      status(result) mustBe OK

      val json = contentAsJson(result)

      assert(json.transform(createdAtPath.json.pick).isSuccess)

      json.transform(dataPath.json.pick) mustBe JsSuccess(draftData, dataPath)
      assert(json.transform(referencePath.json.pick).isError)
    }

    "must update a section" in assertMongoTest(createApplication) { application =>
      // Update draft section
      val amendedDraftRequestData = RegistrationSubmissionDraftData(amendedDraftData, Some("amendedReference"), None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(amendedDraftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val result = route(application, request).get
      status(result) mustBe OK
    }

    "must read an amended section" in assertMongoTest(createApplication) { application =>
      // Read amended draft section
      val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/main")).get
      status(result) mustBe OK

      val resultJson = contentAsJson(result)
      resultJson.transform(dataPath.json.pick) mustBe JsSuccess(amendedDraftData, dataPath)
      resultJson.transform(referencePath.json.pick) mustBe JsSuccess(JsString("amendedReference"), referencePath)
    }

    "must read all drafts" in assertMongoTest(createApplication) { application =>
      // Read all drafts
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

    "must delete a draft" in assertMongoTest(createApplication) { application =>
      // Delete draft
      val result = route(application, FakeRequest(DELETE, "/trusts/register/submission-drafts/Draft0001")).get
      status(result) mustBe OK
    }

    "must read all empty drafts" in assertMongoTest(createApplication) { application =>
      // Read all (empty) drafts
      val result = route(application, FakeRequest(GET, "/trusts/register/submission-drafts")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    }
  }
}
