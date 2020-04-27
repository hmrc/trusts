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

import play.api.libs.json._

case class AmendDeceasedSettlorTransform(amended: JsValue,
                                         original: JsValue) extends DeltaTransform {

  private val path: JsPath = __ \ 'details \ 'trust \ 'entities \ 'deceased

  override def applyTransform(input: JsValue): JsResult[JsValue] = amend(input, path, amended)

  private def amend(input: JsValue, path: JsPath, toReplaceWith: JsValue) : JsResult[JsValue] = {

    for {
      lineNo <- input.transform((path \ 'lineNo).json.pick)
      bpMatchStatus <- input.transform((path \ 'bpMatchStatus).json.pick)
      lineNoAndStatusPreserved <- input.transform(
        path.json.prune andThen
          JsPath.json.update {
            path.json.put(toReplaceWith) andThen
              (path \ 'lineNo).json.put(lineNo) andThen
              (path \ 'bpMatchStatus).json.put(bpMatchStatus)
          }
      )
    } yield {
      lineNoAndStatusPreserved
    }

  }
}

object AmendDeceasedSettlorTransform {

  val key = "AmendDeceasedSettlorTransform"

  implicit val format: Format[AmendDeceasedSettlorTransform] =
    Json.format[AmendDeceasedSettlorTransform]
}


