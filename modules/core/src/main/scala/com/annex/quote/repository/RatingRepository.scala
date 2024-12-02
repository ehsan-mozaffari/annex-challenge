package com.annex.quote.repository

import zio.*
import scala.io.Source

enum RatingType:
  case Wind, AOP

enum FactorType:
  case YearBuilt, RoofAge, WindDeductibleTX_FL, WindDeductibleVA_NJ, AOPDeductible

case class Factor(
  factorType: FactorType,
  fieldName: String,
  value: BigDecimal
)

trait RatingRepository:
  def getFactor(ratingType: RatingType, factorType: FactorType, fieldName: String): IO[String, BigDecimal]

class RatingRepositoryLive(
  aopFactors: Ref[Map[FactorType, Map[String, BigDecimal]]],
  windFactors: Ref[Map[FactorType, Map[String, BigDecimal]]]
) extends RatingRepository:

  def getFactor(ratingType: RatingType, factorType: FactorType, fieldName: String): IO[String, BigDecimal] =
    val factors = ratingType match
      case RatingType.AOP  => aopFactors
      case RatingType.Wind => windFactors

    val fieldNameStr = factorType match
      case FactorType.WindDeductibleTX_FL | FactorType.WindDeductibleVA_NJ => s"$fieldName%"
      case _                                                               => fieldName

    factors.get
      .map(_.get(factorType).flatMap(_.get(fieldNameStr)))
      .map(_.toRight(s"Factor not found for $factorType $fieldName"))
      .absolve

object RatingRepository:

  val live: ZLayer[Any, String, RatingRepository] =
    ZLayer.scoped {
      for
        windFactors <- loadFactors("wind_factors.csv").mapError(_.getMessage)
        aopFactors  <- loadFactors("aop_factors.csv").mapError(_.getMessage)
        windRef     <- Ref.make(windFactors)
        aopRef      <- Ref.make(aopFactors)
      yield RatingRepositoryLive(aopRef, windRef)
    }

  private def loadFactors(filename: String): IO[Throwable, Map[FactorType, Map[String, BigDecimal]]] =
    ZIO.scoped {
      ZIO
        .acquireRelease(
          ZIO.attempt {
            val url = Option(getClass.getClassLoader.getResource(filename))
              .getOrElse(throw new RuntimeException(s"Resource $filename not found"))
            Source.fromURL(url)
          }
        )(source => ZIO.succeed(source.close()))
        .flatMap { source =>
          ZIO.attempt {
            source
              .getLines()
              .drop(1) // Skip header
              .map { line =>
                val Array(typeStr, fieldName, factorStr) = line.split(",").map(_.trim)
                val factorType                           = typeStr match
                  case "Year Built"                               => FactorType.YearBuilt
                  case "Roof Age"                                 => FactorType.RoofAge
                  case s if s.startsWith("Wind Deductible TX-FL") => FactorType.WindDeductibleTX_FL
                  case s if s.startsWith("Wind Deductible VA-NJ") => FactorType.WindDeductibleVA_NJ
                  case "AOP Deductible"                           => FactorType.AOPDeductible
                  case other                                      => throw new RuntimeException(s"Unknown factor type: $other")

                Factor(factorType, fieldName, BigDecimal(factorStr))
              }
              .toList
              .groupBy(_.factorType)
              .view
              .mapValues(_.map(f => f.fieldName -> f.value).toMap)
              .toMap
          }
        }
    }
