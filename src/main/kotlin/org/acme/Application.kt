package org.acme

import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import mu.KLogging
import org.acme.verticle.HttpServerVerticle
import kotlin.reflect.KClass

class Application : AbstractVerticle() {
  private companion object : KLogging()

  override fun start(startPromise: Promise<Void>) {
    CompositeFuture.all(listOf(deploy(HttpServerVerticle::class).future()))
        .setHandler {
          if (it.succeeded()) {
            startPromise.complete()
          } else {
            startPromise.fail(it.cause())
          }
        }.onSuccess {
          logger.info { "Verticle(s) deployment...DONE" }
        }.onFailure {
          logger.warn { "Verticle(s) deployment...FAILED" }
        }
  }

  override fun stop(stopPromise: Promise<Void>) {
    logger.debug("Undeploying verticle(s)...DONE")
    logger.info("Application stopped successfully. Enjoy the elevator music while we're offline...")
    stopPromise.complete()
  }

  //-----------------------------------------------------------------------------------------------
  //
  //-----------------------------------------------------------------------------------------------

  private fun deploy(verticle: KClass<out AbstractVerticle>): Promise<Void> {
    val promise: Promise<Void> = Promise.promise()
    val options = DeploymentOptions(config().getJsonObject("${verticle.qualifiedName}"))
    vertx.deployVerticle(verticle.qualifiedName, options) {
      if (it.succeeded()) {
        logger.debug { "${verticle.simpleName} started successfully (deployment ID [${it.result()}])" }
        promise.complete()
      } else {
        logger.error { "${verticle.simpleName} deployment failed due to: ${it.cause()}" }
        promise.fail(it.cause())
      }
    }
    return promise
  }
}
