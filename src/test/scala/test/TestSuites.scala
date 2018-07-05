package test

import myproject.web.api.methods.{ApiLoginSpecs, ApiWelcomeSpecs}
import org.scalatest.Suites

class TestSuites extends Suites(
  new ApiLoginSpecs,
  new ApiWelcomeSpecs)
