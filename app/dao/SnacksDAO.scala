package dao
import scala.concurrent.Future

import javax.inject.Inject
import models.Snack
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

trait SnacksComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class Snacks(tag: Tag) extends Table[Snack](tag, "SUGGESTED_SNACKS") {
    def id = column[Int]("ID")
    def dateSuggested = column[String]("DATE_SUGGESTED")
    def votes = column[Int]("VOTES")

    def * = (id, dateSuggested, votes) <> (Snack.tupled, Snack.unapply _)
  }
}

class SnackDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends SnacksComponent
  with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val snacks = TableQuery[Snacks]

  def all(): Future[Seq[Snack]] = db.run(snacks.result)

  def insert(snack: Snack): Future[Unit] = db.run(snacks += snack).map { _ => () }

  def vote(id: Int) = {
    val a = sqlu"""UPDATE SUGGESTED_SNACKS SET VOTES = VOTES + 1 WHERE id=${id}"""
    db.run(a)
  }
}
