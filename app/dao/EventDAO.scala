package dao

import javax.inject.Inject

import com.mysql.jdbc.exceptions.jdbc4.MySQLDataException
import model._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}



class EventDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider, userDAO: UserDAO) extends HasDatabaseConfigProvider[JdbcProfile] {
//class EventDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider, userDAO: UserDAO) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Events = TableQuery[EventsTable]

  def allEvents: Future[Seq[Event]] = {
    db.run(Events.result)
  }

  def addEvent(event: Event): Future[Option[Int]] = {
    db.run(
      (Events returning Events.map(_.id)) += event
    )
  }

  def updateEvent(retrievedEvent: Event): Future[Option[Int]] = {
    db.run(
      Events.filter(_.id === retrievedEvent.id).update(retrievedEvent).map {
        case 0 => None
        case 1 => {
          retrievedEvent.id
        }
      }
    )
  }

  def getEventById(eventId: Int): Future[Option[Event]] = {
    db.run(Events.filter(_.id === eventId).result).map(event => event.headOption)
  }

  def getEventByName(eventName: String): Future[Option[Event]] = {
    val q = for {
      event <- Events if event.eventName like "%" + eventName + "%"
    } yield event

    db.run(q.result).map {
      eventSeq => eventSeq.headOption
    }
  }

  def getEventsForGroup(groupId: Int): Future[Seq[Event]] = {
    /* val q = for {
      e <- Events if e.groupId === groupId
    } yield (e) unused, could be used */

    val extraquery = Events.filter(_.groupId === groupId).result

    db.run(extraquery)
  }

  def getEventsForUser(userId: Int): Future[Seq[Event]] = {
    userDAO.getUserById(userId).flatMap {
      case None => throw UserNotFoundException("")
      case Some(user) => user.groupId match {
          case None => throw UserHasNoGroupException("")
          case Some(groupId) => getEventsForGroup(groupId)
      }
    }
  }


  def deleteEvent(eventId: Int): Future[Option[Int]] = {
    /* val q = Events.filter(_.id === eventId)
    val action = q.delete
    db.run(action)*/

    db.run(
      Events.filter(_.id === eventId).delete.map {
        case 0 => None
        case 1 => Some(eventId)
      }
    )
  }




}

