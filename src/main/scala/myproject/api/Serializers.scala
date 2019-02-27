package myproject.api

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, Locale, UUID}

import myproject.common.Done
import myproject.common.Geography.Country
import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.{User, UserLevel}
import uk.gov.hmrc.emailaddress.EmailAddress

object Serializers {

  trait Serializer[A] {
    def serialize(value: A): Any
  }

  object Serializer {
    def serialize[A](value: A)(implicit serializer: Serializer[A]) = serializer.serialize(value)
  }

  implicit class SerializerExtension[A](v: A) {
    def serialize(implicit serializer: Serializer[A]) = Serializer.serialize(v)
  }

  implicit def optionSerializer[A](implicit serializer: Serializer[A]) = new Serializer[Option[A]] {
    def serialize(value: Option[A]) = value match {
      case None => None
      case Some(v) => Some(serializer.serialize(v))
    }
  }

  implicit def seqSerializer[A](implicit serializer: Serializer[A]) = new Serializer[Seq[A]] {
    def serialize(value: Seq[A]) = value.map(_.serialize)
  }

  implicit def listSerializer[A](implicit serializer: Serializer[A]) = new Serializer[List[A]] {
    def serialize(value: List[A]) = value.map(_.serialize)
  }

  implicit val intSerializer = new Serializer[Int] {
    def serialize(value: Int) = value
  }

  implicit val booleanSerializer = new Serializer[Boolean] {
    def serialize(value: Boolean) = value
  }

  implicit val floatSerializer = new Serializer[Float] {
    def serialize(value: Float) = value
  }

  implicit val charSerializer = new Serializer[Char] {
    def serialize(value: Char) = value
  }

  implicit val shortSerializer = new Serializer[Short] {
    def serialize(value: Short) = value
  }

  implicit val doubleSerializer = new Serializer[Double] {
    def serialize(value: Double) = value
  }

  implicit val longSerializer = new Serializer[Long] {
    def serialize(value: Long) = value
  }

  implicit val uuidSerializer = new Serializer[UUID] {
    def serialize(uuid: UUID) = uuid.toString
  }

  implicit val emailSerializer = new Serializer[EmailAddress] {
    def serialize(email: EmailAddress) = email.toString
  }

  implicit val dateTimeSerializer = new Serializer[LocalDateTime] {
    def serialize(dateTime: LocalDateTime) = dateTime.toString + "Z"
  }

  implicit val dateSerializer = new Serializer[LocalDate] {
    def serialize(date: LocalDate) = date.toString
  }

  implicit val countrySerializer = new Serializer[Country] {
    def serialize(country: Country) = country.iso3
  }

  implicit val bigDecimalSerializer = new Serializer[BigDecimal] {
    def serialize(value: BigDecimal) = value
  }

  implicit val localeSerializer = new Serializer[Locale] {
    def serialize(locale: Locale) = locale.toLanguageTag
  }

  implicit val currencySerializer = new Serializer[Currency] {
    def serialize(currency: Currency) = currency.getCurrencyCode
  }

  implicit val stringSerializer = new Serializer[String] {
    def serialize(value: String) = value
  }

  implicit def enumSerializer[A <: Enumeration] = new Serializer[A#Value] {
    def serialize(value: A#Value) = value.toString
  }

  implicit val userSerializer = new Serializer[User] {
    def serialize(user: User) = {

      val common = Map(
        "user_id" -> user.id.serialize,
        "login" -> user.login.serialize,
        "first_name" -> user.firstName.serialize,
        "last_name" -> user.lastName.serialize,
        "level" -> user.level.serialize,
        "email" -> user.email.serialize,
        "status" -> user.status.serialize,
        "created" -> user.created.serialize,
        "last_update" -> user.lastUpdate.serialize)

      user.level match {
        case UserLevel.Platform | UserLevel.NoLevel => common
        case UserLevel.Channel =>
          common ++ Map("channel_id" -> user.channelId.serialize)
        case UserLevel.Group =>
          common ++ Map("group_id" -> user.groupId.serialize, "group_role" -> user.groupRole.serialize)
      }
    }
  }

  implicit val channelSerializer = new Serializer[Channel] {
    def serialize(channel: Channel) = Map(
      "channel_id" -> channel.id.serialize,
      "name" -> channel.name.serialize,
      "created" -> channel.created.serialize,
      "last_update" -> channel.lastUpdate.serialize)
  }

  implicit val groupSerializer = new Serializer[Group] {
    def serialize(group: Group) = Map(
      "group_id" -> group.id.serialize,
      "name" -> group.name.serialize,
      "status" -> group.status.serialize,
      "parent_id" -> group.parentId.serialize,
      "channel_id" -> group.channelId.serialize,
      "created" -> group.created.serialize,
      "last_update" -> group.lastUpdate.serialize)
  }

  implicit val doneTypeSerializer = new Serializer[Done.type] {
    def serialize(done: Done.type) = Unit
  }

  implicit val doneSerializer = new Serializer[Done] {
    def serialize(done: Done) = Unit
  }
}
