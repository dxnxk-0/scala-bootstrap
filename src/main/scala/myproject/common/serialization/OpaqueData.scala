package myproject.common.serialization

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, YearMonth}
import java.util.{Currency, Locale, UUID}

import myproject.common.Geography.Country
import myproject.common.{Geography, InvalidTypeException, MissingKeyException, NullValueException}
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.util.Try

object OpaqueData {

  object Type extends Enumeration {
    type Type = Value
    val Int, String, NonEmptyString, UUID, StringEnumeration, IntEnumeration, IntList, StringList, Boolean, Long,
        Double, Date, Datetime, BigDecimal, Email, List, Object, Locale, Currency, Country = Value
  }

  class ReifiedDataWrapper(private val underlying: Any) {

    import Type.Type

    private val underlyingMap = Try(underlying.asInstanceOf[Map[String, Any]])
    private val underlyingArray = Try(underlying.asInstanceOf[Seq[Any]])

    private def get(key: String, tpe: Type) = {
      val value = if (key == ".")
        Some(underlying)
      else if (underlyingMap.isSuccess) {
        underlyingMap.get.get(key)
      }
      else if (underlyingArray.isSuccess) {
        val pos = Try(key.toInt).getOrElse(throw new NoSuchFieldException(s"The underlying array is only accessible by position"))
        Try(underlyingArray.get(pos)).toOption
      }
      else throw new NoSuchFieldException(s"Don't know how to access the underlying data")

      value match {
        case Some(null) => throw NullValueException(s"null value for key `$key` of type [$tpe] is not allowed")
        case None => throw MissingKeyException(if (underlyingMap.isSuccess) s"key `$key` of type [$tpe] was not found" else s"parameter at position $key was not found")
        case Some(v) => v
      }
    }

    def int(key: String, value: Option[Any] = None, tpe: Type = Type.Int): Int = value.getOrElse(get(key, tpe)) match {
      case e: Int => e
      case e: String => Try(e.toInt).getOrElse(throw InvalidTypeException(s"key `$key` is not of type [$tpe]"))
      case _ => throw InvalidTypeException(s"key `$key` is not of type Int")
    }

    def intList(key: String, tpe: Type = Type.IntList): List[Int] = {
      Try(get(key, tpe).asInstanceOf[List[Any]])
        .getOrElse(throw InvalidTypeException(s"key $key is not of type [$tpe]"))
        .zipWithIndex.map { case (v, i) => int(s"$key-$i", Some(v)) }
    }

    def boolean(key: String, value: Option[Any] = None, tpe: Type = Type.Boolean): Boolean = value.getOrElse(get(key, tpe)) match {
      case b: Boolean => b
      case e: String => Try(e.toBoolean).getOrElse(throw InvalidTypeException(s"key `$key` is not of type [$tpe]"))
      case _ => throw InvalidTypeException(s"key `$key` is not of type [$tpe]")
    }

    def long(key: String, value: Option[Any] = None, tpe: Type = Type.Long): Long = value.getOrElse(get(key, tpe)) match {
      case e: Int => e.toLong
      case e: Long => e.toLong
      case e: String => Try(e.toLong).getOrElse(throw InvalidTypeException(s"key `$key` is not of type [$tpe]"))
      case _ => throw InvalidTypeException(s"key `$key` is not of type [$tpe]")
    }

    def double(key: String, value: Option[Any] = None, tpe: Type = Type.Double): Double = value.getOrElse(get(key, tpe)) match {
      case e: Int => e.toDouble
      case e: Long => e.toDouble
      case e: Double => e
      case e: Float => e.toDouble
      case e: String => Try(e.toDouble).getOrElse(throw InvalidTypeException(s"key `$key` is not of type [$tpe]"))
      case _ => throw InvalidTypeException(s"key `$key` is not of type [$tpe]")
    }

