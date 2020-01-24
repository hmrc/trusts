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

package uk.gov.hmrc.trusts.transformers

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json._
import uk.gov.hmrc.trusts.utils.JsonUtils

class DeclareNoChangeTransformer {
  def transform(beforeJson: JsValue): JsResult[JsValue] = {

    val fromPath = (__ \ 'responseHeader \ 'formBundleNo ).json
    val toPath = (__ \ 'reqHeader \ 'formBundleNo).json

    val formBundleNoTransformer = toPath.copyFrom(fromPath.pick)
    beforeJson.transform(formBundleNoTransformer)
  }
}

class DeclareNoChangeTransformerSpec extends FreeSpec with MustMatchers with OptionValues {
  "the no change transformer should" - {
    "copy the formBundleNo from the response header to the request header" in {
      val beforeJson: JsValue = Json.parse(
        """
          |{
          |"responseHeader": {
          |    "dfmcaReturnUserStatus": "Processed",
          |    "formBundleNo": "000012387218"
          |  }
          |}
          |""".stripMargin)
      val afterJson = Json.parse(
        """
          |{
          |"reqHeader": {
          |    "formBundleNo": "000012387218"
          |  }
          |}
          |""".stripMargin)
      val transformer = new DeclareNoChangeTransformer

      val result: JsResult[JsValue] = transformer.transform(beforeJson)
      result match {
        case JsSuccess(json, _) => json mustBe afterJson
        case JsError(errors) => println(s"Errors: $errors")
      }
    }

    "do it all" ignore {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent.json")
      val transformer = new DeclareNoChangeTransformer

      val result = transformer.transform(beforeJson)
      result.asOpt.value mustBe afterJson
    }
  }
}