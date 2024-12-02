package com.annex.quote.service

import com.annex.quote.repository.RatingRepository
import com.annex.quote.*
import zio.*

trait QuoteService:
  def calculateQuote(request: QuoteRequest): IO[String, QuoteResult]

class QuoteServiceLive(ratingRepository: RatingRepository) extends QuoteService:
  private val BaseRate = BigDecimal("0.50")

  def calculateQuote(request: QuoteRequest): IO[String, QuoteResult] =
    for
      validated <- ZIO.fromEither(request.validate)
      result <- calculatePremium(validated)
    yield result

  private def calculatePremium(request: QuoteRequest): IO[String, QuoteResult] =
    val tiv = request.dwellingLimit + request.contentsLimit
    val age = request.policyEffectiveDate.getYear - request.yearBuilt

    for
      yearBuiltFactor <- ratingRepository.getYearBuiltFactor(age)
      roofAgeFactor <- ratingRepository.getRoofAgeFactor(request.roofAge)
      windDeductibleFactor <- ratingRepository
        .getWindDeductibleFactor(request.riskAddress.state.toString, request.dwellingDeductiblePercent)
      aopDeductibleFactor <- ratingRepository
        .getAOPDeductibleFactor(request.contentsDeductible)

      windPremium = calculateWindPremium(tiv, yearBuiltFactor, roofAgeFactor, windDeductibleFactor)
      aopPremium = calculateAOPPremium(tiv, yearBuiltFactor, roofAgeFactor, aopDeductibleFactor)
      totalPremium = windPremium + aopPremium
    yield QuoteResult.Quoted(totalPremium)

  private def calculateWindPremium(
      tiv: Int,
      yearBuiltFactor: BigDecimal,
      roofAgeFactor: BigDecimal,
      deductibleFactor: BigDecimal
  ): BigDecimal =
    BaseRate * (tiv / 100) * yearBuiltFactor * roofAgeFactor * deductibleFactor

  private def calculateAOPPremium(
      tiv: Int,
      yearBuiltFactor: BigDecimal,
      roofAgeFactor: BigDecimal,
      deductibleFactor: BigDecimal
  ): BigDecimal =
    BaseRate * (tiv / 100) * yearBuiltFactor * roofAgeFactor * deductibleFactor

object QuoteService:
  val layer: ZLayer[RatingRepository, Throwable, QuoteService] =
    ZLayer.fromFunction(QuoteServiceLive(_)) 