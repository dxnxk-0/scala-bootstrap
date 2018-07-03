package myproject.common.serialization

import java.util.UUID

import akka.http.scaladsl.model.{ContentType, HttpCharsets, MediaType}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.server.Directives.mapResponseEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import myproject.common.serialization.JSONSerializer._

trait AkkaHttpMarshalling {
  /**
    * Provides an Akka-Http Json unmarshaller
    * @tparam A the requested type
    * @return the unmarshaller
    */
  protected def getJsonUnmarshaller[A: Manifest] = Unmarshaller
    .stringUnmarshaller
    .forContentTypes(`application/json`)
    .map(_.fromJson[A])

  /**
    * Provides a UUID from String Akka-Http Unmarshaller
    * @return the unmarshaller
    */
  protected def getUuidFromStringUnmarshaller = Unmarshaller.strict[String, UUID] { s =>
    UUID.fromString(s)
  }

  private val jsonContentTypeWithUTF8: ContentType.NonBinary =
    ContentType.WithCharset(MediaType.applicationWithOpenCharset("json", "json"), HttpCharsets.`UTF-8`)

  val respondWithJsonContentType = mapResponseEntity(_.withContentType(jsonContentTypeWithUTF8))
}
