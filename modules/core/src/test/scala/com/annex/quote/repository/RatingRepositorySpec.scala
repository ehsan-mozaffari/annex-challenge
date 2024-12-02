package com.annex.quote.repository

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault

object RatingRepositorySpec extends ZIOSpecDefault:

  override def spec = suite("RatingRepository")(
    test("should load and retrieve wind factors") {
      for
        repo               <- ZIO.service[RatingRepository]
        yearBuiltFactor    <- repo.getFactor(RatingType.Wind, FactorType.YearBuilt, "10")
        roofAgeFactor      <- repo.getFactor(RatingType.Wind, FactorType.RoofAge, "5")
        windDeductibleTXFL <- repo.getFactor(RatingType.Wind, FactorType.WindDeductibleTX_FL, "5")
      yield assertTrue(
        yearBuiltFactor == BigDecimal("0.8"),
        roofAgeFactor == BigDecimal("1.0"),
        windDeductibleTXFL == BigDecimal("1.0")
      )
    },
    test("should load and retrieve AOP factors") {
      for
        repo            <- ZIO.service[RatingRepository]
        yearBuiltFactor <- repo.getFactor(RatingType.AOP, FactorType.YearBuilt, "10")
        roofAgeFactor   <- repo.getFactor(RatingType.AOP, FactorType.RoofAge, "5")
        aopDeductible   <- repo.getFactor(RatingType.AOP, FactorType.AOPDeductible, "5000")
      yield assertTrue(
        yearBuiltFactor == BigDecimal("0.800"),
        roofAgeFactor == BigDecimal("1.0"),
        aopDeductible == BigDecimal("0.950")
      )
    },
    test("should fail for non-existent factors") {
      for
        repo   <- ZIO.service[RatingRepository]
        result <- repo.getFactor(RatingType.Wind, FactorType.YearBuilt, "999").either
      yield assert(result)(isLeft(containsString("Factor not found")))
    }
  ).provide(RatingRepository.live)
