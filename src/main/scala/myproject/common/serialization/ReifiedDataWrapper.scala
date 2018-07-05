package myproject.common.serialization

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, YearMonth}
import java.util.{Currency, Locale, UUID}

import myproject.common.CustomException
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.util.Try

/**
  * Exception spawned when the value is set to null. Note that it can be a valid value in the context of an option.
  * Setting the option to None will be done by setting the value to null. The caller will wrap the extractor with
  * [[ReifiedDataWrapper.asOpt]] or [[ReifiedDataWrapper.missingKeyOrNullAsOption]]
  */
case class NullValueException(msg: String) extends CustomException(msg)

/**
  * Exception spawned when the requested [[ReifiedDataWrapper]] key does not exist. If the field is optional,
  * the call to the extractor should be wrapped by [[ReifiedDataWrapper.missingKeyAsOption]]
  */
case class MissingKeyException(msg: String) extends CustomException(msg)

/**
  * Exception spawned when the extractor in a [[ReifiedDataWrapper]] find a value with a type which does not
  * correspond to the expected one.
  */
case class InvalidTypeException(msg: String) extends CustomException(msg)

/**
  * This object allows a typed access to an opaque value, such as a json de-serialized object.
  * @param underlying The underlying object
  */
class ReifiedDataWrapper(private val underlying: Any) {

  private val underlyingMap = Try(underlying.asInstanceOf[Map[String, Any]])
  private val underlyingArray = Try(underlying.asInstanceOf[Seq[Any]])

