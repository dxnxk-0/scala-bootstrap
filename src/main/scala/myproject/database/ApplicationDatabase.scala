package myproject.database

import myproject.Config
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Tokens.TokenDAO
import myproject.iam.Users.UserDAO
import myproject.iam.dao.{SlickChannelDAO, SlickGroupDAO, SlickTokenDAO, SlickUserDAO}
import slick.jdbc.JdbcProfile

trait ApplicationDatabase extends DatabaseInterface with JdbcProfile with SlickUserDAO with SlickTokenDAO with SlickGroupDAO with SlickChannelDAO

object ApplicationDatabase {
  val productionDatabaseImpl = Class.forName(Config.database.implClassName).newInstance.asInstanceOf[DatabaseInterface with ChannelDAO with GroupDAO with UserDAO with TokenDAO]
}
