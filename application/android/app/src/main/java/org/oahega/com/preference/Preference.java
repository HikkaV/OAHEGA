package org.oahega.com.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Preference {

  private static final String TAG = "Preserence";

  private static final String PREF_NAME = "SETTINGS";
  public static final String KEY_MIN_PERCENT_CLASSIF = "KEY_MIN_PERCENT_CLASSIF";
  public static final String KEY_MIN_PERCENT_DETECT = "KEY_MIN_PERCENT_DETECT";
  private static final String KEY_AVERAGE = "KEY_AVERAGE";

  private static Preference sInstance;

  private final SharedPreferences mPref;


  private Preference(Context context) {
    mPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    Log.d(TAG, "constructor -> instance created. Mode - private. Name: " + PREF_NAME);
  }

  public static synchronized void initializeInstance(Context context) {
    if (sInstance == null) {
      Log.d(TAG, "initializeInstance");
      sInstance = new Preference(context);
    }
  }

  public static synchronized Preference getInstance() {
    Log.d(TAG, "getInstance");

    if (sInstance == null) {
      throw new IllegalStateException(Preference.class.getSimpleName() +
          " is not initialized, call initializeInstance(..) method first.");
    }
    return sInstance;
  }


  public int getMinPercentDetect() {
    return mPref.getInt(KEY_MIN_PERCENT_DETECT, 70);
  }

  public void setMinPercentDetect(int percents) {
    mPref.edit()
        .putInt(KEY_MIN_PERCENT_DETECT, percents)
        .commit();
  }
  public int getMinPercentClassif() {
    return mPref.getInt(KEY_MIN_PERCENT_CLASSIF, 70);
  }

  public void setMinPercentClassif(int percents) {
    mPref.edit()
        .putInt(KEY_MIN_PERCENT_CLASSIF, percents)
        .commit();
  }
  public int getAverage() {
    return mPref.getInt(KEY_AVERAGE, 0);
  }

  public void setAverage(int average) {
    mPref.edit()
        .putInt(KEY_AVERAGE, average)
        .commit();
  }

}
