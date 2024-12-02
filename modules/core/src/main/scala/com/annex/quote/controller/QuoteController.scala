package com.annex.quote.controller

import com.annex.quote.QuoteEndpoints

import com.annex.quote.service.QuoteService
import com.annex.quote.*
import zio.*
import sttp.tapir.ztapir.*

trait QuoteController:
  def routes: List[ZServerEndpoint[Any, String]]

class QuoteControllerLive(quoteService: QuoteService) extends QuoteController:

  private lazy val calculateQuote: ZServerEndpoint[Any, String] =
    QuoteEndpoints.calculateQuoteEndpoint.zServerLogic { request =>
      quoteService.calculateQuote(request)
    }

  lazy val routes: List[ZServerEndpoint[Any, String]] = calculateQuote :: Nil

object QuoteController:
  val layer: ZLayer[QuoteService, Nothing, QuoteController] =
    ZLayer.fromFunction(QuoteControllerLive(_))

