package controllers.rest

import javax.inject.Inject

import scala.concurrent.duration._
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.rest.restmodel.{JsonEvent, JsonUser}
import dao.EventDAO
import model.{Event, EventNotFoundException, UserHasNoGroupException, UserNotFoundException}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import play.api.libs.functional.syntax._

import scala.concurrent.{Await, Future}

class EventRestController @Inject()(eventDAO: EventDAO) extends Controller {

  /* refactor to jsonHeaders for seriousness */
  val effinHeaders = "Content-Type" -> "application/json; charset=utf-8"

  implicit val eventWrites = new Writes[JsonEvent] {
    def writes(event: JsonEvent) = {

      val toAsString: String = event.to match {
        case Some(date) => date
        case None => "None"
      }

      Json.obj(
        "id" -> event.id,
        "title" -> event.eventName,
        "from" -> event.from.toString,
        "to" -> toAsString,
        "groupId" -> event.groupId
      )
    }
  }

  implicit val eventReads: Reads[JsonEvent] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "title").read[String] and
      (JsPath \ "from").read[String] and
      (JsPath \ "to").readNullable[String] and
      (JsPath \ "groupId").read[Int]
    )(JsonEvent.apply _)

  def addEvent() = Action.async(BodyParsers.parse.json) { request =>
     val eventResult = request.body.validate[JsonEvent]

    eventResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      event => {

        val eventToIntoMillis: Long = event.toDateOption match {
          case Some(date) => date.getMillis
          case None => 0L
        }

        val eventToDb = Event(event.id, event.eventName, event.fromDate.getMillis, eventToIntoMillis, event.groupId)

        eventDAO.addEvent(eventToDb).map {
          result => {
            result match {
              case Success(res) => {
                Logger.debug("Added event: " + event.eventName)
                Created.withHeaders("Location" -> ("/events/" + res))
              }
              case Failure(e: MySQLIntegrityConstraintViolationException) => {
                toFailureJson("No group with that ID")
              }
              case Failure(e: Exception) => {
                Logger.error(e.toString)
                toFailureJson(e.getMessage)
              }
            }
          }
        }
      }
    )
  }

  private def toFailureJson(message: String): Result = {
    BadRequest(Json.obj("status" -> "KO", "message" -> (message)))
  }

  def allEvents() = Action.async {
    eventDAO.allEvents.map {
      events => {

        val asJsonEvents = events.seq.map(event => dbEventToJsonEvent(event))

        val asJson = Json.toJson(asJsonEvents)
        Ok(asJson).withHeaders(effinHeaders)
        }
      }
  }

  def eventsForUserJson(userId: Int) = Action.async {

    eventDAO.getEventsForUser(userId).map {
      result => result match {
        case Success(res) => {
          val asJsonEvents = res.seq.map(event => dbEventToJsonEvent(event))
          Ok(Json.toJson(asJsonEvents)).withHeaders(effinHeaders)
        }
        case Failure(e: UserHasNoGroupException) => {
          toFailureJson("User has no group")
        }
        case Failure(e: UserNotFoundException) => {
          NotFound
        }
      }
    }

    /* pseudo for recovering from bad future */
    /* eventDAO.getEventsForUser(userId).map {
      events => {
        val asJsonEvents = events.map(_.toJsonEvent)
        Ok(Json.toJson(asJsonEvents)).withHeaders(effinHeaders)
      } recover {
        case e: UserHasNoGroupException => toFailureJson("User has no group")
        case e: UserNotFoundException => NotFound
        case e: Exception => {
          Logger.warn("fallthrough in eventsforuserjson")
        }
      }
    } */

  }

  private def dbEventToJsonEvent(dbEvent: Event): JsonEvent = {
    val fromString: String = new DateTime(dbEvent.from).toString

    /* WARNING: imperative programming */
    /* can i use pattern matching instead? */
    var toString: Option[String] = None
    if (dbEvent.to != 0) {
      toString = Some(new DateTime(dbEvent.to).toString)
    }

    // JsonEvent(dbEvent.id, dbEvent.eventName, fromString, toString, dbEvent.groupId)
    JsonEvent(dbEvent.id, dbEvent.eventName, fromString, toString, dbEvent.groupId)
  }

  private def jsonEventToDbEvent(jsonEvent: JsonEvent): Event = {
    val fromLong: Long = new DateTime(jsonEvent.from).getMillis
    val toLong: Long = jsonEvent.to match {
      case None => 0L
      case Some(string) => new DateTime(string).getMillis
    }
    Event(jsonEvent.id, jsonEvent.eventName, fromLong, toLong, jsonEvent.groupId)
  }


  def getEvent(eventId: Int) = Action.async {
    eventDAO.getEventById(eventId).map {
      case Some(event) => {
        val jsonEvent = dbEventToJsonEvent(event)
        Ok(Json.toJson(jsonEvent))
      }
      case None => NotFound
    }
  }

  def updateEvent(eventId: Int) = Action.async(BodyParsers.parse.json) { request =>
    val eventResult = request.body.validate[JsonEvent]

    eventResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      jsonEvent => {
        val dbEvent = jsonEventToDbEvent(jsonEvent)
        eventDAO.updateEvent(dbEvent).map {
          result => {
            result match {
              case Success(res) => {
                Logger.debug("result: " + res)
                Ok
              }
              case Failure(e: EventNotFoundException) => NotFound
              case Failure(e: MySQLIntegrityConstraintViolationException) => {
                toFailureJson("No such groupId")
              }
              case Failure(e: Exception) => {
                Logger.error("updateEvent(): " + e.getMessage)
                toFailureJson(e.getMessage)
              }
            }
          }
        }
      }

    )
  }
}
