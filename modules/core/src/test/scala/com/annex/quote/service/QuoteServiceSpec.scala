package com.annex.quote.service

import com.annex.quote.*
import com.annex.quote.repository.RatingRepository
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import zio.test.ZIOSpecDefault

import java.time.LocalDate

object QuoteServiceSpec extends ZIOSpecDefault:

  val sampleRequest = QuoteRequest(
    riskAddress = Address(
      street = "123 Main St",
      city = "Newark",
      state = State.NJ,
      zipCode = "07102"
    ),
    policyEffectiveDate = LocalDate.of(2024, 1, 1),
    roofAge = 5,
    yearBuilt = 2000,
    dwellingLimit = 500000,
    dwellingDeductiblePercent = 5,
    contentsLimit = 100000,
    contentsDeductible = 5000
  )

  override def spec = suite("QuoteService")(
    test("should calculate quote for valid request") {
      for
        service <- ZIO.service[QuoteService]
        result  <- service.calculateQuote(sampleRequest)
      yield assertTrue(
        result match
          case QuoteResult.Quoted(premium) => premium > BigDecimal(0)
          case _                           => false
      )
    },
    test("should validate roof age") {
      for
        service <- ZIO.service[QuoteService]
        result  <- service.calculateQuote(sampleRequest.copy(roofAge = 101)).either
      yield assert(result)(isLeft(containsString("Roof age must be between")))
    },
    test("should validate year built") {
      for
        service <- ZIO.service[QuoteService]
        result  <- service.calculateQuote(sampleRequest.copy(yearBuilt = 1900)).either
      yield assert(result)(isLeft(containsString("Building age must not exceed")))
    },
    test("should calculate premium according to example") {
      val exampleRequest = QuoteRequest(
        riskAddress = Address(
          street = "123 Main St",
          city = "Newark",
          state = State.FL,
          zipCode = "07102"
        ),
        policyEffectiveDate = LocalDate.of(2024, 1, 1),
        roofAge = 10,                  // Factor 1.05 for both Wind and AOP
        yearBuilt = 2014,              // Age 10 years -> Factor 0.8 for both
        dwellingLimit = 100000,
        dwellingDeductiblePercent = 5, // Factor 1.0 for Wind
        contentsLimit = 20000,
        contentsDeductible = 6000      // Factor 0.94 for AOP
      )

      for
        service <- ZIO.service[QuoteService]
        result  <- service.calculateQuote(exampleRequest)
      yield assertTrue(
        result match
          case QuoteResult.Quoted(premium) =>
            // Wind: 0.5 * 120000/100 * 1.05 * 0.8 * 1.0 = 504.00
            // AOP:  0.5 * 120000/100 * 1.05 * 0.8 * 0.94 = 473.76
            // Total: 977.76
            println(s"premium: $premium")
            premium == BigDecimal("977.76")
          case _                           => false
      )
    }
  ).provide(
    QuoteService.layer,
    RatingRepository.live
  )
