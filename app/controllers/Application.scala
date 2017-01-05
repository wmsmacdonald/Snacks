package controllers

import dao.SuggestedSnacksDAO
import dao.ServiceSnacksDAO
import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import play.api.mvc.Controller
import com.github.nscala_time.time.Imports._
import models.SnackDetailed

import scala.util.{Failure, Success}

class Application @Inject()(serviceSnackDao: ServiceSnacksDAO, suggestedSnacksDao: SuggestedSnacksDAO) extends Controller {

  /** returns view that shows always puchased and suggested snacks for voting */
  def index = Action.async { request =>
    for {
      // get suggestedSnacks joined with the web service snacks on id
      suggestedSnacksTry <- suggestedSnacksDao.joinServiceSnacks()
      // get snacks from the web service
      serviceSnacksTry <- serviceSnackDao.all()
    } yield (suggestedSnacksTry, serviceSnacksTry) match {
        // json web service succeeded
        case (Success(suggestedSnacks), Success(serviceSnacks)) => {
          val alwaysPurchased = serviceSnacks.filter(s => !s.optional)

          def parseDate(value: String) = DateTimeFormat.forPattern("MM-dd-yyyy").parseDateTime(value)
          def isRecent(dateString: String) = (new DateTime).getMonthOfYear == parseDate(dateString).getMonthOfYear

          // get suggested snacks for this month only
          val recent = suggestedSnacks.filter(s => isRecent(s.lastSuggested) && s.optional)

          Ok(views.html.index(alwaysPurchased, recent))
        }
        // json service failed
        case _ => Ok(views.html.error())
    }
  }

  /** returns view that shows snacks that should be bought this month **/
  def shoppinglist = Action.async {
    for {
      suggestedSnacksTry <- suggestedSnacksDao.joinServiceSnacks()
      serviceSnacksTry <- serviceSnackDao.all()
    } yield (serviceSnacksTry, suggestedSnacksTry) match {
      case (Success(serviceSnacks), Success(suggestedSnacks)) => {
        // get names and purchase locations for always purchased snacks
        // convert to name, location tuples
        val alwaysPurchased = serviceSnacks.filter(s => !s.optional).map(s => (s.name, s.purchaseLocations))

        val optionalSnacks = suggestedSnacks.filter(_.optional)
        val topOptional = optionalSnacks.sortBy(_.votes).reverse.take(10 - alwaysPurchased.size)
        // convert to name, location tuples
        val topOptionalTuples = topOptional.map(s => (s.name, s.purchaseLocations))

        // combine always purchased and optional lists
        Ok(views.html.shoppinglist(alwaysPurchased ++ topOptionalTuples))
      }
      case _ => Ok(views.html.error())
    }
  }

  /** returns view that shows the suggestions submission page */
  def suggestions = Action.async {
    for {
      serviceSnacksTry <- serviceSnackDao.all()
      suggestedSnacks <- suggestedSnacksDao.all()
    } yield serviceSnacksTry match {
      case Success(serviceSnacks) => {
        val suggestedIds = suggestedSnacks.map(s => s.id).toSet
        // only allow snacks that haven't been suggested and are optional
        val suggestable = serviceSnacks.filter(s => s.optional && !suggestedIds(s.id))
        Ok(views.html.suggestions(suggestable))
      }
      case Failure(e) => Ok(views.html.error())
    }
  }

}
