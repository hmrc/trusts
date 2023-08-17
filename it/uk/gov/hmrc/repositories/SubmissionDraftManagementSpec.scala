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

package uk.gov.hmrc.repositories

import models.registration.RegistrationSubmissionDraftData
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.itbase.IntegrationTestBase

class SubmissionDraftManagementSpec extends IntegrationTestBase {

  private val draftData = Json.obj(
    "field1" -> "value1",
    "field2" -> "value2"
  )
  private val amendedDraftData = Json.obj(
    "fieldY" -> "new value1",
    "fieldZ" -> " new value2"
  )
  private val createdAtPath = JsPath() \ Symbol("createdAt")
  private val dataPath = JsPath() \ Symbol("data")
  private val referencePath = JsPath() \ Symbol("reference")
  private val draftIdPath = JsPath() \ Symbol("draftId")

  "working with submission drafts" should {

    "read an empty document" in assertMongoTest(createApplication)({ (app) =>
      // Initial empty
      val result = route(app, FakeRequest(GET, "/trusts/register/submission-drafts")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    })

    "read non-existent draft" in assertMongoTest(createApplication)({ (app) =>
      // Read non-existent draft
      val result = route(app, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/beneficiaries")).get
      status(result) mustBe NOT_FOUND
    })

    "create a draft section" in assertMongoTest(createApplication)({ (app) =>
      // Create draft section
      val draftRequestData = RegistrationSubmissionDraftData(draftData, None, None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(draftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val result = route(app, request).get
      status(result) mustBe OK
    })

    "read a draft section" in assertMongoTest(createApplication) { (app) =>
      // Create draft section
      val draftRequestData = RegistrationSubmissionDraftData(draftData, None, None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(draftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val rez = route(app, request).get
      status(rez) mustBe OK

      // Read draft section
      val result = route(app, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/main")).get
      status(result) mustBe OK

      val json = contentAsJson(result)

      assert(json.transform(createdAtPath.json.pick).isSuccess)

      json.transform(dataPath.json.pick) mustBe JsSuccess(draftData, dataPath)
      assert(json.transform(referencePath.json.pick).isError)
    }

    "update a section" in assertMongoTest(createApplication)({ (app) =>
      // Update draft section
      val amendedDraftRequestData = RegistrationSubmissionDraftData(amendedDraftData, Some("amendedReference"), None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(amendedDraftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val result = route(app, request).get
      status(result) mustBe OK
    })

    "read an amended section" in assertMongoTest(createApplication) { (app) =>
      // Update draft section
      val amendedDraftRequestData = RegistrationSubmissionDraftData(amendedDraftData, Some("amendedReference"), None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(amendedDraftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val rez = route(app, request).get
      status(rez) mustBe OK

      // Read amended draft section
      val result = route(app, FakeRequest(GET, "/trusts/register/submission-drafts/Draft0001/main")).get
      status(result) mustBe OK

      val resultJson = contentAsJson(result)
      resultJson.transform(dataPath.json.pick) mustBe JsSuccess(amendedDraftData, dataPath)
      resultJson.transform(referencePath.json.pick) mustBe JsSuccess(JsString("amendedReference"), referencePath)
    }

    "read all drafts" in assertMongoTest(createApplication) { (app) =>
      // Create drafts section
      val draftRequestData = RegistrationSubmissionDraftData(draftData, None, None)
      val request = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(draftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val rez1 = route(app, request).get
      status(rez1) mustBe OK
      // Update draft section
      val amendedDraftRequestData = RegistrationSubmissionDraftData(amendedDraftData, Some("amendedReference"), None)
      val request2 = FakeRequest(POST, "/trusts/register/submission-drafts/Draft0001/main")
        .withBody(Json.toJson(amendedDraftRequestData))
        .withHeaders(CONTENT_TYPE -> "application/json")
      val rez2 = route(app, request2).get
      status(rez2) mustBe OK

      // Read all drafts
      val result = route(app, FakeRequest(GET, "/trusts/register/submission-drafts")).get
      status(result) mustBe OK
      val json = contentAsJson(result)

      val drafts = json.as[JsArray]
      drafts.value.size mustBe 1

      val draft = drafts(0)
      assert(draft.transform(createdAtPath.json.pick).isSuccess)

      draft.transform(draftIdPath.json.pick) mustBe JsSuccess(JsString("Draft0001"), draftIdPath)
      draft.transform(referencePath.json.pick) mustBe JsSuccess(JsString("amendedReference"), referencePath)
    }

    "delete a draft" in assertMongoTest(createApplication)({ (app) =>
      // Delete draft
      val result = route(app, FakeRequest(DELETE, "/trusts/register/submission-drafts/Draft0001")).get
      status(result) mustBe OK
    })

    "read all empty drafts" in assertMongoTest(createApplication)({ (app) =>
      // Read all (empty) drafts
      val result = route(app, FakeRequest(GET, "/trusts/register/submission-drafts")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.parse("[]")
    })
  }
}
