package controllers.rest

import javax.inject.Inject

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.rest.json.{LicenseJson, UserJson}
import controllers.rest.restmodel.JsonUser
import dao.LicenseDAO
import model.{License, User}
import play.api.Logger
import play.api.libs.json.{JsError, Json, Reads, Writes}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LicenseRestController @Inject()(licenseDAO: LicenseDAO) extends Controller with LicenseJson with UserJson {

  /* json readers and writers */
  implicit val reads: Reads[License] = getReads
  implicit val writes: Writes[License] = getWrites

  implicit val userReader: Reads[JsonUser] = userReads
  implicit val userWriter: Writes[User] = userWrites

  def addLicense = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[License]

    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      license => {
        val licenseToDb = License(license.id, license.companyName)

        licenseDAO.addLicense(licenseToDb).map {
          result => {
            Logger.debug("Added license: " + license.companyName)

            Created.withHeaders("Location" -> ("/licenses/" + result))
          }
        } recover {
          case e: MySQLIntegrityConstraintViolationException => {
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

  def addAdmin(licenseId: Int) = Action.async(BodyParsers.parse.json) { request =>
    val userResult = request.body.validate[JsonUser]

    userResult.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
      },
      jsonUser => {

        val withId = jsonUser.id match {
          case None => 0
          case Some(int) => int.toLong
        }

        val isAdmin: Option[Boolean] = jsonUser.admin match {
          case None => Some(true)
          case Some(bool) => Some(bool)
        }

        val superAdmin = Some(false)

        val userToDb = User(withId, jsonUser.username, jsonUser.password, isAdmin, jsonUser.groupId, superAdmin, jsonUser.licenseId)

        licenseDAO.addAdmin(userToDb, licenseId).map {
          result => Created
        } recover {
          case ex: Exception => BadRequest
        }
      }
    )
  }

  def allLicenses = Action.async {
    licenseDAO.allLicenses().map {
      licenses => Ok(Json.toJson(licenses))
    }
  }

  def getLicense(id: Int) = Action.async {
    licenseDAO.getLicense(id).map {
      case Some(license) => Ok(Json.toJson(license))
      case None => NotFound
    }
  }

  def adminsForLicense(licenseId: Int) = Action.async {
    licenseDAO.getAdminsForLicense(licenseId).map {
      admins => Ok(Json.toJson(admins))
    }
  }

}
