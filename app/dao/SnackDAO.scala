package dao
import scala.concurrent.Future

import javax.inject.Inject
import models.Snack
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

class SnackDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val Snacks = TableQuery[SnacksTable]

  def all(): Future[Seq[Snack]] = db.run(Snacks.result)

  def insert(snack: Snack): Future[Unit] = db.run(Snacks += snack).map { _ => () }

  private class SnacksTable(tag: Tag) extends Table[Snack](tag, "snacks") {

    def name = column[String]("name")

    def * = (name) <> (Snack, Snack.unapply _)
  }
}