  /**
    * Access an underlying data and throw a [[NullValueException]] in case of the value is null, or a [[MissingKeyException]]
    * if the key does not exist. The exceptions can be mapped to options using the asOpt method of [[ReifiedDataWrapper]] object.
    * @param key The requested key of the underlying dictionary. "." means to use the underlying
    */
  private def get(key: String): Any = {
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
      case Some(null) => throw NullValueException(s"null value for key `$key` is not allowed")
      case None => throw MissingKeyException(if (underlyingMap.isSuccess) s"key `$key` was not found" else s"parameter at position $key was not found")
      case Some(v) => v
    }
  }

  def int(key: String, value: Option[Any] = None): Int = value.getOrElse(get(key)) match {
    case e: Int => e
    case e: String => Try(e.toInt).getOrElse(throw InvalidTypeException(s"key `$key` is not of type int"))
    case _ => throw InvalidTypeException(s"key `$key` is not of type Int")
  }

  def intList(key: String): List[Int] = {
    Try(get(key).asInstanceOf[List[Any]])
      .getOrElse(throw InvalidTypeException(s"key $key is not a list of int values"))
      .zipWithIndex.map { case (v, i) => int(s"$key-$i", Some(v)) }
  }

  def boolean(key: String, value: Option[Any] = None): Boolean = value.getOrElse(get(key)) match {
    case b: Boolean => b
    case e: String => Try(e.toBoolean).getOrElse(throw InvalidTypeException(s"key `$key` is not of type Boolean"))
    case _ => throw InvalidTypeException(s"key `$key` is not of type Boolean")
  }

  def long(key: String, value: Option[Any] = None): Long = value.getOrElse(get(key)) match {
    case e: Int => e.toLong
    case e: Long => e.toLong
    case e: String => Try(e.toLong).getOrElse(throw InvalidTypeException(s"key `$key` is not of type Long"))
    case _ => throw InvalidTypeException(s"key `$key` is not of type Long")
  }

  def double(key: String, value: Option[Any] = None): Double = value.getOrElse(get(key)) match {
    case e: Int => e.toDouble
    case e: Long => e.toDouble
    case e: Double => e
    case e: Float => e.toDouble
    case e: String => Try(e.toDouble).getOrElse(throw InvalidTypeException(s"key `$key` is not of type Double"))
    case _ => throw InvalidTypeException(s"key `$key` is not of type Double")
  }

  def bigDecimal(key: String, value: Option[Any] = None): BigDecimal = value.getOrElse(get(key)) match {
    case e: Int => BigDecimal(e)
    case e: Long => BigDecimal(e)
    case e: Double => BigDecimal(e)
    case _ => throw InvalidTypeException(s"key `$key` is not of type Numeric")
  }

  def nonEmptyString(key: String, value: Option[Any] = None): String = string(key, value) match {
    case e: String if e.trim.isEmpty => throw InvalidTypeException(s"Empty strings are not allowed (key $key)")
    case e => e
  }

  def string(key: String, value: Option[Any] = None): String = value.getOrElse(get(key)) match {
    case e: String => e.trim
    case v => throw InvalidTypeException(s"key `$key` ($v) is not of type String")
  }

  def stringList(key: String): List[String] = {
    Try(get(key).asInstanceOf[List[Any]])
      .getOrElse(throw InvalidTypeException(s"key $key is not a list of string values"))
      .zipWithIndex.map { case (v, i) => nonEmptyString(s"$key-$i", Some(v)) }
  }

  def stringListOpt(key: String): List[Option[String]] = {
    Try(get(key).asInstanceOf[List[Any]])
      .getOrElse(throw InvalidTypeException(s"key $key is not a list of string values"))
      .zipWithIndex.map { case (v, i) => Option(v).map(str => nonEmptyString(s"$key-$i", Some(str))) }
  }

  def enumString(key: String, enum: Enumeration, value: Option[Any] = None): enum.Value = value.getOrElse(get(key)) match {
    case e => Try(enum.withName(e.toString))
      .getOrElse(throw InvalidTypeException(s"Value `$e` for key `$key` is invalid. Possible values: ${enum.values.mkString(",")}"))
  }

  def enumStringList(key: String, enum: Enumeration): List[enum.Value] = {
    Try(get(key).asInstanceOf[List[String]])
      .getOrElse(throw InvalidTypeException(s"Key $key is not a list of enum values"))
      .map(v => enumString(key, enum, Some(v)))
  }

  def enumId(key: String, enum: Enumeration): enum.Value = {

    def fail(e: Any) =
      throw InvalidTypeException(s"Value `$e` for key `$key` is invalid. Possible integer values: ${enum.values.map(_.id).mkString(",")}")

    val value = int(key)
    Try(enum(value)).getOrElse(fail(value))

  }

  def currency(key: String, value: Option[Any] = None): Currency = value.getOrElse(get(key)) match {
    case cur =>
      Try(Currency.getInstance(cur.toString))
        .getOrElse(throw InvalidTypeException(s"Currency `$cur` for key `$key` is invalid"))
  }

  def locale(key: String, value: Option[Any] = None): Locale = value.getOrElse(get(key)) match {
    case str: String =>
      val locale = Locale.forLanguageTag(str)
      if (locale != Locale.UK && locale != Locale.US && locale != Locale.FRANCE)
        throw InvalidTypeException(s"locale `$locale` is not supported. Valid locales are: ${Locale.UK.toLanguageTag}, ${Locale.US.toLanguageTag}, ${Locale.FRANCE.toLanguageTag}")
      else locale

    case _ => throw InvalidTypeException(s"key `$key` is not a valid locale")
  }

  def email(key: String, value: Option[String] = None): EmailAddress = {

    value.getOrElse(nonEmptyString(key)) match {
      case e if Try(EmailAddress(e)).isSuccess => EmailAddress(e)
      case _ =>
        throw InvalidTypeException(s"key `$key` is not a valid email")
    }
  }

  def dateTime(key: String, value: Option[Any] = None): LocalDateTime = value.getOrElse(get(key)) match {
    case date: String =>
      Try(LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME))
        .getOrElse(throw InvalidTypeException(s"LocalDateTime `$date` for key `$key` is invalid"))
  }

  def date(key: String, value: Option[Any] = None): LocalDate = value.getOrElse(get(key)) match {
    case date: String =>
      Try(LocalDate.parse(date))
        .getOrElse(Try(YearMonth.parse(date, DateTimeFormatter.ofPattern("yyyy-M")).atDay(1))
          .getOrElse(throw InvalidTypeException(s"Date (without time) `$date` for key `$key` is invalid")))
  }

  def uuid(key: String, value: Option[Any] = None): UUID = value.getOrElse(get(key)) match {
    case uuid: String => Try(UUID.fromString(uuid.trim)).getOrElse(throw InvalidTypeException(s"key $key is not a valid UUID"))
    case _ => throw InvalidTypeException(s"key $key is not a valid UUID")
  }

  def subList(key: String, value: Option[Any] = None): List[ReifiedDataWrapper] = value.getOrElse(get(key)) match {
    case list => Try(list.asInstanceOf[List[Any]])
      .getOrElse(throw InvalidTypeException(s"key `$key` is not a sequence"))
      .map(item => new ReifiedDataWrapper(item))
  }

  def subData(key: String, value: Option[Any] = None): ReifiedDataWrapper = {
    new ReifiedDataWrapper(value.getOrElse(get(key)))
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

/**
  * This object provide convenient methods to map [[ReifiedDataWrapper]] exceptions to options. Depending on the semantic
  * of the value (missing, null), the appropriate method will wrap a query to the underlying object. Examples:
  * {{{
  * val optionalScalarUpdate = missingKeyAsOption(wrapper.int("my_value"))
  * val optionalOptionUpdate = missingKeyAsOption(asOpt(wrapper.int("my_value")))
  * }}}
  */
object ReifiedDataWrapper {

  /**
    * Map a null or non existing value in a [[ReifiedDataWrapper]] to an option. That allows json objects to set an
    * option to None by setting a json key to "null".
    */
  def missingKeyOrNullAsOption[A](v: => A): Option[A] = try {
    Some(v)
  } catch {
    case MissingKeyException(_) => None
    case NullValueException(_) => None
  }

  /**
    * Map a non existing value in a [[ReifiedDataWrapper]] to an option.
    */
  def missingKeyAsOption[A](v: => A): Option[A] = try {
    Some(v)
  } catch {
    case MissingKeyException(_) => None
  }

  /**
    * Map an existing value in a [[ReifiedDataWrapper]] to an Option. That allows json objects to set an option to None by setting a json
    * key to "null".
    */
  def asOpt[A](v: => A): Option[A] = try {
    Some(v)
  } catch {
    case NullValueException(_) => None
    case e: MissingKeyException => throw e.copy(msg = e.msg + " (As an option, null means none)")
  }
}
