package controllers

import javax.inject.Inject

import dao.{ServiceSnacksDAO, SuggestedSnacksDAO}
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.mvc.{Action, Controller, Cookie}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}


class Snacks @Inject()(serviceSnacksDao: ServiceSnacksDAO, suggestedSnacksDao: SuggestedSnacksDAO) extends Controller {
  def vote = Action.async { request =>
    val id = request.queryString("id").head.toInt

    val votedFor: Set[Int] = request.cookies.get("votedFor") match {
      case Some(cookie) => cookie.value.split(",").map(s => s.toInt).toSet
      case None => Set()
    }

    if (votedFor.size < 3 && !votedFor(id)) {
      val a = suggestedSnacksDao.vote(id)
      val successJson = JsObject(Map("error" -> JsBoolean(false)))
      a.map(r => Ok(successJson).withCookies(Cookie("votedFor", id.toString, httpOnly = false)))
    }
    else {
      val errorJson = JsObject(Map("error" -> JsString("You already voted for this snack")))
      Future {
        Ok(errorJson)
      }
    }
  }

  def create = Action.async { request =>
    val name = request.body.asFormUrlEncoded.get("name").map(_.head).toString()
    val location = request.body.asFormUrlEncoded.get("location").map(_.head).toString()
    serviceSnacksDao.all().map({
      case Success(serviceSnacks) => {
        val names = serviceSnacks.map(s => s.name).toSet
        if (names(name)) {
          Ok("")
        }
        else {
          println("asdfas")
          Ok("")
        }
      }
      case Failure(e) => Ok("bad")
    })
  }
}
