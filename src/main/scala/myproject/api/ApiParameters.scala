package myproject.api

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, Locale, UUID}

import myproject.common._
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ApiParameters {

  import ApiParameterType.ApiParameterType

  case class ApiParameter(
      name: String,
      `type`: ApiParameterType,
      description: String,
      nullable: Boolean = false,
      optional: Boolean = false,
      path: Option[List[String]] = None,
      withEnum: Option[Enumeration] = None) {

    private def dataForPath(implicit p: ReifiedDataWrapper): ReifiedDataWrapper = path map {
      pathElems => pathElems.foldLeft(p){ case (acc, elem) =>
        acc.subData(elem)
      }
    } getOrElse p

    private def pathToString = path map ("/" + _.mkString("/")) getOrElse ""

    private def wrapper(op: String => Any) = Try { (optional, nullable) match {
        case (false, false) => op(name)
        case (true, false) => missingKeyAsOption(op(name))
        case (false, true) => asOpt(op(name))
        case (true, true) => missingKeyAsOption(asOpt(op(name)))
      }
    } match {
      case Success(v) => v
      case Failure(MissingKeyException(msg)) => throw MissingKeyException(msg + s" key path: $pathToString")
      case Failure(InvalidTypeException(msg)) => throw InvalidTypeException(msg + s". key path: $pathToString")
      case Failure(NullValueException(msg)) => throw NullValueException(msg + s". key path: $pathToString")
      case f @ Failure(e) => throw e
    }

    def get(implicit p: ReifiedDataWrapper) = `type` match {
      case ApiParameterType.UUID => wrapper(dataForPath.uuid(_))
      case ApiParameterType.String => wrapper(dataForPath.string(_))
      case ApiParameterType.Int => wrapper(dataForPath.int(_))
      case ApiParameterType.IntList => wrapper(dataForPath.intList(_))
      case ApiParameterType.Boolean => wrapper(dataForPath.boolean(_))
      case ApiParameterType.Long => wrapper(dataForPath.long(_))
      case ApiParameterType.Double => wrapper(dataForPath.double(_))
      case ApiParameterType.BigDecimal => wrapper(dataForPath.bigDecimal(_))
      case ApiParameterType.NonEmptyString => wrapper(dataForPath.nonEmptyString(_))
      case ApiParameterType.StringList => wrapper(dataForPath.stringList(_))
      case ApiParameterType.StringListOpt => wrapper(dataForPath.stringListOpt(_))
      case ApiParameterType.Currency => wrapper(dataForPath.currency(_))
      case ApiParameterType.Locale => wrapper(dataForPath.locale(_))
      case ApiParameterType.Email => wrapper(dataForPath.email(_))
      case ApiParameterType.DateTime => wrapper(dataForPath.dateTime(_))
      case ApiParameterType.Date => wrapper(dataForPath.date(_))
      case ApiParameterType.EnumString =>
        withEnum.map(enum => wrapper(dataForPath.enumString(_, enum))).getOrElse(throw InvalidContextException(s"no enum provided for a string enum parameter"))
      case ApiParameterType.EnumId =>
        withEnum.map(enum => wrapper(dataForPath.enumId(_, enum))).getOrElse(throw InvalidContextException(s"no enum provided for a id enum parameter"))
      case t => throw InvalidTypeException(s"type $t is not supported")
    }
  }

  object Enumerations {
    def toEnum(param: ApiParameter, enum: Enumeration)(implicit p: ReifiedDataWrapper): enum.Value = {
      if(param.`type`==ApiParameterType.EnumString && !param.nullable || !param.optional) param.get.asInstanceOf[enum.Value] else throwInvalidType(param.name, "enum string option")
    }

    def toEnumOpt(param: ApiParameter, enum: Enumeration)(implicit p: ReifiedDataWrapper): Option[enum.Value] = {
      if(param.`type`==ApiParameterType.EnumString && (param.nullable || param.optional)) param.get.asInstanceOf[Option[enum.Value]] else throwInvalidType(param.name, "enum string option")
    }

    def toEnumOptOpt(param: ApiParameter, enum: Enumeration)(implicit p: ReifiedDataWrapper): Option[Option[enum.Value]] = {
      if(param.`type`==ApiParameterType.EnumString && (param.nullable && param.optional)) param.get.asInstanceOf[Option[Option[enum.Value]]] else throwInvalidType(param.name, "enum string option")
    }
  }

  private def throwInvalidType(name: String, `type`: String, msg: Option[String] = None) = throw InvalidTypeException(s"cannot extract parameter `$name` as a ${`type`}" + msg.map(m => ": $m").getOrElse(""))

  object ApiParameterType extends Enumeration {
    type ApiParameterType = Value
    val String = Value("string")
    val NonEmptyString = Value("non_empty_string")
    val Int = Value("int")
    val IntList = Value("intList")
    val Boolean = Value("boolean")
    val Long = Value("long")
    val Double = Value("double")
    val BigDecimal = Value("big_decimal")
    val StringList = Value("string_list")
    val StringListOpt = Value("string_list_opt")
    val Currency = Value("currency")
    val Locale = Value("locale")
    val Email = Value("email")
    val DateTime = Value("dateTime")
    val Date = Value("date")
    val UUID = Value("uuid")
    val EnumString = Value("enum_string")
    val EnumId = Value("enum_id")
  }

  implicit def toString(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if((param.`type`==ApiParameterType.String || param.`type`==ApiParameterType.NonEmptyString) && !param.nullable && !param.optional) param.get.asInstanceOf[String] else throwInvalidType(param.name, "string")
  implicit def toStringOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if((param.`type`==ApiParameterType.String || param.`type`==ApiParameterType.NonEmptyString) && (param.nullable || param.optional)) param.get.asInstanceOf[Option[String]] else throwInvalidType(param.name, "string option")
  implicit def toStringOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if((param.`type`==ApiParameterType.String || param.`type`==ApiParameterType.NonEmptyString) && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[String]]] else throwInvalidType(param.name, "string nested option")

  implicit def toStringList(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.StringList && !param.nullable && !param.optional) param.get.asInstanceOf[List[String]] else throwInvalidType(param.name, "string list")
  implicit def toStringListOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.StringList && (param.nullable || param.optional)) param.get.asInstanceOf[Option[List[String]]] else throwInvalidType(param.name, "string list option")
  implicit def toStringListOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.StringList && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[List[String]]]] else throwInvalidType(param.name, "string list nested option")

  implicit def toInt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Int && !param.nullable && !param.optional) param.get.asInstanceOf[Int] else throwInvalidType(param.name, "int")
  implicit def toIntOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Int && (param.nullable || param.optional)) param.get.asInstanceOf[Option[Int]] else throwInvalidType(param.name, "int option")
  implicit def toIntOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Int && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[Int]]] else throwInvalidType(param.name, "int nested option")

  implicit def toIntList(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.IntList && !param.nullable && !param.optional) param.get.asInstanceOf[List[Int]] else throwInvalidType(param.name, "int list")
  implicit def toIntListOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.IntList && (param.nullable || param.optional)) param.get.asInstanceOf[Option[List[Int]]] else throwInvalidType(param.name, "int list option")
  implicit def toIntListOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.IntList && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[List[Int]]]] else throwInvalidType(param.name, "int list nested option")

  implicit def toBoolean(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Boolean && !param.nullable && !param.optional) param.get.asInstanceOf[Boolean] else throwInvalidType(param.name, "boolean")
  implicit def toBooleanOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Boolean && (param.nullable || param.optional)) param.get.asInstanceOf[Option[Boolean]] else throwInvalidType(param.name, "boolean option")
  implicit def toBooleanOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Boolean && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[Boolean]]] else throwInvalidType(param.name, "boolean nested option")

  implicit def toLong(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Long && !param.nullable && !param.optional) param.get.asInstanceOf[Long] else throwInvalidType(param.name, "long")
  implicit def toLongOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Long && (param.nullable || param.optional)) param.get.asInstanceOf[Option[Long]] else throwInvalidType(param.name, "long option")
  implicit def toLongOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Long && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[Long]]] else throwInvalidType(param.name, "long nested option")

  implicit def toDouble(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Double && !param.nullable && !param.optional) param.get.asInstanceOf[Double] else throwInvalidType(param.name, "double")
  implicit def toDoubleOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Double && (param.nullable || param.optional)) param.get.asInstanceOf[Option[Double]] else throwInvalidType(param.name, "double option")
  implicit def toDoubleOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Double && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[Double]]] else throwInvalidType(param.name, "double nested option")

  implicit def toBigDecimal(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.BigDecimal && !param.nullable && !param.optional) param.get.asInstanceOf[BigDecimal] else throwInvalidType(param.name, "big decimal")
  implicit def toBigDecimalOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.BigDecimal && (param.nullable || param.optional)) param.get.asInstanceOf[Option[BigDecimal]] else throwInvalidType(param.name, "big decimal option")
  implicit def toBigDecimalOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.BigDecimal && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[BigDecimal]]] else throwInvalidType(param.name, "big decimal nested option")

  implicit def toCurrency(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Currency && !param.nullable && !param.optional) param.get.asInstanceOf[Currency] else throwInvalidType(param.name, "currency")
  implicit def toCurrencyOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Currency && (param.nullable || param.optional)) param.get.asInstanceOf[Option[Currency]] else throwInvalidType(param.name, "currency option")
  implicit def toCurrencyOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Currency && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[Currency]]] else throwInvalidType(param.name, "currency nested option")

  implicit def toLocale(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Locale && !param.nullable && !param.optional) param.get.asInstanceOf[Locale] else throwInvalidType(param.name, "locale")
  implicit def toLocaleOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Locale && (param.nullable || param.optional)) param.get.asInstanceOf[Option[Locale]] else throwInvalidType(param.name, "locale option")
  implicit def toLocaleOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Locale && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[Locale]]] else throwInvalidType(param.name, "locale nested option")

  implicit def toEmail(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Email && !param.nullable && !param.optional) param.get.asInstanceOf[EmailAddress] else throwInvalidType(param.name, "email")
  implicit def toEmailOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Email && (param.nullable || param.optional)) param.get.asInstanceOf[Option[EmailAddress]] else throwInvalidType(param.name, "email option")
  implicit def toEmailOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Email && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[EmailAddress]]] else throwInvalidType(param.name, "email nested option")

  implicit def toDateTime(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.DateTime && !param.nullable && !param.optional) param.get.asInstanceOf[LocalDateTime] else throwInvalidType(param.name, "date time")
  implicit def toDateTimeOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.DateTime && (param.nullable || param.optional)) param.get.asInstanceOf[Option[LocalDateTime]] else throwInvalidType(param.name, "date time option")
  implicit def toDateTimeOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.DateTime && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[LocalDateTime]]] else throwInvalidType(param.name, "date time nested option")

  implicit def toDate(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Date && !param.nullable && !param.optional) param.get.asInstanceOf[LocalDate] else throwInvalidType(param.name, "date")
  implicit def toDateOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Date && (param.nullable || param.optional)) param.get.asInstanceOf[Option[LocalDate]] else throwInvalidType(param.name, "date option")
  implicit def toDateOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.Date && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[LocalDate]]] else throwInvalidType(param.name, "date nested option")

  implicit def toUUID(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.UUID && !param.nullable && !param.optional) param.get.asInstanceOf[UUID] else throwInvalidType(param.name, "uuid")
  implicit def toUUIDOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.UUID && (param.nullable || param.optional)) param.get.asInstanceOf[Option[UUID]] else throwInvalidType(param.name, "uuid option")
  implicit def toUUIDOptOpt(param: ApiParameter)(implicit p: ReifiedDataWrapper) =
    if(param.`type`==ApiParameterType.UUID && param.nullable && param.optional) param.get.asInstanceOf[Option[Option[UUID]]] else throwInvalidType(param.name, "uuid nested option")
}