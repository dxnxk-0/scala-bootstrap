package myproject.web.controllers

import myproject.common.serialization.AkkaHttpMarshalling

trait HtmlController extends AkkaHttpMarshalling {

  implicit val respondHtml = htmlMarshaller
}
