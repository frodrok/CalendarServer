package controllers.rest

import javax.inject.Inject

import `trait`.EventJsonHandler
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import dao.EventDAO
import model.{Event, EventNotFoundException, UserHasNoGroupException, UserNotFoundException}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class EventRestController @Inject()(eventDAO: EventDAO) extends Controller with EventJsonHandler {

  /* refactor to jsonHeaders for seriousness */
  val effinHeaders = "Content-Type" -> "application/json; charset=utf-8"

  implicit val eventReads: Reads[Event] = getReads
  implicit val eventWrites: Writes[Event] = getWrites

  def addEvent() = Action.async(BodyParsers.parse.json) { request =>
    val eventResult = request.body.validate[Event]

    eventResult.fold(
      errors => {

        /* if it couldn't validate for event perhaps it's a list */
        val listResult = request.body.validate[List[Event]]

        listResult.fold(
          errors => {
            Future(BadRequest(Json.obj("status" -> "KOL", "message" -> JsError.toJson(errors))))
          },
          success => {

            /* update the ones who have id, create the ones who do not */
            val toCreate: List[Event] = success.filterNot(event => event.id.isDefined)
            val toUpdate: List[Event] = success.filter(event => event.id.isDefined)

            val createIds = toCreate.map {
              event => eventDAO.addEvent(event)
            }

            val updateIds = toUpdate.map {
              event => eventDAO.updateEvent(event)
            }

            val flatCreateIds = Future.sequence(createIds).map(_.flatten)
            val flatUpdateIds = Future.sequence(updateIds).map(_.flatten)

            val allIds = flatCreateIds.zip(flatUpdateIds).map {
              ids => tuple2ToList(ids).flatten
            }

            allIds.map {
              list => Created(Json.toJson(list))
            }

          }
        )

        // Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
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

  def tuple2ToList[T](t: (T,T)): List[T] = List(t._1, t._2)

  def serializeListOptions(eventId: Int) = getOptions()
  def serializeListOptionsTwo() = getOptions()

  def serializeListOptionsThree() = getOptions()
  def serializeListOptionsFour(licenseId: Int) = getOptions()
  def serializeListOptionsFive(licenseId: Int) = getOptions()
  def serializeListOptionsSix(groupId: Int) = getOptions()
  def serializeListOptionsSeven() = getOptions()
  def serializeListOptionsEight(userId: Int) = getOptions()
  def serializeListOptionsNine() = getOptions()



  def getOptions() = Action {
    Ok("").withHeaders(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
    "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type, X-Json, X-Prototype-Version, X-Requested-With",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Max-Age" -> (60 * 60 * 24).toString
    )
  }



  private def toFailureJson(message: String): Result = {
    BadRequest(Json.obj("status" -> "KO", "message" -> (message)))
  }

  /* implicit val dbEventWrites = new Writes[Event] {
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
  } */

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
//      case e: UserHasNoGroupException => toFailureJson("User has no group")
      case e: UserHasNoGroupException => NotFound("User has no group")
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

  def deleteEvent(eventId: Int) = Action.async {
    eventDAO.deleteEvent(eventId).map {
      case Some(_) => NoContent
      case None => NotFound
    }
  }
}

