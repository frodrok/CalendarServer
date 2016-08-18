package model

import controllers.rest.restmodel.{JsonGroup, JsonUser}

case class User(id: Long, username: String, password: String,
                admin: Option[Boolean] = None,
                groupId: Option[Int] = None, superAdmin: Option[Boolean],
                licenseId: Option[Int] = None) {
  def toJsonUser: JsonUser = {
    JsonUser(Some(id.toInt), username, password, admin, groupId, superAdmin, licenseId)
  }
}

case class Group(id: Int, groupName: String = "", active: Boolean) {
  def toJsonGroup: JsonGroup = {
    JsonGroup(Some(this.id), this.groupName, Some(this.active))
  }
}

case class Event(id: Option[Int], eventName: String, from: Long, to: Option[Long], groupId: Int, background: Option[Boolean] = Some(false),
                 color: Option[String])

case class License(id: Option[Int], companyName: String)

case class UserNotFoundException(s: String) extends Exception
case class EventNotFoundException(s: String) extends Exception

case class UserHasNoGroupException(s: String) extends Exception



