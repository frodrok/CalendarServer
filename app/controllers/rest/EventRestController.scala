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
    ) (JsonEvent.apply _)

  def addEvent() = Action.async(BodyParsers.parse.json) { request =>
    val eventResult = request.body.validate[Event]

    eventResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      event => {

        eventDAO.addEvent(event).map {
          result => {
            result match {
              case None => toFailureJson("Option was none, not sure what happened")
              case Some(result) => {
                Logger.debug("Added event " + event.eventName)
                Created.withHeaders("Location" -> ("/events/" + result))
              }
            }
          }
        } recover {
          case e: MySQLIntegrityConstraintViolationException => toFailureJson("No group with that ID")
          case e: Exception => {
            Logger.error(e.toString)
            toFailureJson(e.getMessage)
          }
        }
      }
    )
  }

  implicit val dbEventReads: Reads[Event] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "title").read[String] and
      (JsPath \ "from").read[Long] and
      (JsPath \ "to").readNullable[Long] and
      (JsPath \ "groupId").read[Int] and
      (JsPath \ "background").readNullable[Boolean]
    ) (Event.apply _)


  def serializeList = Action.async(BodyParsers.parse.json) { request =>
    val events = request.body.validate[List[Event]]

    events.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      success => {

        val ids = success.map {
          event => {
            val toCannotBeNull: Event = event.to match {
              case None => Event(event.id, event.eventName, event.from, Some(0), event.groupId, event.background)
              case Some(long) => event
            }
            eventDAO.addEvent(toCannotBeNull)
          }
        }

        Future.sequence(ids).map(_.flatten).map {
          list => Ok(Json.toJson(list))
        } recover {
          case ex: Exception => {
            Logger.error(ex.toString)
            toFailureJson(ex.getMessage)
          }
        }
      }
    )

  }

  def serializeListOptions = Action { request =>

    Ok("").withHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With",
      "Access-Control-Allow-Credentials" -> "true",
      "Access-Control-Max-Age" -> (60 * 60 * 24).toString
    )

  }

  private def toFailureJson(message: String): Result = {
    BadRequest(Json.obj("status" -> "KO", "message" -> (message)))
  }

  implicit val dbEventWrites = new Writes[Event] {
    def writes(event: Event) = {

      val to: Long = event.to match {
        case None => 0L
        case Some(value) => value
      }

      Json.obj(
        "id" -> event.id,
        "title" -> event.eventName,
        "from" -> event.from,
        "to" -> to,
        "groupId" -> event.groupId,
        "background" -> event.background
      )
    }
  }

  def allEvents() = Action.async {
    eventDAO.allEvents.map {
      events => {

        val asJson = Json.toJson(events)
        Ok(asJson).withHeaders(effinHeaders)
      }
    }
  }

  def eventsForUserJson(userId: Int) = Action.async {
    eventDAO.getEventsForUser(userId).map {
      events => Ok(Json.toJson(events)).withHeaders(effinHeaders)
    } recover {
      case e: UserHasNoGroupException => toFailureJson("User has no group")
      case e: UserNotFoundException => NotFound
      case e: Exception => toFailureJson(e.getMessage)
    }

  }

  def getEvent(eventId: Int) = Action.async {
    eventDAO.getEventById(eventId).map {
      case Some(event) => {
        Ok(Json.toJson(event))
      }
      case None => NotFound
    }
  }

  def updateEvent(eventId: Int) = Action.async(BodyParsers.parse.json) { request =>
    val eventResult = request.body.validate[Event]

    eventResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      event => {

        eventDAO.updateEvent(event).map {
          result => {
            Logger.debug("result: " + result)
            Ok
          }
        } recover {
          case e: EventNotFoundException => NotFound
          case e: MySQLIntegrityConstraintViolationException => toFailureJson("No group with that ID")
          case e: Exception => {
            Logger.error("updateEvent(): " + e.getMessage);
            toFailureJson(e.getMessage)
          }
        }

      })

  }
}

