package org.oahega.com.env;

import android.util.Log;

public class Logger {

  private final String tag;

  public Logger(String tag) {
    this.tag = tag;
  }

  public void v(String text) {
      Log.v(tag, text);
  }

  public void d( String text) {
      Log.d(tag, text);

  }
}
