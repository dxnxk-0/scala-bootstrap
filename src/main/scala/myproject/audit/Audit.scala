package myproject.audit

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.TimeManagement
import myproject.identity.User

case class AuditData(ipAddress: Option[String], userInfo: Option[AuditUserInfo])

case class AuditUserInfo(realUser: User, effectiveUser: User)

case class AuditLog(
    id: UUID,
    realUser: Option[User],
    effectiveUser: Option[User],
    objectId: UUID,
    ipAddress: Option[String],
    time: LocalDateTime,
    auditOperation: AuditOperation,
    extendedData: Option[Map[String, Any]])

object AuditLog {

  def apply( /* Audit log constructor using the Audit data */
    objectId: UUID,
    auditData: AuditData,
    auditOperation: AuditOperation,
    extendedData: Option[Map[String, Any]]): AuditLog = {

    AuditLog(
      id = UUID.randomUUID(),
      realUser = auditData.userInfo.map(_.realUser),
      effectiveUser = auditData.userInfo.map(_.effectiveUser),
      objectId = objectId,
      ipAddress = auditData.ipAddress,
      time = TimeManagement.getCurrentDateTime,
      auditOperation = auditOperation,
      extendedData = extendedData
    )
  }
}