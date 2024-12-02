package com.annex.quote

import com.annex.domain.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import zio.*

object QuoteEndpoints:
  private val baseEndpoint = endpoint.in("api" / "homeowners_lite")

  val calculateQuoteEndpoint: PublicEndpoint[QuoteRequest, String, QuoteResult, Any] =
    baseEndpoint.post
      .in("quote")
      .in(jsonBody[QuoteRequest])
      .out(jsonBody[QuoteResult])
      .errorOut(stringBody)
      .description("Calculate insurance quote for a home")
      .name("calculateQuote")
      .tag("Quotes")