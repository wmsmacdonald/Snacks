package dao
import scala.concurrent.Future
import javax.inject.Inject

import models.{ServiceSnack, SnackDetailed, SuggestedSnack}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.util.{Failure, Success, Try}

trait SuggestedSnacksComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class SuggestedSnacks(tag: Tag) extends Table[SuggestedSnack](tag, "SUGGESTED_SNACKS") {
    def id = column[Int]("ID")
    def dateSuggested = column[String]("LAST_SUGGESTED")
    def votes = column[Int]("VOTES")
    def * = (id, dateSuggested, votes) <> (SuggestedSnack.tupled, SuggestedSnack.unapply _)
  }
}

class SuggestedSnacksDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, serviceSnacksDao: ServiceSnacksDAO)
  extends SuggestedSnacksComponent
  with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val suggestedSnacks = TableQuery[SuggestedSnacks]

  /** return all snacks in the table */
  def all(): Future[Seq[SuggestedSnack]] = db.run(suggestedSnacks.result)

  /** add snack to suggested list if not already there, and update suggested date, and return the status */
  def suggest(id: Int) = {
    val format = DateTimeFormat.forPattern("MM-dd-yyyy")
    val today = format.print(new LocalDate())
    val suggestedSnack = SuggestedSnack(id, today, 0)
    db.run((suggestedSnacks += suggestedSnack).asTry).map({
      case Success(r) => r
      case Failure(e) => this.update(suggestedSnack)
    })
  }

  /**
    * @param suggestedSnack id of the snack you want to update with different fields
    */
  def update(suggestedSnack: SuggestedSnack) = {
    val q = for { s <- suggestedSnacks if s.id === suggestedSnack.id } yield (s.dateSuggested, s.votes)
    val action = q.update((suggestedSnack.lastSuggested, suggestedSnack.votes))
    db.run(action)
  }

  /** increment number of votes for a suggested snack */
  def vote(id: Int) = {
    val a = sqlu"""UPDATE SUGGESTED_SNACKS SET VOTES = VOTES + 1 WHERE id=${id}"""
    db.run(a)
  }

  /**
    * takes snacks from the suggested snacks table and joins with the web service on id
    * essentially pulls more detailed information about the snacks from the web service
    * returns Success with the snacks or Failure
    */
  def joinServiceSnacks(): Future[Try[Seq[SnackDetailed]]] = {
     for {
      serviceSnacksTry <- serviceSnacksDao.all()
      _suggestedSnacks <- all()
    } yield {
       serviceSnacksTry match {
         case Success(serviceSnacks) => {
           // map of ids to Service Snacks
           val serviceSnacksMap = serviceSnacks.groupBy(s => s.id)

           Success(_suggestedSnacks.map(suggestedSnack => {
             // get service snack with same id as suggested snack
             val matchingServiceSnack: ServiceSnack = serviceSnacksMap(suggestedSnack.id).head

             // model that is the union of the SuggestedSnack and ServiceSnack fields
             SnackDetailed(suggestedSnack.id, matchingServiceSnack.optional, matchingServiceSnack.name,
               suggestedSnack.lastSuggested, matchingServiceSnack.lastPurchaseDate, suggestedSnack.votes,
               matchingServiceSnack.purchaseLocations)
           }))
         }
         case Failure(e) => Failure(e)
       }
    }
  }
}
