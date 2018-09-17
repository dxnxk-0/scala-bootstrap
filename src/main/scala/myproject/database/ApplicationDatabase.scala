package myproject.database

import myproject.Config
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Tokens.TokenDAO
import myproject.iam.Users.UserDAO

trait ApplicationDatabase
  extends DatabaseInterface
    with UserDAO
    with TokenDAO
    with GroupDAO
    with ChannelDAO

object ApplicationDatabase {
  val productionDatabaseImpl = Class.forName(Config.database.implClassName).newInstance.asInstanceOf[ApplicationDatabase]
}
