package controllers.rest

import javax.inject.Inject
import javax.inject.Inject

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.rest.restmodel.JsonUser
import model.{User, UserHasNoGroupException, UserNotFoundException}
import dao.UserDAO
import org.joda.time.DateTime
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, BodyParsers, Controller, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import play.api.libs.json._
import play.api.i18n.Messages.Implicits._
import play.api.libs.functional.syntax._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class UserRestController @Inject()(userDao: UserDAO) extends Controller {

  implicit val userReads: Reads[JsonUser] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "admin").readNullable[Boolean] and
      (JsPath \ "groupId").readNullable[Int]
    )(JsonUser.apply _)

  implicit val userWrites = new Writes[JsonUser] {
    def writes(user: JsonUser) = Json.obj(
      "id" -> user.id,
      "username" -> user.username,
      "password" -> user.password,
      "admin" -> user.admin,
      "groupId" -> user.groupId
    )
  }

  def getUser(userId: Int) = Action {
    val userOption = Await.result(userDao.getUserById(userId), 3.seconds)

    val jsonUserOption = userOption.map {
      user => Some(JsonUser(Some(user.id.toInt), user.username, user.password, user.admin, user.groupId))
    }

    jsonUserOption match {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
  }

  def allUsers = Action.async {
    /* val jsonUsers = Await.result(userDao.allUsers, 3.seconds).map {
      user => JsonUser(Some(user.id.toInt), user.username, user.password, user.admin, user.groupId)
    }

    val  asJson = Json.toJson(jsonUsers) */

    userDao.allUsers.map {
      user => {
        val asJsonUsers = user.seq.map {
          user => JsonUser(Some(user.id.toInt), user.username, user.password, user.admin, user.groupId)
        }
        val asJsonData = Json.toJson(asJsonUsers)
        Ok(asJsonData)
      }
    }

    // Ok(asJson).withHeaders("Content-Type" -> "application/json; charset=utf-8")
  }

  def register() = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[JsonUser]
    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      user => {
        val userToDb = User(0, user.username, user.password, user.admin, user.groupId)

        userDao.add(userToDb).map {
          result => {
            result match {
              case Success(res) => {
                Logger.debug("Added user " + user.username)

                Created.withHeaders("Location" -> ("/user/" + res))

              }
              case Failure(e: MySQLIntegrityConstraintViolationException) => {
                // BadRequest(Json.obj("status" -> "KO", "message" -> ("Username already exists, use update instead")))
                Conflict
              }
              case Failure(e: Exception) => {
                Logger.error(e.toString)
                BadRequest(Json.obj("status" -> "KO", "message" -> (e.getMessage)))
              }
            }
          }
        }

      }
    )
  }

  def update(userId: Int) = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[JsonUser]

    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      user => {
        val userToDb = User(user.id.get, user.username, user.password, user.admin, user.groupId)

        userDao.update(userToDb).map {
          result => {
            result match {
              case Success(res) => {
                Logger.debug("Result: " + res)
                Ok
              }
              case Failure(e: UserNotFoundException) => {
                NotFound
              }
              case Failure(e: MySQLIntegrityConstraintViolationException) => {
                BadRequest(Json.obj("status" -> "KO", ("message" -> "No such groupId")))
              }
              case Failure(e: Exception) => {
                BadRequest("oh shit son, fall through in UserRestController.update")
              }
            }
          }
        }
      }

    )

  }
}
