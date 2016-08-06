package model

import slick.driver.MySQLDriver.api._

class UsersTable(tag: Tag) extends Table[User](tag, "user") {

  val Groups = TableQuery[GroupsTable]
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.SqlType("VARCHAR(100)"))
  def password = column[String]("password")
  def admin = column[Option[Boolean]]("isadmin")

  def groupId = column[Option[Int]]("user_group_id")
  def group = foreignKey("user_group_id", groupId, Groups)(_.id)

  override def * = (id, username, password, admin, groupId) <> (User.tupled, User.unapply)

  def idxUsername = index("idx_username", username, unique = true)
}

class GroupsTable(tag: Tag) extends Table[Group](tag, "group") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def groupName = column[String]("group_name", O.SqlType("VARCHAR(100)"))
  def active = column[Boolean]("active", O.Default(true))

  override def * = (id, groupName, active) <> (Group.tupled, Group.unapply)

  def idxGroupName = index("idx_groupName", groupName, unique = true)
}

class EventsTable(tag: Tag) extends Table[Event](tag, "event") {

  val Groups = TableQuery[GroupsTable]

  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def eventName = column[String]("event_name")

  def from = column[Long]("from")
  def to = column[Long]("to")

  def groupId = column[Int]("event_group_id")
  def group = foreignKey("event_group_id", groupId, Groups)(_.id)

  override def * = (id, eventName, from, to, groupId) <> (Event.tupled, Event.unapply)
}