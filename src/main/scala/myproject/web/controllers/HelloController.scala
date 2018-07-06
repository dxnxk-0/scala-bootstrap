package myproject.web.controllers

import akka.http.scaladsl.server.Directives._
import myproject.common.serialization.AkkaHttpMarshalling
import myproject.web.views.HelloView

trait HelloController extends HtmlController with HelloView with AkkaHttpMarshalling {

  val HelloRoute = path("hello") {
    complete(view.render)
  }
}
