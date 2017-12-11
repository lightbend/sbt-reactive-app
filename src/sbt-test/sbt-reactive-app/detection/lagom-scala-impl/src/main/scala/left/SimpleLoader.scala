package left

import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServer }
import play.api.libs.ws.ahc.AhcWSComponents

class SimpleLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new SimpleApplication(context) with ConfigurationServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new SimpleApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[SimpleService])
}

abstract class SimpleApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[SimpleService](new SimpleServiceImpl)
}
