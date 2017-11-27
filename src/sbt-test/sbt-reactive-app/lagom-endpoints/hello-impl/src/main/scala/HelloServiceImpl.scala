package lagomendpoints.impl

import lagomendpoints.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceCall

import scala.concurrent.{ ExecutionContext, Future }

class HelloServiceImpl(implicit ec: ExecutionContext) extends HelloService {
  override def hello(id: String) = ServiceCall { _ =>
    Future { "Hello, " + id }
  }
}
