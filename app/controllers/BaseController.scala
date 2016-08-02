package controllers

import javax.inject.Inject

import dao.UserDAO
import model.{Event, Group, User}
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class BaseController @Inject()(val messagesApi: MessagesApi, userDao: UserDAO) extends Controller with I18nSupport {

  val registerUserForm: Form[UserRegisterData] = Form(
    mapping(
      "username" -> text,
      "password" -> text,
      "isadmin" -> boolean,
      "groupid" -> optional(number)
    )(UserRegisterData.apply)(UserRegisterData.unapply)
  )

  val loginUserForm: Form[UserFormData] = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(UserFormData.apply)(UserFormData.unapply)
  )

  def index = Action { implicit request =>
    Ok(views.html.index("YH3CL kalender app")(registerUserForm)(loginUserForm))
  }

  def register = Action { implicit request =>
    registerUserForm.bindFromRequest.fold(
      formWithErrors => {
        println(formWithErrors)
        Ok("registerform error")
      },
      userData => {
        val newUser = User(0, userData.username, userData.password, Some(userData.isAdmin), userData.groupId)
        userDao.add(newUser).onFailure { case ex => println("could not save user: " + ex.getMessage)}
        Redirect("/")
      }
    )

  }

  def login = Action { implicit request =>
    loginUserForm.bindFromRequest.fold(
      formWithErrors => {
        Ok("loginform error")
      },
      userData => {
        val user = User(0, userData.username, userData.password)

        val dbUser = Await.result(userDao.getUserByUsername(user.username), Duration.Inf)

        if (loginUser(user, dbUser))
          Redirect("/user").withSession("connected" -> dbUser.username)
        else BadRequest("login failed")
      }
    )
  }

  private def loginUser(user: User, dbUser: User): Boolean = {
    dbUser.username == user.username && dbUser.password == user.password
  }

  def getUsers = Action.async {
    userDao.allUsers.map(
      users => Ok(views.html.allusers(users))
    )
  }

  def setup = Action {
    userDao.setup
    Ok("db initiated")
  }

  def test = Action {
    Ok("test")

  }

}

case class UserRegisterData(username: String, password: String, isAdmin: Boolean, groupId: Option[Int])
case class UserFormData(username: String, password: String)

