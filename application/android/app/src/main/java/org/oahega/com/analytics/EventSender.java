package org.oahega.com.analytics;

import androidx.annotation.NonNull;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.oahega.com.tflite.Recognition;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class EventSender {

  private static final int MAX_AMMOUT_OF_EVENTS = 50;

  private ArrayList<Recognition> events = new ArrayList<>();
  private static EventSender instance;
  private boolean isInProcess = false;


  private EventSender() {

  }

  public static EventSender getInstance() {
    if (instance == null) {
      instance = new EventSender();
    }
    return instance;
  }


  public void addEvent(Recognition recognition) {
    if (events.size() < MAX_AMMOUT_OF_EVENTS) {
      events.add(recognition);
    }
    if (!isInProcess) {
      sendEvent();
    }
  }

  private void sendEvent() {
    isInProcess = true;
    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    OkHttpClient.Builder client = new OkHttpClient.Builder();
    client.addInterceptor(interceptor);
    client.addInterceptor(new BasicAuthInterceptor("elastic", "oC5aD9M4YoFSa3aTZ95wlb5O"));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(
            "https://7dbae084dc32407eb37c291df0f34430.europe-west3.gcp.cloud.es.io:9243/")
        .client(client.build())
        .addConverterFactory(ScalarsConverterFactory.create())
        .build();
    ServerApi serverApi = retrofit.create(ServerApi.class);

    String gsontext = "null";
    try {
      gsontext = events.get(0).toGson();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }

    Call<String> call = serverApi.sendSingle(gsontext, "Application/json");
    call.enqueue(new Callback<String>() {
      @Override
      public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
        events.remove(0);
        if (events.size() > 0) {
          sendEvent();
        } else {
          isInProcess = false;
        }
      }

      @Override
      public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
        events.remove(0);
        if (events.size() > 0) {
          sendEvent();
        } else {
          isInProcess = false;
        }
      }
    });
  }

}