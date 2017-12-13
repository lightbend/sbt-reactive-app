package right;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import java.util.concurrent.CompletableFuture;

public class SimpleServiceImpl implements SimpleService {

  @Override
  public ServiceCall<NotUsed, String> simple(String input) {
    return req -> {
      CompletableFuture<String> result = CompletableFuture.completedFuture(input.toUpperCase());
      return result;
    };
  }
}
