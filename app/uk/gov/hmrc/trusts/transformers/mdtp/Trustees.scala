package uk.gov.hmrc.trusts.transformers.mdtp

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType, DisplayTrustTrusteeType}

object Trustees {

  private val pathToTrustees = JsPath \ 'details \ 'trust \ 'entities \ 'trustees

  def transform(response : JsValue) : Reads[JsObject] = {
    response.transform(pathToTrustees.json.pick).fold(
      _ => {
        JsPath.json.pick[JsObject]
      },
      trustees => {

        val trusteesUpdated = Json.obj(
          "trustees" -> trustees.as[List[DisplayTrustTrusteeType]].map {
            case DisplayTrustTrusteeType(Some(trusteeInd), None) =>
              Json.obj(
                "trusteeInd" -> Json.toJson(trusteeInd)(DisplayTrustTrusteeIndividualType.writeToMaintain)
              )
            case DisplayTrustTrusteeType(None, Some(trusteeOrg)) =>
              Json.obj(
                "trusteeOrg" -> Json.toJson(trusteeOrg)(DisplayTrustTrusteeOrgType.writeToMaintain)
              )
            case _ => JsNull
          }
        )

        pathToTrustees.json.prune andThen
          pathToTrustees.json.put(trusteesUpdated)
      }
    )
  }

}
