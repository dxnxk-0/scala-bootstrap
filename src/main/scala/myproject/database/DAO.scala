package myproject.database

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}

import slick.jdbc.JdbcProfile

trait DAO extends JdbcProfile {

  import api._

  val db: Database

  implicit val localDateMapper = MappedColumnType.base[LocalDate, Date](
    l => Date.valueOf(l),
    d => d.toLocalDate)

  implicit val localDateTimeMapper = MappedColumnType.base[LocalDateTime, Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime)
}
