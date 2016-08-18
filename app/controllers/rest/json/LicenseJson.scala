package controllers.rest.json

import controllers.rest.restmodel.JsonUser
import model.License
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by frodrok on 2016-08-17.
  */
trait LicenseJson {

  val licenseReads: Reads[License] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "company").read[String]
    )(License.apply _)


  val licenseWrites = new Writes[License] {
    def writes(license: License) = Json.obj(
      "id" -> license.id,
      "company" -> license.companyName
    )
  }

  def getWrites() = licenseWrites
  def getReads() = licenseReads

}
