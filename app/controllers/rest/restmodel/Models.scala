package controllers.rest.restmodel

import org.joda.time.DateTime

case class JsonUser(id: Option[Int], username: String, password: String, admin: Option[Boolean], groupId: Option[Int])




