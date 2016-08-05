package controllers.rest

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

import com.mysql.jdbc.exceptions.jdbc4.{MySQLDataException, MySQLIntegrityConstraintViolationException}
import controllers.rest.restmodel.JsonGroup
import dao.GroupDAO
import model.Group
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller, Result}
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class GroupRestController @Inject()(groupDao: GroupDAO) extends Controller {

  /* refactor to jsonHeaders for seriousness */
  val effinHeaders = "Content-Type" -> "application/json; charset=utf-8"

  implicit val groupWrites = new Writes[JsonGroup] {
    def writes(group: JsonGroup) = Json.obj(
      "id" -> group.id.get,
      "groupName" -> group.groupName
      )
  }

  implicit val groupReads: Reads[JsonGroup] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "groupName").read[String] and
        (JsPath \ "active").readNullable[Boolean]
    )(JsonGroup.apply _)

  def addGroup() = Action.async(BodyParsers.parse.json) { request =>
    val groupResult = request.body.validate[JsonGroup]

    groupResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      group => {

        val dbGroup = group.toDbGroup

        groupDao.addGroup(dbGroup).map {
          result => {
            result match {
              case Success(res) => {
                Logger.debug("Added event: " + group.groupName)
                Created.withHeaders("Location" -> ("/groups/" + res))
              }
              case Failure(e: MySQLIntegrityConstraintViolationException) => {
                toFailureJson("Group needs to have a groupId")
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

  def allGroups() = Action.async {
    groupDao.allGroups.map {
      groups => {

        val asJsonGroups = groups.seq.map(_.toJsonGroup)

        val asJson = Json.toJson(asJsonGroups)
        Ok(asJson).withHeaders(effinHeaders)
      }
    }
  }

  def getGroupByName(groupName: String) = Action.async {
    groupDao.getGroupByGroupName(groupName).map {
      case Some(group) => Ok(Json.toJson(group.toJsonGroup))
      case None => NotFound
    }
  }

  def getGroup(groupId: Int) = Action.async {
    groupDao.getGroupById(groupId).map {
      case Some(group) => {

        val jsonGroup = group.toJsonGroup

        Ok(Json.toJson(jsonGroup))
      }
      case None => NotFound
    }
  }

  def updateGroup(groupId: Int) = Action.async(BodyParsers.parse.json) { request =>
    val groupResult = request.body.validate[JsonGroup]

    groupResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      jsongroup => {

        val dbGroup = jsongroup.toDbGroup
        groupDao.updateGroup(dbGroup).map {
          case Some(number) => Ok
          case None => {
            Logger.warn("Case none in GroupRestController.updateGroup")
            toFailureJson("No exception in updateGroup, but retrieved number was none")
          }
        } recover {
          case e: MySQLDataException => NotFound
          case e: MySQLIntegrityConstraintViolationException => toFailureJson("No such groupId")
          case e: Exception => {
            Logger.error("updateGroup(): " + e.getMessage)
            toFailureJson(e.getMessage)
          }
        }
      }

    )
  }

}

