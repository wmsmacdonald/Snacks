package controllers

import javax.inject.Inject

import dao.{ServiceSnacksDAO, SuggestedSnacksDAO}
import models.{ServiceSnack, SuggestedSnack}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.mvc.{Action, Controller, Cookie}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}


class Snacks @Inject()(serviceSnacksDao: ServiceSnacksDAO, suggestedSnacksDao: SuggestedSnacksDAO) extends Controller {
  def vote = Action.async { request =>
    val id: Int = request.queryString("id").head.toInt

    val votedFor: Set[Int] = request.cookies.get("votedFor") match {
      case Some(cookie) => cookie.value.split(":").map(s => s.toInt).toSet
      case None => Set()
    }

    if (votedFor.size < 3 && !votedFor(id)) {
      val a = suggestedSnacksDao.vote(id)
      val successJson = JsObject(Map("error" -> JsBoolean(false)))

      val newVotedFor = votedFor + id

      a.map(r => Ok(successJson).withCookies(Cookie("votedFor", newVotedFor.mkString(":"), httpOnly = false)))
    }
    else {
      val errorJson = JsObject(Map("error" -> JsString("You already voted for this snack")))
      Future {
        Ok(errorJson)
      }
    }
  }

  def create = Action.async { implicit request =>
    val name = request.body.asFormUrlEncoded.get("name").head
    val location = request.body.asFormUrlEncoded.get("location").head
    serviceSnacksDao.create(name, location).map({
      case Left(snack) => {
        suggestedSnacksDao.suggest(snack.id)
        Ok(JsObject(Map("error" -> JsBoolean(false))))
      }
      case Right(error) => Ok(JsObject(Map("error" -> JsString(error))))
    })
  }
}
