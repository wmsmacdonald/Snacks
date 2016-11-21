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

class SuggestedSnacksDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, snacksDao: ServiceSnacksDAO)
  extends SuggestedSnacksComponent
  with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val suggestedSnacks = TableQuery[SuggestedSnacks]

  def all(): Future[Seq[SuggestedSnack]] = db.run(suggestedSnacks.result)

  def suggest(id: Int): Future[Unit] = {
    val format = DateTimeFormat.forPattern("MM-dd-yyyy")
    val today = format.print(new LocalDate())
    val suggestedSnack = SuggestedSnack(id, today, 0)
    db.run((suggestedSnacks += suggestedSnack).asTry).map({
      case Success(_) => ()
      case Failure(e) => this.update(suggestedSnack)
    })
  }

  def update(suggestedSnack: SuggestedSnack) = {
    val q = for { s <- suggestedSnacks if s.id === suggestedSnack.id } yield (s.dateSuggested, s.votes)
    val action = q.update((suggestedSnack.lastSuggested, suggestedSnack.votes))
    db.run(action)
  }

  def vote(id: Int) = {
    val a = sqlu"""UPDATE SUGGESTED_SNACKS SET VOTES = VOTES + 1 WHERE id=${id}"""
    db.run(a)
  }

  def joinSnacks(): Future[Try[Seq[SnackDetailed]]] = {
     for {
      snacksTry <- snacksDao.all()
      _suggestedSnacks <- all()
    } yield {
       snacksTry match {
         case Success(snacks) => {
           val optionalSnacksMap = snacks.groupBy(s => s.id)
           Success(_suggestedSnacks.map(suggestedSnack => {
             val matchingSnack: ServiceSnack = optionalSnacksMap(suggestedSnack.id).head
             SnackDetailed(suggestedSnack.id, matchingSnack.optional, matchingSnack.name,
               suggestedSnack.lastSuggested, matchingSnack.lastPurchaseDate, suggestedSnack.votes,
               matchingSnack.purchaseLocations)
           }))
         }
         case Failure(e) => Failure(e)
       }
    }
  }
}
