package controllers.rest.restmodel

import org.joda.time.DateTime

case class JsonEvent(id: Option[Int], eventName: String, from: String, to: Option[String], groupId: Int) {

  val fromDate: DateTime = new DateTime(from)
  val toDateOption: Option[DateTime] = to match {
    case Some(date) =>
      date match {
        case "None" => None
        case _ => Some(new DateTime(date))
      }
    case None => None
  }

  def apply(id: Option[Int], eventName: String, from: String, to: Option[String], groupId: Int): JsonEvent = JsonEvent(id, eventName, from, to, groupId)

  def unapply(arg: JsonEvent): Option[(Option[Int], String, String, Option[String], Int)] = Some(arg.id, arg.eventName, arg.from, arg.to, arg.groupId)

}
