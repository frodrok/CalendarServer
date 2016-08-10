package `trait`

import model.Event
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

/**
  * Created by frodrok on 09/08/16.
  */
trait EventJsonHandler {

  val writes = new Writes[Event] {
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
        "background" -> event.background,
        "color" -> event.color
      )
    }
  }

  val reads: Reads[Event] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "title").read[String] and
      (JsPath \ "from").read[Long] and
      (JsPath \ "to").readNullable[Long] and
      (JsPath \ "groupId").read[Int] and
      (JsPath \ "background").readNullable[Boolean] and
      (JsPath \ "color").readNullable[String]
    ) (Event.apply _)

  def getWrites = {
    writes
  }

  def getReads = {
    reads
  }

}
