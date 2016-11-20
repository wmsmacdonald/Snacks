package controllers
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

import dao.SuggestedSnacksDAO
import dao.SnacksDAO
import javax.inject.Inject

import models.{ServiceSnack, SnackDetailed, SuggestedSnack}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.libs.json._
import play.api.mvc.Cookie
import com.github.nscala_time.time.Imports._

import scala.util.{Failure, Success, Try}


class Application @Inject()(snacksDao: SnacksDAO, suggestedSnacksDao: SuggestedSnacksDAO) extends Controller {

  def index = Action.async { request =>
    for {
      detailedTry <- suggestedSnacksDao.joinSnacks()
      snacksTry <- snacksDao.all()
    } yield { (detailedTry, snacksTry) match {
        case (Success(detailed), Success(snacks)) => {
          // json web service succeeded
          val alwaysPurchased = snacks.filter(s => !s.optional)

          def parseDate(value: String) = DateTimeFormat.forPattern("MM-dd-yyyy").parseDateTime(value)
          def isRecent(dateString: String) = (new DateTime).getMonthOfYear == parseDate(dateString).getMonthOfYear
          val recent = detailed.filter(s => isRecent(s.lastSuggested) && s.optional)

          Ok(views.html.index(alwaysPurchased, recent))
        }
        // json service failed
        case _ => Ok("Web service down for maintenance. Please come back later.")
      }
    }
  }


  def shoppinglist = Action.async {
    for {
      snacksTry <- snacksDao.all()
      suggestedSnacksTry <- suggestedSnacksDao.joinSnacks()
    } yield (snacksTry, suggestedSnacksTry) match {
      case (Success(snacks), Success(suggestedSnacks)) => {
        val alwaysPurchased = snacks.filter(s => !s.optional).map(s => (s.name, s.purchaseLocations))
        val optionalSnacks = (
          suggestedSnacks.filter(s => s.optional)
          sortBy(s => s.votes)
          take(10 - alwaysPurchased.size)
          map(s => (s.name, s.purchaseLocations))
        )
        Ok(views.html.shoppinglist(alwaysPurchased ++ optionalSnacks))
      }
      case _ => Ok("Web service down for maintenance. Please come back later.")
    }
  }

  def suggestions = Action.async {
    Future {
      Ok(views.html.suggestions())
    }
  }

}