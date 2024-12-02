package com.annex.quote.service

import com.annex.quote.repository.RatingRepository
import com.annex.quote.*
import zio.*
import com.annex.quote.repository.* 

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
      windYearBuiltFactor <- ratingRepository.getFactor(RatingType.Wind, FactorType.YearBuilt, age.toString)
      aopYearBuiltFactor <- ratingRepository.getFactor(RatingType.AOP, FactorType.YearBuilt, age.toString)
      windRoofAgeFactor <- ratingRepository.getFactor(RatingType.Wind, FactorType.RoofAge, request.roofAge.toString)
      aopRoofAgeFactor <- ratingRepository.getFactor(RatingType.AOP, FactorType.RoofAge, request.roofAge.toString)
      windDeductibleFactor <- request.riskAddress.state match
        case State.TX | State.FL => ratingRepository.getFactor(RatingType.Wind, FactorType.WindDeductibleTX_FL, request.dwellingDeductiblePercent.toString)
        case State.VA | State.NJ => ratingRepository.getFactor(RatingType.Wind, FactorType.WindDeductibleVA_NJ, request.dwellingDeductiblePercent.toString)
      aopDeductibleFactor <- ratingRepository.getFactor(RatingType.AOP, FactorType.AOPDeductible, request.contentsDeductible.toString)

      windPremium = calculateWindPremium(tiv, windYearBuiltFactor, windRoofAgeFactor, windDeductibleFactor)
      aopPremium = calculateAOPPremium(tiv, aopYearBuiltFactor, aopRoofAgeFactor, aopDeductibleFactor)
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