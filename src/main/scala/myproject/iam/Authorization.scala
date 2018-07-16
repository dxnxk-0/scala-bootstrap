package myproject.iam

import myproject.common.AccessRefusedException
import myproject.iam.Companies.Company
import myproject.iam.Users._

object Authorization {

  sealed trait AccessGranted
  case object AccessGranted extends AccessGranted

  def authzLoginAccess(user: User) = Right(AccessGranted)

  def authzUserCreation(author: User, user: User) = author match {
    case User(_, _, _, user.companyId, UserRole.Admin) => Right(AccessGranted)
    case _ => Left(AccessRefusedException(s"user with id ${author.id} is not granted to create a new user in the company ${user.companyId}"))
  }

  def authzUserWrite(author: User, user: User, update: UserUpdate) = author match {
    case User(_, _, _, user.companyId, UserRole.Admin) => Right(AccessGranted)
    case User(user.id, _, _, _, _) => update match {
      case UpdateLogin(_) => Right(AccessGranted)
      case UpdatePassword(_) => Right(AccessGranted)
      case UpdateRole(_) => Left(AccessRefusedException(s"user with id ${author.id} is not authorized to update his role"))
    }
    case _ => Left(AccessRefusedException(s"user with id ${author.id} is not authorized to update user with id ${user.id}"))
  }

  def authzUserRead(author: User, user: User) = author match {
    case User(_, _, _, user.companyId, _) => Right(AccessGranted)
    case _ => Left(AccessRefusedException(s"user with id ${author.id} is not authorized to see users of other companies"))
  }

  def authzCompanyCreation(author: User, company: Company) = ???

  def authzCompanyWrite(author: User, company: Company) = author match {
    case User(_, _, _, company.id, UserRole.Admin) => Right(AccessGranted)
    case _ => Left(AccessRefusedException(s"user with id ${author.id} is not authorized to update the company ${company.id}"))
  }

  def authzCompanyRead(author: User, company: Company) = author match {
    case User(_, _, _, company.id, _) => Right(AccessGranted)
    case _ => Left(AccessRefusedException(s"user with id ${author.id} is not authorized to access company ${company.id} details"))
  }
}

