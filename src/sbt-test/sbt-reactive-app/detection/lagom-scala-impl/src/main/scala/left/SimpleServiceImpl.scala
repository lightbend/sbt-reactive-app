package left

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.Future

class SimpleServiceImpl extends SimpleService {
  override def simple(input: String): ServiceCall[NotUsed, String] = ServiceCall { _ =>
    Future.successful(input.toUpperCase)
  }
}
