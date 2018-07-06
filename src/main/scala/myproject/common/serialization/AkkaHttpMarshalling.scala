package myproject.common.serialization

import java.util.UUID

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `text/html`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.mapResponseEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import myproject.common.serialization.JSONSerializer._

trait AkkaHttpMarshalling {

  protected def jsonUnmarshaller[A: Manifest] = Unmarshaller
    .stringUnmarshaller
    .forContentTypes(`application/json`)
    .map(_.fromJson[A])

  protected def jsonMarshaller[A: Manifest]: ToEntityMarshaller[A] =
    Marshaller.
      withFixedContentType(`application/json`) { v =>
        HttpEntity(ContentTypes.`application/json`, toJson(v))
      }

  protected def htmlMarshaller: ToEntityMarshaller[String] = Marshaller.stringMarshaller(`text/html`)

  protected def uuidFromStringUnmarshaller = Unmarshaller.strict[String, UUID] { s =>
    UUID.fromString(s)
  }

  private val jsonContentTypeWithUTF8: ContentType.NonBinary =
    ContentType.WithCharset(MediaType.applicationWithOpenCharset("json", "json"), HttpCharsets.`UTF-8`)

  val respondWithJsonContentType = mapResponseEntity(_.withContentType(jsonContentTypeWithUTF8))
}
