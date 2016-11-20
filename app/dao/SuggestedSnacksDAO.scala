package dao
import scala.concurrent.Future
import javax.inject.Inject

import models.{ServiceSnack, SnackDetailed, SuggestedSnack}
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

class SuggestedSnacksDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, snacksDao: SnacksDAO)
  extends SuggestedSnacksComponent
  with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val suggestedSnacks = TableQuery[SuggestedSnacks]

  def all(): Future[Seq[SuggestedSnack]] = db.run(suggestedSnacks.result)

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
