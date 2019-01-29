package myproject.database

import myproject.Config
import myproject.database.DatabaseType.DatabaseType
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Tokens.TokenDAO
import myproject.iam.Users.UserDAO

trait ApplicationDatabase
  extends DatabaseInterface
    with UserDAO
    with TokenDAO
    with GroupDAO
    with ChannelDAO { val dbType: DatabaseType }

object ApplicationDatabase {
  val currentDatabaseImpl = Class.forName(Config.Database.implClassName).newInstance.asInstanceOf[ApplicationDatabase]
}
