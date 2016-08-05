package model

import controllers.rest.restmodel.JsonGroup

case class User(id: Long, username: String, password: String,
                admin: Option[Boolean] = None,
                groupId: Option[Int] = None)

case class Group(id: Int, groupName: String = "", active: Boolean) {
  def toJsonGroup: JsonGroup = {
    JsonGroup(Some(this.id), this.groupName, Some(this.active))
  }
}

case class Event(id: Option[Int], eventName: String, from: Long, to: Long, groupId: Int)

case class UserNotFoundException(s: String) extends Exception
case class EventNotFoundException(s: String) extends Exception

case class UserHasNoGroupException(s: String) extends Exception



