package myproject.database

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}

import myproject.common.Geography
import myproject.common.Geography.Country
import slick.jdbc.JdbcProfile
import uk.gov.hmrc.emailaddress.EmailAddress

trait SlickDAO extends JdbcProfile {

  import api._

  val db: Database

  implicit val localDateMapper = MappedColumnType.base[LocalDate, Date](
    l => Date.valueOf(l),
    d => d.toLocalDate)

  implicit val localDateTimeMapper = MappedColumnType.base[LocalDateTime, Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime)

  implicit val countryMapper = MappedColumnType.base[Country, String](
    country => country.iso3,
    code => Geography.getCountry(code))

  implicit val emailAddressMapper = MappedColumnType.base[EmailAddress, String](
    email => email.toString,
    str => EmailAddress(str))
}
