package myproject.api

import myproject.audit.Audit.{AuditData, AuditUserInfo}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.{AuthenticationNeededException, ObjectNotFoundException}
import myproject.iam.Users.{Guest, User, UserGeneric}
import myproject.common.Runtime.ec

import scala.concurrent.Future

object ApiMapper {

  def async[A](future: => Future[A]) = Future(1) flatMap(_ => future)

  def dispatchRequest(
    user: UserGeneric,
    functionName: String,
    clientIp: Option[String])(implicit params: ReifiedDataWrapper): Future[Any] = {

    def processInsecure(function: ApiFunction): Future[Any] = function.process(params, AuditData(clientIp, None))

    def processSecure(function: ApiFunction): Future[Any] = user match {

      case Guest() =>
        Future.failed(AuthenticationNeededException(s"Access to function `${function.name}` requires valid authentication"))

      case u: User =>
        function.process(params, u, AuditData(clientIp, Some(AuditUserInfo(u))))
    }

    /* We search a function with the corresponding name and execute it */
    ApiFunctionsRegistry.find(functionName) match {
      case None => Future.failed(ObjectNotFoundException(s"Function with name `$functionName` was not found"))
      case Some(method) if !method.secured =>
        async(processInsecure(method))
      case Some(method) =>
        async(processSecure(method))
    }
  }
}
