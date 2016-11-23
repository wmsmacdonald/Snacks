package controllers

import javax.inject.Inject

import dao.{ServiceSnacksDAO, SuggestedSnacksDAO}
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.mvc.{Action, Controller, Cookie}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}


class Snacks @Inject()(serviceSnacksDao: ServiceSnacksDAO, suggestedSnacksDao: SuggestedSnacksDAO) extends Controller {

  /** records vote in database and returns JSON status with error if applicable
    * updates/set the client cookie to track snacks voted for
    * */
  def vote = Action.async { request =>
    val id: Int = request.queryString("id").head.toInt

    val votedFor: Set[Int] = request.cookies.get("votedFor") match {
      case Some(cookie) => cookie.value.split(":").map(s => s.toInt).toSet
      case None => Set()
    }

    // already voted for this snack
    if (votedFor(id)) {
      val errorJson = JsObject(Map("error" -> JsString("you already voted for this snack.")))
      Future (Ok(errorJson))
    }
    // have used all votes
    else if (votedFor.size >= 3) {
      val errorJson = JsObject(Map("error" -> JsString("you have no remaining votes.")))
      Future (Ok(errorJson))
    }
    else {
      val a = suggestedSnacksDao.vote(id)
      val successJson = JsObject(Map("error" -> JsBoolean(false)))

      val newVotedFor = votedFor + id
      // return status with update cookie
      a.map(r => Ok(successJson).withCookies(Cookie("votedFor", newVotedFor.mkString(":"), httpOnly = false)))
    }
  }

  /** suggests snack that already exists in the web service and return JSON status */
  def suggestExisting = Action.async { implicit request =>
    val id = request.body.asFormUrlEncoded.get("id").head
    suggestedSnacksDao.suggest(id.toInt).map(r => Ok(JsObject(Map("error" -> JsBoolean(false)))))
  }

  /** suggests snack that does not exist in the web service and returns JSON status with error if applicable */
  def suggestNew = Action.async { implicit request =>
    val name = request.body.asFormUrlEncoded.get("name").head
    val location = request.body.asFormUrlEncoded.get("location").head
    serviceSnacksDao.create(name, location).flatMap({
      case Left(snack) => {
        suggestedSnacksDao.suggest(snack.id).map(r => Ok(JsObject(Map("error" -> JsBoolean(false)))))
      }
      case Right(error) => Future.successful(Ok(JsObject(Map("error" -> JsString(error)))))
    })
  }
}
