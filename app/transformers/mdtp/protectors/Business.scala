/*
 * Copyright 2021 HM Revenue & Customs
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

package transformers.mdtp.protectors

import play.api.libs.json._
import models.variation.ProtectorCompany

object Business {

  private val path = JsPath \ 'details \ 'trust \ 'entities \ 'protectors \ 'protectorCompany

  def transform(response : JsValue) : Reads[JsObject] = {
    response.transform(path.json.pick).fold(
      _ => {
        JsPath.json.update(
          path.json.put(JsArray())
        )
      },
      protectors => {

        val protectorsUpdated = JsArray(protectors.as[List[ProtectorCompany]].map {
          protector =>
            Json.toJson(protector)(ProtectorCompany.writeToMaintain)
        })

        JsPath.json.update(
          path.json.prune andThen
            path.json.put(protectorsUpdated)
        )
      }
    )
  }

}
