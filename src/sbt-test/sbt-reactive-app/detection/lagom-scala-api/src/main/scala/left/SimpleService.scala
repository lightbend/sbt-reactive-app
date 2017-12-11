package left

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }

trait SimpleService extends Service {
  def simple(input: String): ServiceCall[NotUsed, String]

  override final def descriptor: Descriptor = {
    import Service._
    named("left")
      .withCalls(restCall(Method.GET, "/left/:text", simple _))
      .withAutoAcl(true)
  }
}
