package myproject.web.views

import scalatags.Text.all._

trait AppView {

  def loginView(error: Option[String], loginUrl: String) =
    html(
      body(
        div(
          h2(color:="red", error),
          form(
            action:=loginUrl,
            method:="post",
            input(`type`:="text", name:="login"),
            input(`type`:="text", name:= "password"),
            input(`type`:="submit", value:="Submit")
          )
        )
      )
    )

  def homeView(username: String, cssUrl: String) =
    html(
      head(
        link(href:=cssUrl, rel:="stylesheet")
      ),
      body(
        div(
          h2(s"Welcome $username! This is the home page.")
        )
      )
    )
}
