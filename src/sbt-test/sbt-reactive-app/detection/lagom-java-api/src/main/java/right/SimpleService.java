package right;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import static com.lightbend.lagom.javadsl.api.Service.*;

public interface SimpleService extends Service {

  ServiceCall<NotUsed, String> simple(String input);

  @Override
  default Descriptor descriptor() {
    // @formatter:off
    return named("right").withCalls(
        pathCall("/right/:text", this::simple)
      ).withAutoAcl(true);
    // @formatter:on
  }
}
