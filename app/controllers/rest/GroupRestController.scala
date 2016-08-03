package controllers.rest

import javax.inject.Inject

import dao.GroupDAO
import play.api.mvc.Controller

class GroupRestController @Inject()(groupDao: GroupDAO) extends Controller {

}
