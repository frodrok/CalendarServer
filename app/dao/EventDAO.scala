package dao

import javax.inject.Inject

import model._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

import scala.concurrent.duration._



class EventDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider, userDAO: UserDAO) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Events = TableQuery[EventsTable]

  def addEvent(event: Event): Future[Option[Int]] = {
    db.run(
      (Events returning Events.map(_.id)) += event
    )
  }

  def updateEvent(retrievedEvent: Event): Future[Option[Int]] = db.run {
    Events.filter(_.id === retrievedEvent.id).update(retrievedEvent).map {
      case 0 => None
      case _ => retrievedEvent.id
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
    /* TODO: change to 100% asynchronous code, userDAO.getUserById(userId).map { etc } */
    val user = Await.result(userDAO.getUserById(userId), 3.seconds)
    user match {
      case Some(user) => {
        /* getEventsForGroup(user.groupId.get) */
        user.groupId match {
          case Some(groupId) => getEventsForGroup(groupId)
          case None => throw UserHasNoGroupException("User with id: " + userId + " has no group")
        }
      }
      case None => throw UserNotFoundException("User with id: " + userId + " not found")
    }
  }

  def deleteEvent(eventId: Int): Future[Int] = {
    val q = Events.filter(_.id === eventId)
    val action = q.delete
    db.run(action)
  }




}

