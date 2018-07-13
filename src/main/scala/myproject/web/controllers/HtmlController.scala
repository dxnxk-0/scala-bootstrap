package myproject.web.controllers

import myproject.common.serialization.AkkaHttpMarshalling

trait HtmlController extends Controller {

  protected implicit val respondHtml = AkkaHttpMarshalling.getHtmlMarshaller
}
