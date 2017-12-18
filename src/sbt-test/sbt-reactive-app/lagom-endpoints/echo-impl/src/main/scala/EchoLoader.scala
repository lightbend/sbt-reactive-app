package lagomendpoints.impl

import lagomendpoints.api.EchoService
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext, LagomApplicationLoader }
import play.api.libs.ws.ahc.AhcWSComponents

class EchoLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new EchoApplication(context) with ConfigurationServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new EchoApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[EchoService])
}

abstract class EchoApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with AhcWSComponents {
  override lazy val lagomServer = serverFor[EchoService](new EchoServiceImpl)
}
