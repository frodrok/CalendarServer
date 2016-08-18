

package controllers.rest

import javax.inject.Inject

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.rest.restmodel.JsonUser
import dao.UserDAO
import model.{User, UserNotFoundException}
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class UserRestController @Inject()(userDao: UserDAO) extends Controller {

  implicit val userReads: Reads[JsonUser] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "admin").readNullable[Boolean] and
      (JsPath \ "groupId").readNullable[Int] and
        (JsPath \ "superAdmin").readNullable[Boolean] and
          (JsPath \ "licenseId").readNullable[Int]
    )(JsonUser.apply _)

   implicit val userWrites = new Writes[JsonUser] {
      def writes(user: JsonUser) = Json.obj(
        "id" -> user.id,
        "username" -> user.username,
        "password" -> user.password,
        "admin" -> user.admin,
        "groupId" -> user.groupId,
        "superAdmin" -> user.superAdmin,
        "licenseId" -> user.licenseId
      )
    }

  private def userToJsonUser(user: User): JsonUser = {
    JsonUser(Some(user.id.toInt), user.username, user.password, user.admin, user.groupId, user.superAdmin, user.licenseId)
  }

  def getUser(userId: Int) = Action.async { implicit request =>
    val userIdLong = userId.toLong
    val userOptionFuture = userDao.getUserById(userIdLong)

    userOptionFuture.map {
      userOption => userOption.map {
            user => userToJsonUser(user)
          } match {
            case Some(jsonUser) => Ok(Json.toJson(jsonUser))
            case None => NotFound
          }
        }
  }


  def getUserByUsername(username: String) = Action.async {
    val userOptionFuture = userDao.getUserByUsername(username)

    userOptionFuture.map {
      userOption => userOption.map {
        user => userToJsonUser(user)
      } match {
        case Some(jsonUser) => Ok(Json.toJson(jsonUser))
        case None => NotFound
      }
    }
  }

  def login = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[JsonUser]

    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      user => {
        val temp = User(0, user.username, user.password, superAdmin = user.superAdmin)

        userDao.getUserByUsername(temp.username).map {

          userOption => {
            if (userOption.isDefined) {
              loginUser(temp, userOption.get) match {
                case false => {
                  BadRequest(Json.obj("status" -> "KO", "message" -> "login failed"))
                }
                case true => Ok
              }
            } else {
              BadRequest(Json.obj("status" -> "KO", "message" -> ("login failed")))
            }

          }
        }
      }
    )

  }

  private def loginUser(user: User, dbUser: User): Boolean = {
    dbUser.username == user.username && dbUser.password == user.password
  }

  def allUsers = Action.async {

    userDao.allUsers.map {
      user => {
        val asJsonUsers = user.seq.map {
          user => JsonUser(Some(user.id.toInt), user.username, user.password, user.admin, user.groupId, user.superAdmin, user.licenseId)
        }
        val asJsonData = Json.toJson(asJsonUsers)
        Ok(asJsonData)
      }
    }
  }

  def register() = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[JsonUser]
    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      user => {
        val userToDb = User(0, user.username, user.password, user.admin, user.groupId, user.superAdmin, user.licenseId)

        userDao.add(userToDb).map {
          result => {
            Logger.debug("Added user: " + user.username)

            Created.withHeaders("Location" -> ("/users/" + result))
          }
        } recover {
          case e: MySQLIntegrityConstraintViolationException => {
            // BadRequest(Json.obj("status" -> "KO", "message" -> ("Username already exists, use update instead")))
            Conflict
          }
          case e: Exception => {
            Logger.error(e.toString)
            BadRequest(Json.obj("status" -> "KO", "message" -> e.getMessage))
          }
        }

      }
    )
  }

  def allUsersForGroup(groupId: Int) = Action.async {
    userDao.getUserByGroup(groupId).map(users => {
      val asJson = users.map(_.toJsonUser)
      Ok(Json.toJson(asJson))
    })
  }

  def update(userId: Int) = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[JsonUser]

    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      user => {
        val userToDb = User(user.id.get, user.username, user.password, user.admin, user.groupId, user.superAdmin, user.licenseId)

        userDao.updateUser(userToDb).map {
          result => result match {
            case None => NotFound
            case Some(result) => Ok
          }
        } recover {
          case e: UserNotFoundException => NotFound
          case e: MySQLIntegrityConstraintViolationException => Conflict
          case e: Exception => {
            Logger.debug("fallthrough [UserREstController.update")
            InternalServerError(e.getMessage)
          }
        }
      }

    )

  }
}
