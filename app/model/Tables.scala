package model

import slick.driver.MySQLDriver.api._

class UsersTable(tag: Tag) extends Table[User](tag, "user") {

  val Groups = TableQuery[GroupsTable]
  val Licenses = TableQuery[LicenseTable]

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.SqlType("VARCHAR(100)"))
  def password = column[String]("password")
  def admin = column[Option[Boolean]]("isadmin")
  def superAdmin = column[Option[Boolean]]("superadmin")

  def groupId = column[Option[Int]]("user_group_id")
  def group = foreignKey("user_group_id", groupId, Groups)(_.id.?)

  def licenseId = column[Option[Int]]("user_license_id")
  def license = foreignKey("user_license_id", licenseId, Licenses)(_.id)

  override def * = (id, username, password, admin, groupId, superAdmin, licenseId) <> (User.tupled, User.unapply)

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
  def to = column[Option[Long]]("to")

  def background = column[Option[Boolean]]("background")
  def color = column[Option[String]]("color")

  def groupId = column[Int]("event_group_id")
  def group = foreignKey("event_group_id", groupId, Groups)(_.id)

  override def * = (id, eventName, from, to, groupId, background, color) <> (Event.tupled, Event.unapply)
}

class LicenseTable(tag: Tag) extends Table[License](tag, "license") {

  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def companyName = column[String]("companyName")

  override def * = (id, companyName) <> (License.tupled, License.unapply)
}