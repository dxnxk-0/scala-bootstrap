package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.{Done, ObjectNotFoundException}
import myproject.iam.Notifier
import myproject.iam.Tokens.TokenDAO
import myproject.iam.Users.{CRUD, UserDAO}

class SendMagicLink(implicit db: UserDAO with TokenDAO, notifier: Notifier) extends ApiFunction {
  override val name = "send_magic_link"
  override val doc = ApiSummaryDoc("send to an email address a magic link in order to login to the platform", "nothing is returned")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper) = {
    CRUD.sendMagicLink(p.email("email")) recover {
      case _: ObjectNotFoundException => Done
    } map (_ => Done)
  }
}