    def bigDecimal(key: String, value: Option[Any] = None, tpe: Type = Type.BigDecimal): BigDecimal = value.getOrElse(get(key, tpe)) match {
      case e: Int => BigDecimal(e)
      case e: Long => BigDecimal(e)
      case e: Double => BigDecimal(e)
      case _ => throw InvalidTypeException(s"key `$key` is not of type [$tpe]")
    }

    def nonEmptyString(key: String, value: Option[Any] = None, tpe: Type = Type.NonEmptyString): String = string(key, value, tpe) match {
      case e: String if e.trim.isEmpty => throw InvalidTypeException(s"key `$key` cannot be an empty string")
      case e => e
    }

    def string(key: String, value: Option[Any] = None, tpe: Type = Type.String): String = value.getOrElse(get(key, tpe)) match {
      case e: String => e.trim
      case v => throw InvalidTypeException(s"key `$key` (`$v`) is not of type [$tpe]")
    }

    def stringList(key: String, tpe: Type = Type.StringList): List[String] = {
      Try(get(key, tpe).asInstanceOf[List[Any]])
        .getOrElse(throw InvalidTypeException(s"key $key is not a list of type [$tpe]"))
        .zipWithIndex.map { case (v, i) => nonEmptyString(s"$key-$i", Some(v)) }
    }

    def enumString(key: String, enum: Enumeration, value: Option[Any] = None, tpe: Type = Type.StringEnumeration): enum.Value = value.getOrElse(get(key, tpe)) match {
      case e => Try(enum.withName(e.toString))
        .getOrElse(throw InvalidTypeException(s"value `$e` for key `$key` is invalid. Possible values: ${enum.values.mkString(",")}"))
    }

    def enumStringList(key: String, enum: Enumeration, tpe: Type = Type.StringList): List[enum.Value] = {
      Try(get(key, tpe).asInstanceOf[List[String]])
        .getOrElse(throw InvalidTypeException(s"key $key is not a list of [$tpe]"))
        .map(v => enumString(key, enum, Some(v)))
    }

    def enumId(key: String, enum: Enumeration, tpe: Type = Type.IntEnumeration): enum.Value = {

      def fail(e: Any) =
        throw InvalidTypeException(s"value `$e` for key `$key` is invalid. Possible integer values: ${enum.values.map(_.id).mkString(",")}")

      val value = int(key, tpe = tpe)
      Try(enum(value)).getOrElse(fail(value))

    }

    def currency(key: String, value: Option[Any] = None, tpe: Type = Type.Currency): Currency = value.getOrElse(get(key, tpe)) match {
      case cur =>
        Try(Currency.getInstance(cur.toString))
          .getOrElse(throw InvalidTypeException(s"Currency `$cur` for key `$key` is invalid"))
    }

    def locale(key: String, value: Option[Any] = None, tpe: Type = Type.Locale): Locale = value.getOrElse(get(key, tpe)) match {
      case str: String =>
        val locale = Locale.forLanguageTag(str)
        if (locale != Locale.UK && locale != Locale.US && locale != Locale.FRANCE)
          throw InvalidTypeException(s"locale `$locale` is not supported. Valid locales are: ${Locale.UK.toLanguageTag}, ${Locale.US.toLanguageTag}, ${Locale.FRANCE.toLanguageTag}")
        else locale

      case _ => throw InvalidTypeException(s"key `$key` is not of type [$tpe]")
    }

    def email(key: String, value: Option[String] = None, tpe: Type = Type.Email): EmailAddress = {

      value.getOrElse(nonEmptyString(key)) match {
        case e if Try(EmailAddress(e)).isSuccess => EmailAddress(e.toLowerCase)
        case _ =>
          throw InvalidTypeException(s"key `$key` is not of type [$tpe]")
      }
    }

    def dateTime(key: String, value: Option[Any] = None, tpe: Type = Type.Datetime): LocalDateTime = value.getOrElse(get(key, tpe)) match {
      case date: String =>
        Try(LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME))
          .getOrElse(throw InvalidTypeException(s"LocalDateTime `$date` for key `$key` is invalid"))
    }

