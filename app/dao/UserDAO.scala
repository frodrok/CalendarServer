package dao

import javax.inject.Inject

import model._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

class UserDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Users = TableQuery[UsersTable]
  private val Groups = TableQuery[GroupsTable]
  private val Events = TableQuery[EventsTable]

  def setup() {
    db.run(DBIO.seq(
      Groups.schema.create
    )).onFailure{ case ex => Logger.debug(s"error in dbSetup groups: $ex.getMessage") }

    Thread sleep 2000

    db.run(DBIO.seq(
      Events.schema.create
    )).onFailure{ case ex => Logger.debug(s"error in dbSetup events: $ex.getMessage") }

    Thread sleep 2000

    db.run(DBIO.seq(
      Users.schema.create
    )).onFailure{ case ex => Logger.debug(s"error in dbSetup user: $ex.getMessage") }


  }

  def getUserById(id: Long): Future[Option[User]] = {
    val idQuery = for {
      u <- Users if u.id === id
    } yield u

    db.run(idQuery.result).map(user => user.headOption)
  }

  def updateUser(newUser: User): Future[Option[Long]] = db.run {
    Users.filter(_.id === newUser.id).update(newUser).map {
      case 0 => None
      case _ => Some(newUser.id)
    }
  }

  def add(user: User): Future[Try[Long]] = {
    db.run(
      ((Users returning Users.map(_.id)) += user).asTry
    )/* .map {
      result => {
        result match {
          case Success(res) => {Logger.debug("Added user " + user.username)
            0L
          }
          case Failure(e: Exception) => {Logger.error(e.getMessage)
          0L}
        }
      } */
  }

  def exists(id : Long, username : String): Future[Boolean] = {
    db.run(Users.filter(i => i.id === id || i.username === username).exists.result)
  }

  def update(user: User): Future[Option[Long]] = {
    val wat = Await.result(exists(user.id, user.username), 3.seconds)

    if (wat) {
      val q1 = Users.filter(_.id === user.id).update(user).map(int => Some(int.toLong))
      db.run(q1)
    } else {
      throw UserNotFoundException("")
    }

    /* i do not know why this does not work
    exists(user.id, user.username).map {
      case false => {
        throw new UserNotFoundException("")
      }
      case true => {
        val q1 = Users.filter(_.id === user.id).update(user).map(_.toLong).asTry
        db.run(q1)
      }
    } */
  }


  def allUsers: Future[Seq[User]] = db.run(Users.result)

  def getUserByGroup(retrievedGroupName: String): Future[Seq[User]] = {
    val userByGroupName = for {
      (user, group) <- Users join Groups on (_.groupId === _.id) if group.groupName === retrievedGroupName
    } yield user

    db.run(userByGroupName.result)
  }

  /* overloaded from above */
  def getUserByGroup(retrievedGroupId: Int): Future[Seq[User]] = {
    val userByGroupName = for {
      (user, group) <- Users join Groups on (_.groupId === _.id) if group.id === retrievedGroupId
    } yield user

    db.run(userByGroupName.result)
  }

  def getUserByUsername(retrievedUsername: String): Future[Option[User]] = {
    val q = for {
    q <- Users if q.username === retrievedUsername
    } yield q

    db.run(q.result).map {
      seq => seq.headOption
    }
  }



}