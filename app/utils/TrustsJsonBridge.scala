/*
 * Copyright 2026 HM Revenue & Customs
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

package utils

import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.{Logger, Logging}

trait TrustsJsonBridge extends Logging {

  implicit class RichJsObject(in: JsValue) {

    implicit val log: Logger = logger

    def convertToMdtpJson: JsObject =
      (in \ "success" \ "trustOrEstateDisplay" \ "details" \ "trust" \ "entities" \ "leadTrustees" \ "name" \ "lastName").toOption match {
        case None    =>
          val trustChange = JsonNodeRenamer.renameNode(
            in.as[JsObject],
            "success.trustOrEstateDisplay.details.trust.entities.beneficiary.trusts",
            "trust"
          )
          val nameChange  = JsonNodeRenamer.renameNode(
            trustChange,
            "success.trustOrEstateDisplay.details.trust.entities.leadTrustees.orgName",
            "name"
          )
          nameChange
        case Some(_) =>
          JsonNodeRenamer.renameNode(
            in.as[JsObject],
            "success.trustOrEstateDisplay.details.trust.entities.beneficiary.trusts",
            "trust"
          )
      }

    def convertToHipJson: JsObject =
      JsonNodeRenamer.renameNode(in.as[JsObject], "details.trust.entities.leadTrustees.name", "orgName")

    def convertToHipJsonForVariation: JsValue = {
      val transformation: Reads[JsObject] = (__ \ "details" \ "trust" \ "entities" \ "leadTrustees").json.update(
        Reads
          .list {
            ((__ \ "leadTrusteeOrg").json.update(
              (__ \ "orgName").json.copyFrom((__ \ "name").json.pick) orElse
                (__ \ "leadTrusteeInd").json.update((__ \ "name").json.copyFrom((__ \ "name").json.pick))
            )) andThen
              (__ \ "leadTrusteeOrg" \ "name").json.prune orElse
              (__ \ "leadTrusteeInd").json.update((__ \ "name").json.copyFrom((__ \ "name").json.pick))
          }
          .map(JsArray(_))
      )

      in.transform(transformation).getOrElse {
        logger.warn("unable to convert name => orgName for trust variation payload")
        in
      }
    }

  }

}
