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

package transformers.mdtp

import models.variation.Entity
import play.api.libs.json._
import utils.JsonOps.putNewValue

trait Entities[T <: Entity[T]] {

  val path: JsPath

  def transform(response: JsValue)(implicit rds: Reads[T]): Reads[JsObject] = {
    response.transform(path.json.pick).fold(
      _ => {
        putNewValue(path, JsArray())
      },
      entities => {
        val updatedEntities = updateEntities(entities)
        
        JsPath.json.update(
          path.json.prune andThen
            path.json.put(updatedEntities)
        )
      }
    )
  }

  def updateEntities(entities: JsValue)(implicit rds: Reads[T]): JsArray = {
    JsArray(entities.as[List[T]].map {
      entity =>
        Json.toJson(entity)(entity.writeToMaintain)
    })
  }

}
