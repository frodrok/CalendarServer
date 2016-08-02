package controllers.rest.restmodel

/**
  * Created by frodrok on 01/08/16.
  */
case class JsonUser(id: Option[Int], username: String, password: String, admin: Option[Boolean], groupId: Option[Int])
