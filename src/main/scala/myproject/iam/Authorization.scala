package myproject.iam

import myproject.common.Authorization.AccessChecker
import myproject.iam.Channels.{DefaultChannelAccessChecker, VoidChannelAccessChecker}
import myproject.iam.Groups.{DefaultGroupAccessChecker, VoidGroupAccessChecker}
import myproject.iam.Users.{DefaultUserAccessChecker, User, VoidUserAccessChecker}

object Authorization {

  class DefaultIAMAccessChecker(requestor: Option[User])
    extends AccessChecker
      with DefaultChannelAccessChecker
      with DefaultGroupAccessChecker
      with DefaultUserAccessChecker { implicit val requester = requestor }

  object VoidIAMAccessChecker
    extends AccessChecker
      with VoidChannelAccessChecker
      with VoidGroupAccessChecker
      with VoidUserAccessChecker { override val requester = None }
}
