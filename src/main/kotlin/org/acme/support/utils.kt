package org.acme.support

import io.netty.handler.codec.http.HttpResponseStatus
import java.time.Instant

object Address {
  //
  // External Addresses

  //
  // Internal Addresses
}

object Config {
  const val BODY_LIMIT: Long = 512

  const val HTTP_PORT: Int = 8080
}

object Message {
  const val UNEXPECTED: String = "Something happened on our end – it wasn't your fault. We've been notified."
}

sealed class HttpResponse(val status: HttpResponseStatus, val message: String, val path: String) {
  val timestamp: Long = Instant.now().toEpochMilli()

  class ClientError(
      status: HttpResponseStatus = HttpResponseStatus.BAD_REQUEST,
      message: String,
      path: String
  ) : HttpResponse(status, message, path)

  class ServerError(
      status: HttpResponseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR,
      message: String,
      path: String
  ) : HttpResponse(status, message, path)
}
