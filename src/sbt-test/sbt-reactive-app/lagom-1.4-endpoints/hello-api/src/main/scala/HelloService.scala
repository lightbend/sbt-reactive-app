package lagomendpoints.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }

trait HelloService extends Service {
  def hello(id: String): ServiceCall[NotUsed, String]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("hello")
      .withCalls(
        pathCall("/api/hello/:id", hello _))
      .withAutoAcl(true)
    // @formater:on
  }
}
