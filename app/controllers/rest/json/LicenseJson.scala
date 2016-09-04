package controllers.rest.json

import model.License
import play.api.libs.functional.syntax._
import play.api.libs.json._

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

  def getWrites = licenseWrites
  def getReads = licenseReads

}
