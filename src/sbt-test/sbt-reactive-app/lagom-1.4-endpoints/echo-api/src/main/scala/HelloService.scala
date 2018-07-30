package lagomendpoints.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }

trait EchoService extends Service {
  def echo(id: String): ServiceCall[NotUsed, String]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("echo")
      .withCalls(
        pathCall("/api/echo/:id", echo _))
    // @formater:on
  }
}
