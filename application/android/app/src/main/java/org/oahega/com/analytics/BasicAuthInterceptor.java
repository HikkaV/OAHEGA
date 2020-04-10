package org.oahega.com.analytics;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class BasicAuthInterceptor implements Interceptor, Authenticator {

  private String credentials;

  public BasicAuthInterceptor(String user, String password) {
    this.credentials = Credentials.basic(user, password);
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Request authenticatedRequest = request.newBuilder()
        .header("Authorization", credentials).build();
    return chain.proceed(authenticatedRequest);
  }

  @Override
  public Request authenticate(Route route, Response response) throws IOException {
    return null;
  }
}
