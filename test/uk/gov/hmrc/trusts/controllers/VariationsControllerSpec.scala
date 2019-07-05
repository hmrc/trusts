/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.controllers

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustFoundResponse
import uk.gov.hmrc.trusts.models.variation.VariationTvnResponse
import uk.gov.hmrc.trusts.services.DesService

import scala.concurrent.Future

class VariationsControllerSpec extends BaseSpec {

  lazy val mockDesService: DesService = mock[DesService]

  private def variationsController = {
    val SUT = new VariationsController(new FakeIdentifierAction(Organisation), mockDesService)
    SUT
  }

  ".variation" should {
    "return a 200 - OK" when {
      "the des service returns a VariationTvnResponse" in {
        when(mockDesService.variation()(any())).thenReturn(Future.successful(VariationTvnResponse("XXTVN1234567890")))

        val result = variationsController.variation().apply(postRequestWithPayload(Json.obj("test" -> "value"), withDraftId = false))

        status(result) mustBe OK
      }
    }
  }
}
