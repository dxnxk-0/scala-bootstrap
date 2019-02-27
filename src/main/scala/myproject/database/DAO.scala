package myproject.database

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime}

import myproject.common.Geography.Country
import myproject.common.Runtime.ec
import myproject.common.{DatabaseErrorException, Done, Geography}
import slick.dbio.DBIO
import uk.gov.hmrc.emailaddress.EmailAddress

object DAO {
  object DBIOImplicits {
    implicit class DBIOUpdateExtensions(dbio: DBIO[Int]) {
      def doneUpdated(expected: Int) = dbio map {
        case 1 => Done
        case n => throw DatabaseErrorException(s"an error occured: 1 deletion expected got $n")
      }

      def doneSingleUpdate = doneUpdated(1)
    }

    implicit class DBIOBatchUpdateExtensions(dbio: DBIO[Option[Int]]) {
      def doneUpdated(expected: Int) = dbio.map {
        case Some(e) if e == expected => Done
        case n => throw DatabaseErrorException(s"an error occured: expected Some($expected) got $n")
      }
    }
  }
}

trait DAO { self: SlickProfile =>

  import slickProfile.api._

  implicit val localDateMapper = MappedColumnType.base[LocalDate, Date](
    l => Date.valueOf(l),
    d => d.toLocalDate)

  implicit val localDateTimeMapper = MappedColumnType.base[LocalDateTime, Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime)

  implicit val countryMapper = MappedColumnType.base[Country, String](
    country => country.iso3,
    code => Geography.getCountryF(code))

  implicit val emailAddressMapper = MappedColumnType.base[EmailAddress, String](
    email => email.toString,
    str => EmailAddress(str))
}
