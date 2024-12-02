package com.annex

import com.annex.quote.QuoteEndpoints
import com.annex.quote.controller.*
import com.annex.quote.repository.RatingRepository
import com.annex.quote.service.QuoteService
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.Server
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.ztapir.*

object Main extends ZIOAppDefault:
  val serverConfig = Server.Config.default.port(8080)
  val serverLayer  = ZLayer.succeed(serverConfig) >>> Server.live

  val appLayer = RatingRepository.live >>> QuoteService.layer >>> QuoteController.layer

  val serverProgram =
    for
      _               <- Console.printLine("Starting Annex Insurance API server...")
      quoteController <- ZIO.service[QuoteController]
      api              = quoteController.routes
      swaggerApi       = SwaggerInterpreter().fromServerEndpoints(api, "Annex Insurance API", "1.0.0")
      serverEndpoints  = api ++ swaggerApi
      httpApp          = ZioHttpInterpreter().toHttp(serverEndpoints)
      _               <- Console.printLine("Server started at http://localhost:8080")
      _               <- Console.printLine("Swagger UI available at http://localhost:8080/docs")
      _               <- Server.serve(httpApp)
    yield ()

  override val run =
    serverProgram
      .provide(
        serverLayer,
        appLayer
      )
      .catchAll { err =>
        Console.printLineError(s"Server failed to start: $err").as(ExitCode.failure)
      }
      .as(ExitCode.success)
