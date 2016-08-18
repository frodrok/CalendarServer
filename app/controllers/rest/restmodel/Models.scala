package controllers.rest.restmodel

import model.Group

case class JsonUser(id: Option[Int], username: String, password: String, admin: Option[Boolean], groupId: Option[Int],
                    superAdmin: Option[Boolean],
                    licenseId: Option[Int])
case class JsonGroup(id: Option[Int], groupName: String, active: Option[Boolean]) {

  def toDbGroup: Group = {
    val dbId = id match {
      case None => 0
      case Some(identity) => identity
    }

    val activeGroup = active match {
      case None => true
      case Some(isActive) => isActive
    }

    Group(dbId, this.groupName, activeGroup)
  }
}



