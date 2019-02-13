package myproject.database

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
    with ChannelDAO {

  val dbType: DatabaseType
  val url: String
  val user: Option[String]
  val password: Option[String]
}

object ApplicationDatabase {
  val currentDatabaseImpl = new SlickApplicationDatabase
}
