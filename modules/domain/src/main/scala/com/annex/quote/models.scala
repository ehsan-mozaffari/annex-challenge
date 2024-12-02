package com.annex.quote

import zio.json.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*

import java.time.LocalDate

enum State derives JsonCodec:
  case NJ, VA, TX, FL

object State:
  given Schema[State] = Schema.derivedEnumeration[State].defaultStringBased

case class Address(
  street: String,
  city: String,
  state: State,
  zipCode: String
) derives JsonCodec

object Address:
  given Schema[Address] = Schema.derived[Address]

case class QuoteRequest(
  riskAddress: Address,
  policyEffectiveDate: LocalDate,
  roofAge: Int,
  yearBuilt: Int,
  dwellingLimit: Int,
  dwellingDeductiblePercent: Int,
  contentsLimit: Int,
  contentsDeductible: Int
) derives JsonCodec:

  def validate: Either[String, QuoteRequest] =
    for
      _ <- validateRoofAge
      _ <- validateYearBuilt
      _ <- validateDwellingLimit
      _ <- validateDwellingDeductible
      _ <- validateContentsLimit
      _ <- validateContentsDeductible
    yield this

  private def validateRoofAge =
    if roofAge > 0 && roofAge <= 100 then Right(())
    else Left("Roof age must be between 1 and 100 years")

  private def validateYearBuilt =
    val age = policyEffectiveDate.getYear - yearBuilt
    if age <= 100 then Right(())
    else Left("Building age must not exceed 100 years from policy effective date")

  private def validateDwellingLimit =
    if dwellingLimit > 0 && dwellingLimit <= 1_000_000 then Right(())
    else Left("Dwelling limit must be between 1 and 1,000,000")

  private def validateDwellingDeductible =
    if dwellingDeductiblePercent >= 1 && dwellingDeductiblePercent <= 10 then Right(())
    else Left("Dwelling deductible must be between 1% and 10%")

  private def validateContentsLimit =
    if contentsLimit > 0 && contentsLimit <= 250_000 then Right(())
    else Left("Contents limit must be between 1 and 250,000")

  private def validateContentsDeductible =
    if contentsDeductible >= 1_000 && contentsDeductible <= 10_000 && contentsDeductible % 1_000 == 0 then Right(())
    else Left("Contents deductible must be between 1,000 and 10,000 in increments of 1,000")

object QuoteRequest:
  given Schema[QuoteRequest] = Schema.derived[QuoteRequest]

enum QuoteResult:
  case Declined(reason: String)
  case Quoted(premium: BigDecimal)

object QuoteResult:
  given JsonCodec[QuoteResult] = DeriveJsonCodec.gen
  given Schema[QuoteResult]    = Schema.derived[QuoteResult]
