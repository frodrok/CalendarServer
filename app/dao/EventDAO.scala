package dao

import javax.inject.Inject

import com.mysql.jdbc.exceptions.jdbc4.MySQLDataException
import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}



class EventDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider, userDAO: UserDAO) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Events = TableQuery[EventsTable]

  def allEvents: Future[Seq[Event]] = {
    db.run(Events.result)
  }

  def addEvent(event: Event): Future[Option[Int]] = {
    db.run(
      ((Events returning Events.map(_.id)) += event)
    )
  }

  def updateEvent(retrievedEvent: Event): Future[Int] = db.run {
    /* investigate whentf exception happens?? */
    Events.filter(_.id === retrievedEvent.id).update(retrievedEvent).map {
      case 0 => throw new MySQLDataException("Could could not update wadafaka is this, L:36 in EventDAO")
      case _ => retrievedEvent.id.get
    }
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


  def deleteEvent(eventId: Int): Future[Int] = {
    val q = Events.filter(_.id === eventId)
    val action = q.delete
    db.run(action)
  }




}

