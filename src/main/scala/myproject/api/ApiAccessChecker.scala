package myproject.api

import myproject.common.Authorization.AccessChecker
import myproject.iam.Channels.DefaultChannelAccessChecker
import myproject.iam.Groups.DefaultGroupAccessChecker
import myproject.iam.Users.{DefaultUserAccessChecker, User}

class ApiAccessChecker(requestor: User)
  extends AccessChecker
    with DefaultChannelAccessChecker
    with DefaultGroupAccessChecker
    with DefaultUserAccessChecker { implicit val requester = Some(requestor) }
