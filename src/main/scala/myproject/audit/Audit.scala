package myproject.audit

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.TimeManagement
import myproject.iam.Users.UserGeneric

object Audit {

  case class AuditData(ipAddress: Option[String], userInfo: Option[AuditUserInfo])

  case class AuditUserInfo(user: UserGeneric)

  sealed trait AuditOperation

  case class AuditLog(
    id: UUID,
    user: Option[UserGeneric],
    objectId: UUID,
    ipAddress: Option[String],
    time: LocalDateTime,
    auditOperation: AuditOperation,
    extendedData: Option[Map[String, Any]])


  def newAuditLog( /* Audit log constructor using the Audit data */
    objectId: UUID,
    auditData: AuditData,
    auditOperation: AuditOperation,
    extendedData: Option[Map[String, Any]]): AuditLog = {

    AuditLog(
      id = UUID.randomUUID(),
      user = auditData.userInfo.map(_.user),
      objectId = objectId,
      ipAddress = auditData.ipAddress,
      time = TimeManagement.getCurrentDateTime,
      auditOperation = auditOperation,
      extendedData = extendedData
    )
  }
}