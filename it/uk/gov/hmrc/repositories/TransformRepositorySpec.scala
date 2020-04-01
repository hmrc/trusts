package uk.gov.hmrc.repositories

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.test.Helpers.running
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, AmendLeadTrusteeIndTransform, ComposedDeltaTransform}

import scala.concurrent.ExecutionContext.Implicits.global

class TransformRepositorySpec extends FreeSpec with MustMatchers with TransformIntegrationTest {

  "a transform repository" - {

    "must be able to store and retrieve a payload" in {

      val application = applicationBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[TransformationRepository]

          val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
          storedOk.futureValue mustBe true

          val retrieved = repository.get("UTRUTRUTR", "InternalId")
            .map(_.getOrElse(fail("The record was not found in the database")))

          retrieved.futureValue mustBe data

          dropTheDatabase(connection)
        }.get
      }
    }
  }

  val data = ComposedDeltaTransform(
    Seq(
      AmendLeadTrusteeIndTransform(
        DisplayTrustLeadTrusteeIndType(
          Some(""),
          None,
          NameType("New", Some("lead"), "Trustee"),
          DateTime.parse("2000-01-01"),
          "",
          None,
          DisplayTrustIdentificationType(None, None, None, None),
          Some(DateTime.parse("2010-10-10"))
        )
      ),
      AddTrusteeIndTransform(
        DisplayTrustTrusteeIndividualType(
          Some("lineNo"),
          Some("bpMatchStatus"),
          NameType("New", None, "Trustee"),
          Some(DateTime.parse("2000-01-01")),
          Some("phoneNumber"),
          Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
          DateTime.parse("2010-10-10")
        )
      )
    )
  )
}
