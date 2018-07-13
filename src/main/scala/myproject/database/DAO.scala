package myproject.database

import java.sql.Date
import java.time.LocalDate

import slick.jdbc.JdbcProfile

trait DAO extends JdbcProfile {
  import api._

  val db: Database

  implicit val localDateToDate = MappedColumnType.base[LocalDate, Date](
    l => Date.valueOf(l),
    d => d.toLocalDate)
}
