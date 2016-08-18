package dao

import javax.inject.Inject

import model.{License, LicenseTable, User, UsersTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by frodrok on 2016-08-17.
  */
class LicenseDAO @Inject()(@NamedDatabase("msql") val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Licenses = TableQuery[LicenseTable]
  private val Users = TableQuery[UsersTable]

  def addLicense(license: License): Future[Option[Int]] = {
    db.run(
      (Licenses returning Licenses.map(_.id)) += license
    )
  }

  def allLicenses(): Future[Seq[License]] = {
    db.run(
      Licenses.result
    )
  }

  def getLicense(licenseId: Int): Future[Option[License]] = {
    val idQuery = for {
      l <- Licenses if l.id === licenseId
    } yield l

    db.run(idQuery.result).map(license => license.headOption)
  }

  def getAdminsForLicense(licenseId: Int): Future[Seq[User]] = {
    val adminQuery = Users.filter(_.licenseId === licenseId).result
    db.run(adminQuery)
  }

  def addAdmin(user: User, licenseId: Int): Future[Option[Long]] = {
    db.run(
      (Users returning Users.map(_.id)).insertOrUpdate(user)
    )
  }


}
