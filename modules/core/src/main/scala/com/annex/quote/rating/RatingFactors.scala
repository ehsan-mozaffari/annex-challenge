package com.annex.core.rating

import zio.*
import zio.stream.*

import scala.io.Source

enum FactorType:
  case YearBuilt, RoofAge, WindDeductible, AOPDeductible

case class Factor(
    factorType: FactorType,
    fieldName: String,
    value: BigDecimal
)

trait RatingFactors:
  def getYearBuiltFactor(age: Int): Task[BigDecimal]
  def getRoofAgeFactor(age: Int): Task[BigDecimal]
  def getWindDeductibleFactor(state: String, deductiblePercent: Int): Task[BigDecimal]
  def getAOPDeductibleFactor(deductible: Int): Task[BigDecimal]

class RatingFactorsLive(
    factors: Ref[Map[FactorType, Map[String, BigDecimal]]]
) extends RatingFactors:

  def getYearBuiltFactor(age: Int): Task[BigDecimal] =
    factors.get.map(_.get(FactorType.YearBuilt).flatMap(_.get(age.toString)))
      .map(_.getOrElse(BigDecimal(1.0)))

  def getRoofAgeFactor(age: Int): Task[BigDecimal] =
    factors.get.map(_.get(FactorType.RoofAge).flatMap(_.get(age.toString)))
      .map(_.getOrElse(BigDecimal(1.0)))

  def getWindDeductibleFactor(state: String, deductiblePercent: Int): Task[BigDecimal] =
    factors.get.map(_.get(FactorType.WindDeductible).flatMap(_.get(s"$deductiblePercent%")))
      .map(_.getOrElse(BigDecimal(1.0)))

  def getAOPDeductibleFactor(deductible: Int): Task[BigDecimal] =
    factors.get.map(_.get(FactorType.AOPDeductible).flatMap(_.get(deductible.toString)))
      .map(_.getOrElse(BigDecimal(1.0)))

object RatingFactors:
  val live: ZLayer[Any, Throwable, RatingFactors] =
    ZLayer.scoped {
      for
        windFactors <- loadFactors("wind_factors.csv")
        aopFactors <- loadFactors("aop_factors.csv")
        ref <- Ref.make(mergeMaps(windFactors, aopFactors))
      yield RatingFactorsLive(ref)
    }

  private def loadFactors(filename: String): ZIO[Any, Throwable, Map[FactorType, Map[String, BigDecimal]]] =
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