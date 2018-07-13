package myproject.common.serialization

import java.util.UUID

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `text/html`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import myproject.common.serialization.JSONSerializer._

object AkkaHttpMarshalling {

  def getJsonUnmarshaller[A: Manifest] = Unmarshaller
    .stringUnmarshaller
    .forContentTypes(`application/json`)
    .map(_.fromJson[A])

  def getJsonMarshaller[A: Manifest]: ToEntityMarshaller[A] =
    Marshaller.
      withFixedContentType(`application/json`) { v =>
        HttpEntity(ContentTypes.`application/json`, toJson(v))
      }

  def getHtmlMarshaller: ToEntityMarshaller[String] = Marshaller.stringMarshaller(`text/html`)

  def getUUIDFromStringUnmarshaller = Unmarshaller.strict[String, UUID] { s =>
    UUID.fromString(s)
  }
}
