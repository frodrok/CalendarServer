package controllers.rest.json

import controllers.rest.restmodel.JsonUser
import model.User
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

trait UserJson {

  val userReads: Reads[JsonUser] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "admin").readNullable[Boolean] and
      (JsPath \ "groupId").readNullable[Int] and
      (JsPath \ "superAdmin").readNullable[Boolean] and
      (JsPath \ "licenseId").readNullable[Int]
    )(JsonUser.apply _)

  val userWrites = new Writes[User] {
    def writes(user: User) = Json.obj(
      "id" -> user.id,
      "username" -> user.username,
      "password" -> user.password,
      "admin" -> user.admin,
      "groupId" -> user.groupId,
      "superAdmin" -> user.superAdmin,
      "licenseId" -> user.licenseId
    )
  }

  def getUserReads = userReads
  def getUserWrites = userWrites

}
