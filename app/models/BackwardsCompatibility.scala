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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Writes, __}

case class AgentDetailsBC(internalReference: String,
                          name: String,
                          addressUKYesNo: Boolean,
                          ukAddress: Option[AddressType],
                          internationalAddress: Option[AddressType],
                          telephoneNumber: String,
                          agentARN: String)

object AgentDetailsBC {

  implicit val oldReads: Reads[AgentDetailsBC] = {

    val ukAddressReads: Reads[AddressType] = {
      (
        (__ \ "line1").read[String] and
          (__ \ "line2").read[String] and
          (__ \ "line3").readNullable[String] and
          (__ \ "line4").readNullable[String] and
          (__ \ "postcode").readNullable[String]).tupled.map {
        case (line1, line2, line3, line4, postCode) =>
          AddressType(line1, line2, line3, line4, postCode, "GB")
      }
    }

    (
      (__ \ "internalReference").read[String] and
        (__ \ "name").read[String] and
        (__ \ "addressYesNo").read[Boolean] and
        (__ \ "ukAddress").readNullable[AddressType](ukAddressReads) and
        (__ \ "internationalAddress").readNullable[AddressType] and
        (__ \ "telephoneNumber").read[String] and
        (__ \ "agentARN").read[String]
      )(AgentDetailsBC.apply _)
  }

  implicit val newWrites: Writes[AgentDetailsBC] = {

    val ukAddressWrites: Writes[AddressType] = {
      (
        (__ \ "line1").write[String] and
          (__ \ "line2").write[String] and
          (__ \ "line3").writeNullable[String] and
          (__ \ "line4").writeNullable[String] and
          (__ \ "postcode").writeNullable[String] and
          (__ \ "country").write[String]
        )(unlift(AddressType.unapply))
    }

    (
      (__ \ "internalReference").write[String] and
        (__ \ "name").write[String] and
        (__ \ "addressUKYesNo").write[Boolean] and
        (__ \ "ukAddress").writeNullable[AddressType](ukAddressWrites) and
        (__ \ "internationalAddress").writeNullable[AddressType] and
        (__ \ "telephoneNumber").write[String] and
        (__ \ "agentARN").write[String]
      )(unlift(AgentDetailsBC.unapply))
  }
}
