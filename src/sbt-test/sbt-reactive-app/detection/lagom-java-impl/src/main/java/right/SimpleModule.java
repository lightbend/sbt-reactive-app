package right;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;

public class SimpleModule extends AbstractModule implements ServiceGuiceSupport {

  @Override
  protected void configure() {
    bindService(SimpleService.class, SimpleServiceImpl.class);
  }
}
