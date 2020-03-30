package org.oahega.com;

import android.app.Application;
import org.oahega.com.preference.Preference;

public class MainApplication extends Application {

  private static Application application;

  @Override
  public void onCreate() {
    super.onCreate();
    application = this;

    Preference.initializeInstance(this);
  }

  public static Application getApplication() {
    return application;
  }
}
