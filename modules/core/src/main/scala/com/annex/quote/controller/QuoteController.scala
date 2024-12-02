package com.annex.quote.controller

import com.annex.quote.QuoteEndpoints
import com.annex.quote.service.QuoteService
import com.annex.quote.*
import zio.*
import sttp.tapir.ztapir.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

trait QuoteController:
  def routes: List[ZServerEndpoint[Any, ZioStreams & WebSockets]]

class QuoteControllerLive(quoteService: QuoteService) extends QuoteController:

  lazy val calculateQuote: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    QuoteEndpoints.calculateQuoteEndpoint.zServerLogic { request =>
      quoteService.calculateQuote(request)
    }

  lazy val routes = List(calculateQuote)

object QuoteController:

  val layer: ZLayer[QuoteService, Nothing, QuoteController] =
    ZLayer.fromFunction(QuoteControllerLive(_))
