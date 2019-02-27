package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.FutureImplicits._
import myproject.common.serialization.OpaqueData
import myproject.common.{Done, ObjectNotFoundException}
import myproject.database.ApplicationDatabase
import myproject.iam.Notifier
import myproject.iam.Users.CRUD

import scala.language.postfixOps

class SendMagicLink(implicit db: ApplicationDatabase, notifier: Notifier) extends ApiFunction {
  override val name = "send_magic_link"
  override val doc = ApiSummaryDoc("send to an email address a magic link in order to login to the platform", "nothing is returned")
  override val secured: Boolean = false

  override def process(implicit p: OpaqueData.ReifiedDataWrapper) = {
    CRUD.sendMagicLink(p.email("email")) recover {
      case _: ObjectNotFoundException => Done
    } toDone
  }
}
