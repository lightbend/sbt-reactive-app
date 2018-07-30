package lagomendpoints.impl

import lagomendpoints.api.EchoService
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.{ ExecutionContext, Future }

class EchoServiceImpl(implicit ec: ExecutionContext) extends EchoService {
  override def echo(id: String) = ServiceCall { _ =>
    Future.successful(id)
  }
}
