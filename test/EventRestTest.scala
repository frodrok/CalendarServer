import javax.inject.Inject

import `trait`.EventJsonHandler
import dao.{EventDAO, GroupDAO, UserDAO}
import model.{Event, Group}
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterAllConfigMap, ConfigMap}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Configuration, Logger, Mode}
import play.api.test.Helpers._
import play.api.test._
import play.api.libs.json.Json
import play.api.test.{FakeHeaders, FakeRequest}


class EventRestTest extends PlaySpec with OneAppPerTest with EventJsonHandler with BeforeAndAfterAllConfigMap {

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

  /* regular DI with @Inject does not work on tests */
  val userDAO = testApp.injector.instanceOf[UserDAO]
  val eventDAO = testApp.injector.instanceOf[EventDAO]
  val groupDAO = testApp.injector.instanceOf[GroupDAO]

  /* setup the db and add a group */
  override def beforeAll(configMap: ConfigMap): Unit = {
    val userDAO = testApp.injector.instanceOf[UserDAO]
    userDAO.setup
    groupDAO.addGroup(Group(0, "the cykas", true))
  }

  def generateEvent(id: Option[Int] = None, group: Boolean): Event = {
    val from = DateTime.now
    val to = DateTime.now().plusDays(1)

    group match {
      case false => Event(id, "hey now", from.getMillis, Some(to.getMillis), 0, None, None)
      case true => Event(id, "hey now", from.getMillis, Some(to.getMillis), 1, None, None)
    }
  }

  "Event REST controller" should {
    "swallow single" in {

      val jsonEvent = Json.toJson(generateEvent(None, true))

      route(testApp, FakeRequest(POST,
        "/events",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        jsonEvent
      )).map(status(_)) mustBe Some(CREATED)

    }

    "require group" in {
      val jsonEvent = Json.toJson(generateEvent(None, false))

      route(testApp, FakeRequest(POST,
        "/events",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        jsonEvent
      )).map(status) mustBe Some(BAD_REQUEST)
    }

    "swallow a group" in {

      val ids = List(1, 2, 3, 4, 5)

      val listOfEventsToCreate: Seq[Event] = ids.map {
        int => generateEvent(None, true)
      }

      val createEvents = Json.toJson(listOfEventsToCreate)

      /* post 5 events */
      val listFromPost = route(testApp, FakeRequest(POST,
        "/events",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        createEvents
      )).map{
        event => {
          val jsValue = Json.parse(contentAsString(event))

          val list: List[Int] = jsValue.validate[List[Int]].fold(
            errors => List.empty,
            success => success
          )
          list
        }
      }.get // mustBe Some(CREATED)

      listFromPost must have size 5

      /* post 2 new and 5 to update */
      val nevents: Seq[Event] = Seq(
        generateEvent(None, true),
        generateEvent(None, true)
      )

      val listOfEventsToUpdate: Seq[Event] = listFromPost.map(id => generateEvent(Some(id), true))

      val jsonEvents = Json.toJson(listOfEventsToUpdate ++ nevents)

      route(testApp, FakeRequest(POST,
          "/events",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        jsonEvents
      )).map{
        event => {
          val jsValue = Json.parse(contentAsString(event))
          val list: List[Int] = jsValue.validate[List[Int]].fold(
            errors => List.empty,
            success => success
          )
          list
        }
      }.get must have size 7 // (5 updated, 2 new)
    }

    var headerLocation: Option[String] = Some("hey")

    "delete event" in {
      val event = generateEvent(None, true)
      val fknId = route(testApp, FakeRequest(POST,
        "/events",
        FakeHeaders(Seq("Content-Type" -> "application/json")),
        Json.toJson(event))).map {
        response => {
          headerLocation = header("Location", response)

          Logger.debug(contentAsString(response))

          val fknString: String = headerLocation.get takeRight 2
          val watId: Int = Integer.parseInt(fknString)

          watId
        }
      }

      Logger.debug(fknId.get.toString)

      fknId must not be 0

      route(testApp, FakeRequest(DELETE,
        "/events/" + fknId)).map(status) mustBe Some(NO_CONTENT)
    }

  }

}
