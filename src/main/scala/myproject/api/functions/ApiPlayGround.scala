package myproject.api.functions

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, Locale, UUID}

import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.{ApiFunction, ApiParameters}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future

class ApiPlayGround extends ApiFunction {

  import MyEnum.MyEnum

  override val name = "play"
  override val description = "test the API parameters handling"
  override val secured = false

  object MyEnum extends Enumeration {
    type MyEnum = Value
    val One = Value("one")
    val Two = Value("two")
    val Three = Value("three")
  }


  case class NestedMore(value: MyEnum)
  case class Nested(value: List[Int], nestedMore: NestedMore)
  case class Values(
      string: String,
      optString: String,
      nullableString: Option[String],
      nullableOptionalString: Option[String],
      int: Int,
      intList: List[Int],
      stringList: List[String],
      boolean: Boolean,
      long: Long,
      double: Double,
      bigDecimal: BigDecimal,
      nonEmptyString: String,
      currency: Currency,
      locale: Locale,
      email: EmailAddress,
      datetime: LocalDateTime,
      date: LocalDate,
      uuid: UUID,
      enumString: MyEnum,
      enumId: MyEnum,
      nested: Nested) {

    def serialize = Map(
      "string" -> string,
      "nullable_string" -> nullableString,
      "nullable_optional_string" -> nullableOptionalString,
      "int" -> int,
      "int_list" -> intList,
      "string_list" -> stringList,
      "boolean" -> boolean,
      "long" -> long,
      "double" -> double,
      "bigdecimal" -> bigDecimal,
      "non_empty_string" -> nonEmptyString,
      "currency" -> currency.getCurrencyCode,
      "locale" -> locale.toLanguageTag,
      "email" -> email.toString,
      "datetime" -> datetime,
      "date" -> date,
      "uuid" -> uuid,
      "enum_string" -> enumString.toString,
      "enum_id" -> enumId.id,
      "nested" -> Map(
        "value" -> nested.value,
        "nested_more" -> Map(
          "value" -> nested.nestedMore.value.toString)
      )
    )
  }

  val string = ApiParameter("a_string", ApiParameterType.String, "a string")
  val optString = ApiParameter("a_string_opt", ApiParameterType.String, "an optional string", optional = true)
  val nullableString = ApiParameter("a_nullable_string", ApiParameterType.String, "an optional string", nullable = true)
  val nullableOptionalString = ApiParameter("a_nullable_optional_string", ApiParameterType.String, "an optional nullable string", nullable = true, optional = true)
  val int = ApiParameter("an_int", ApiParameterType.Int, "an int")
  val intList = ApiParameter("an_int_list", ApiParameterType.IntList, "an int list")
  val boolean = ApiParameter("a_boolean", ApiParameterType.Boolean, "a bollean")
  val long = ApiParameter("a_long", ApiParameterType.Long, "a long ")
  val double = ApiParameter("a_double", ApiParameterType.Double, "a double")
  val bigDecimal = ApiParameter("a_big_decimal", ApiParameterType.BigDecimal, "a bigdecimal")
  val nonEmptyString = ApiParameter("a_non_empty_string", ApiParameterType.NonEmptyString, "a non empty string")
  val stringList = ApiParameter("a_string_list", ApiParameterType.StringList, "a string list")
  val stringListOpt = ApiParameter("a_string_opt_list", ApiParameterType.StringListOpt, "list of optional strings")
  val currency = ApiParameter("a_currency", ApiParameterType.Currency, "a currency")
  val locale = ApiParameter("a_locale", ApiParameterType.Locale, "a locale")
  val email = ApiParameter("an_email", ApiParameterType.Email, "an email")
  val dateTime = ApiParameter("a_datetime", ApiParameterType.DateTime, "a datetime")
  val date = ApiParameter("a_date", ApiParameterType.Date, "a date")
  val uuid = ApiParameter("an_uuid", ApiParameterType.UUID, "an uuid")
  val enumString = ApiParameter("an_enum_string", ApiParameterType.EnumString, "an enum string ", withEnum = Some(MyEnum))
  val enumId = ApiParameter("an_enum_id", ApiParameterType.EnumId, "an enum id", withEnum = Some(MyEnum))
  val nestedIntList = ApiParameter("an_int_list", ApiParameterType.IntList, "an int list", path = Some(List("nested")))
  val veryNestedEnumString = ApiParameter("an_enum_string", ApiParameterType.EnumString, "an enum string ", withEnum = Some(MyEnum), path = Some(List("nested", "very_nested")))

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, auditData: Audit.AuditData) = {
    Future(
      Values(
        string,
        (optString: Option[String]).getOrElse(""),
        nullableString,
        (nullableOptionalString: Option[Option[String]]).flatten,
        int,
        intList,
        stringList,
        boolean,
        long,
        double,
        bigDecimal,
        nonEmptyString,
        currency,
        locale,
        email,
        dateTime,
        date,
        uuid,
        ApiParameters.Enumerations.toEnum(enumString, MyEnum),
        ApiParameters.Enumerations.toEnum(enumId, MyEnum),
        Nested(nestedIntList, NestedMore(ApiParameters.Enumerations.toEnum(veryNestedEnumString, MyEnum)))).serialize
    )
  }
}
