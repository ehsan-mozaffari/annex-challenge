package com.annex.quote.controller

import com.annex.quote.*
import com.annex.quote.repository.RatingRepository
import com.annex.quote.service.QuoteService
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.test.*
import zio.test.TestAspect.*
import zio.test.ZIOSpecDefault
import com.annex.quote.controller.*

object QuoteControllerSpec extends ZIOSpecDefault:

  override def spec = suite("QuoteController")(
    test("should initialize with valid routes") {
      for
        controller <- ZIO.service[QuoteController]
        routes      = controller.routes
      yield assertTrue(
        routes.nonEmpty
      )
    }
  ).provide(
    QuoteController.layer,
    QuoteService.layer,
    RatingRepository.live
  )
