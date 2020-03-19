package org.acme.verticle

import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import mu.KLogging
import org.acme.support.Config
import org.acme.support.HttpResponse
import org.acme.support.Message

class HttpServerVerticle : AbstractVerticle() {
  private companion object : KLogging()

  override fun start() {
    // HTTP server
    val options = HttpServerOptions()
        .setHost(config().getString("host", "localhost"))
        .setPort(config().getInteger("port", Config.HTTP_PORT))
    vertx.createHttpServer(options)
        .requestHandler(router())
        .listen {
          val url = "http${if (options.isSsl) "s" else ""}://${options.host}:${options.port}"
          logger.trace { "HTTP server started at $url" }
        }
  }

  override fun stop() {
    logger.debug { "Stopping ${this.javaClass.simpleName} (ID [${deploymentID()}])...DONE" }
  }

  //-----------------------------------------------------------------------------------------------
  //
  //-----------------------------------------------------------------------------------------------

  private fun router() = Router.router(vertx).apply {
    // Body handler(s)
    route().handler(BodyHandler.create().setBodyLimit(config().getLong("body_limit", Config.BODY_LIMIT)))

    // Failure handler(s)
    route("/api/*").failureHandler {
      val response = HttpResponse.ServerError(message = Message.UNEXPECTED, path = it.request().path())
      it.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(response.status.code())
          .end(JsonObject.mapFrom(response).encode())
    }
  }
}
