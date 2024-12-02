package com.annex.quote.repository

import zio.*
import scala.io.Source

enum FactorType:
  case YearBuilt, RoofAge, WindDeductible, AOPDeductible

case class Factor(
    factorType: FactorType,
    fieldName: String,
    value: BigDecimal
)

trait RatingRepository:
  def getYearBuiltFactor(age: Int): IO[String, BigDecimal]
  def getRoofAgeFactor(age: Int): IO[String, BigDecimal]
  def getWindDeductibleFactor(state: String, deductiblePercent: Int): IO[String, BigDecimal]
  def getAOPDeductibleFactor(deductible: Int): IO[String, BigDecimal]

class RatingRepositoryLive(
    factors: Ref[Map[FactorType, Map[String, BigDecimal]]]
) extends RatingRepository:

  def getYearBuiltFactor(age: Int): IO[String, BigDecimal] =
    factors.get.map(_.get(FactorType.YearBuilt).flatMap(_.get(age.toString)))
      .map(_.toRight(s"Year built factor not found for age: $age"))
      .absolve

  def getRoofAgeFactor(age: Int): IO[String, BigDecimal] =
    factors.get.map(_.get(FactorType.RoofAge).flatMap(_.get(age.toString)))
      .map(_.toRight(s"Roof age factor not found for age: $age"))
      .absolve

  def getWindDeductibleFactor(state: String, deductiblePercent: Int): IO[String, BigDecimal] =
    factors.get.map(_.get(FactorType.WindDeductible).flatMap(_.get(s"$deductiblePercent%")))
      .map(_.toRight(s"Wind deductible factor not found for $deductiblePercent%"))
      .absolve

  def getAOPDeductibleFactor(deductible: Int): IO[String, BigDecimal] =
    factors.get.map(_.get(FactorType.AOPDeductible).flatMap(_.get(deductible.toString)))
      .map(_.toRight(s"AOP deductible factor not found for $deductible"))
      .absolve

object RatingRepository:
  val live: ZLayer[Any, String, RatingRepository] =
    ZLayer.scoped {
      for
        windFactors <- loadFactors("wind_factors.csv").mapError(_.getMessage)
        aopFactors <- loadFactors("aop_factors.csv").mapError(_.getMessage)
        ref <- Ref.make(mergeMaps(windFactors, aopFactors))
      yield RatingRepositoryLive(ref)
    }

  private def loadFactors(filename: String): IO[Throwable, Map[FactorType, Map[String, BigDecimal]]] =
    ZIO.scoped {
      ZIO.acquireRelease(
        ZIO.attempt {
          val url = Option(getClass.getClassLoader.getResource(filename))
            .getOrElse(throw new RuntimeException(s"Resource $filename not found"))
          Source.fromURL(url)
        }
      )(source => ZIO.succeed(source.close())).flatMap { source =>
        ZIO.attempt {
          source.getLines().drop(1) // Skip header
            .map { line =>
              val Array(typeStr, fieldName, factorStr) = line.split(",").map(_.trim)
              val factorType = typeStr match
                case "Year Built" => FactorType.YearBuilt
                case "Roof Age" => FactorType.RoofAge
                case s if s.startsWith("Wind Deductible") => FactorType.WindDeductible
                case "AOP Deductible" => FactorType.AOPDeductible
                case other => throw new RuntimeException(s"Unknown factor type: $other")
              
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

  private def mergeMaps(
      m1: Map[FactorType, Map[String, BigDecimal]],
      m2: Map[FactorType, Map[String, BigDecimal]]
  ): Map[FactorType, Map[String, BigDecimal]] =
    (m1.keySet ++ m2.keySet).map { key =>
      key -> (m1.getOrElse(key, Map.empty) ++ m2.getOrElse(key, Map.empty))
    }.toMap 