package controllers
import javax.inject.Inject

import dao.SnackDAO
import javax.inject.Inject
import models.Snack
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller


class Application @Inject()(snackDao: SnackDAO) extends Controller {

  def index = Action.async {
    val f = snackDao.all()
    f.map(snacks => Ok(views.html.index(snacks.head.name)))
  }

}