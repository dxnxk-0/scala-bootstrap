package myproject.web.views

import scalatags.Text.all._

trait HelloView {

  val view = html {
    body {
      h1(id:="hello", "Hello, World!")
    }
  }
}
