package myproject.web.controllers

import akka.http.scaladsl.server.Directives._
import myproject.web.views.HelloView

object HelloController extends HtmlController {

  val HelloRoute = path("hello") {
    complete(HelloView.view.render)
  }
}
