import javax.inject.Inject

import `trait`.EventJsonHandler
import dao.EventDAO
import model.Event
import org.joda.time.DateTime
import org.scalatestplus.play._
import play.api.Logger
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import slick.lifted.TableQuery

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest with EventJsonHandler {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "Event REST postable" should {
    "be postable" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val from = DateTime.now
        val to = from.plusDays(1)

        val event: Event = Event(None, "hey now", from.getMillis, Some(to.getMillis), 1, None)

        val jsonEvent = Json.toJson(event)

        // contentAsString(route(app, FakeRequest(GET, "/events")).get) mustBe "0"
        /* contentAsString(route(app, FakeRequest(
          POST,
          "/events",
          FakeHeaders(Seq("Content-Type" -> "application/json")),
          jsonEvent
        ))) */
        val wat = route(app, FakeRequest(
          POST,
          "/events",
          FakeHeaders(Seq("Content-Type" -> "application/json")),
          jsonEvent
        )).get

        // Logger.debug(wat)

        // status(wat) mustBe equalTo(OK)
        status(wat) mustBe equal(OK)

      }
    }
  }
  implicit val eventReads = getReads
  implicit val eventWrites = getWrites


}
