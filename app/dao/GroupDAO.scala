package dao

import javax.inject.Inject

import model.{Group, GroupsTable, UsersTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class GroupDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Users = TableQuery[UsersTable]
  private val Groups = TableQuery[GroupsTable]

  def addGroup(group: Group): Future[Try[Int]] = {
    db.run(
      (Groups returning Groups.map(_.id) += group).asTry
    )
  }

  /* just use updategroup to inactivate it */
  def updateGroup(retrievedGroup: Group): Future[Option[Int]] = db.run {
    Groups.filter(_.id === retrievedGroup.id).update(retrievedGroup).map {
      case 0 => None
      case _ => Some(retrievedGroup.id)
    }
  }

  /* get all, get one */
  def allGroups: Future[Seq[Group]] = {
    db.run(Groups.result)
  }

  def getGroupById(groupId: Int): Future[Option[Group]] = {
    db.run(Groups.filter(_.id === groupId).result).map(group => group.headOption)
  }

  def getGroupByGroupName(groupName: String): Future[Option[Group]] = {
    val q = for {
      group <- Groups if group.groupName like "%" + groupName + "%"
    } yield group

    db.run(q.result).map {
      group => group.headOption
    }
  }

}

