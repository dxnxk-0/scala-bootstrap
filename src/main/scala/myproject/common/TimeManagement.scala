package myproject.common

import java.time.{LocalDate, LocalDateTime, ZoneId}

object TimeManagement {

  def getCurrentDateTime: LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))

  def getCurrentLocalDate: LocalDate = LocalDate.now(ZoneId.of("UTC"))
}