    def date(key: String, value: Option[Any] = None, tpe: Type = Type.Date): LocalDate = value.getOrElse(get(key, tpe)) match {
      case date: String =>
        Try(LocalDate.parse(date))
          .getOrElse(Try(YearMonth.parse(date, DateTimeFormatter.ofPattern("yyyy-M")).atDay(1))
            .getOrElse(throw InvalidTypeException(s"Date (without time) `$date` for key `$key` is invalid")))
    }

    def uuid(key: String, value: Option[Any] = None, tpe: Type = Type.UUID): UUID = value.getOrElse(get(key, tpe)) match {
      case uuid: String => Try(UUID.fromString(uuid.trim)).getOrElse(throw InvalidTypeException(s"key $key is not a valid UUID"))
      case uuid: UUID => uuid
      case _ => throw InvalidTypeException(s"key $key is not of type [$tpe]")
    }

    def subList(key: String, value: Option[Any] = None, tpe: Type = Type.List): List[ReifiedDataWrapper] = value.getOrElse(get(key, tpe)) match {
      case list => Try(list.asInstanceOf[List[Any]])
        .getOrElse(throw InvalidTypeException(s"key `$key` is not a sequence"))
        .map(item => new ReifiedDataWrapper(item))
    }

    def subData(key: String, value: Option[Any] = None, tpe: Type = Type.Object): ReifiedDataWrapper = {
      new ReifiedDataWrapper(value.getOrElse(get(key, tpe)))
    }

    def country(key: String, value: Option[Any] = None, tpe: Type = Type.Country): Country = value.getOrElse(get(key, tpe)) match {
      case code: String => Try(Geography.getCountry(code)).getOrElse(throw InvalidTypeException(s"value `$code` of key `$key` is not a valid country code"))
      case _ => throw InvalidTypeException(s"value of key `$key` is not a valid country code")
    }

    override def toString: String = {
      if (underlyingArray.isSuccess) {
        s"ReifiedDataWrapper[ ${underlyingArray.get} ]"
      } else if (underlyingMap.isSuccess) {
        s"ReifiedDataWrapper{ ${underlyingMap.get} }"
      } else {
        s"ReifiedDataWrapper($underlying)"
      }
    }
  }

  object ReifiedDataWrapper {

    def optional[A](v: => A, help: String = ""): Try[Option[A]] = Try {
      val msg = "key is optional, cannot be null"

      try {
        Some(v)
      } catch {
        case _: MissingKeyException => None
        case e: NullValueException => throw e.copy(msg = e.msg + s" ($msg)")
        case e: InvalidTypeException => throw e.copy(msg = e.msg + s" ($msg)")
      }
    }

    def optionalAndNullable[A](v: => A, help: String = ""): Try[Option[Option[A]]] = Try {
      val msg = "key is optional, can be null"

      try {
        Some(Some(v))
      } catch {
        case e: MissingKeyException => None
        case e: NullValueException => None
        case e: InvalidTypeException => throw e.copy(msg = e.msg + s" ($msg)")
      }
    }

    def nullable[A](v: => A, help: String = ""): Try[Option[A]] = Try {
      val msg = "key is required, can be null"

      try {
        Some(v)
      } catch {
        case _: NullValueException => None
        case e: MissingKeyException => throw e.copy(msg = e.msg + s" ($msg)")
        case e: InvalidTypeException => throw e.copy(msg = e.msg + s" ($msg)")
      }
    }

    def required[A](v: => A, help: String = ""): Try[A] = Try {
      val msg = "key is required, cannot be null"

      try {
        v
      } catch {
        case e: MissingKeyException => throw e.copy(msg = e.msg + s" ($msg)")
        case e: NullValueException => throw e.copy(msg = e.msg + s" ($msg)")
        case e: InvalidTypeException => throw e.copy(msg = e.msg + s" ($msg)")
      }
    }
  }
}
