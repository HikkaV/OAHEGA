package org.oahega.com.analytics;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ServerApi {

  @POST("emotions/_doc")
  Call<String> sendSingle(@Body String text, @Header("Content-Type") String content);
}
