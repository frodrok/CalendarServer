package controllers.rest.restmodel

import model.Group
import org.joda.time.DateTime

case class JsonUser(id: Option[Int], username: String, password: String, admin: Option[Boolean], groupId: Option[Int])
case class JsonGroup(id: Option[Int], groupName: String, active: Option[Boolean]) {

  def toDbGroup: Group = {
    val dbId = id match {
      case None => 0
      case Some(id) => id
    }

    val activeGroup = active match {
      case None => true
      case Some(active) => active
    }

    Group(dbId, this.groupName, activeGroup)
  }
}



