import javax.inject.Inject

import `trait`.EventJsonHandler
import dao.{EventDAO, UserDAO}
import model.Event
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import play.api.{Configuration, Logger, Mode}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec @Inject()(eventDAO: EventDAO) extends PlaySpec with OneAppPerTest with EventJsonHandler {

  implicit val eventReads = getReads
  implicit val eventWrites = getWrites

  def testApp = new GuiceApplicationBuilder()
    .configure(
      Configuration.from(
        Map(
          "slick.dbs.msql.driver" -> "slick.driver.H2Driver$",
          "slick.dbs.msql.db.driver" -> "org.h2.Driver",
          "slick.dbs.msql.db.url" -> "jdbc:h2:mem:playnow",

          "slick.dbs.default.driver" -> "slick.driver.MySQLDriver$",
          "slick.dbs.default.db.driver" -> "com.mysql.jdbc.Driver"
        )
      )
    )
    .in(Mode.Test)
    .build()

  "Routes" should {

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

/*   "Event REST postable" should {
    "be postable" in {

      val from = DateTime.now
      val to = DateTime.now().plusDays(1)

      val event: Event = Event(None, "hey now", from.getMillis, Some(to.getMillis), 1, None)

      val jsonEvent = Json.toJson(event)

      route(app, FakeRequest(POST,
        "/events",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        jsonEvent
      )).map(status(_)) mustBe OK

    }
  } */
}
