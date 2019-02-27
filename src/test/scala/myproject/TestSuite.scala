package myproject

import myproject.iam.{StructureSpecs, _}
import org.scalatest.Suites

class TestSuite extends Suites(
  new ChannelSpecs,
  new GroupSpecs,
  new UserSpecs,
  new TokenSpecs,
  new OrganizationSpecs,
  new AuthorizationSpecs,
  new StructureSpecs,
  new DataLoadingSpecs)
