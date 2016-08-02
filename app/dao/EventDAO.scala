package dao

import javax.inject.Inject

import model.Event
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.Future

class EventDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._





}

