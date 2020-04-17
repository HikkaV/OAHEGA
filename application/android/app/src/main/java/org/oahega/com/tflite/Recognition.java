package org.oahega.com.tflite;

import android.graphics.RectF;
import android.provider.Settings;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.oahega.com.MainApplication;

public class Recognition {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private final String id;

    /** Display name for the recognition. */
    private String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private  Float confidence;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;

    public Recognition(
            final String id, final String title, final Float confidence, final RectF location) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public Float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

    public void doubleValueHeight(double value) {
        location = new RectF(location.left, Math.round(location.top * value),
            location.right, Math.round(location.bottom * value));
    }

    public void doubleValueWeight(double value) {
        location = new RectF(Math.round(location.left * value), location.top,
            Math.round(location.right * value), location.bottom);
    }

    public String toGson() throws NoSuchAlgorithmException {
        String userName = Settings.System
            .getString(MainApplication.getApplication().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(userName.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        String hashtext = bigInt.toString(16);
// Now we need to zero pad it if you actually want the full 32 chars.
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }

        return "{ \"username\": \"" + hashtext + "\",\n"
            + "    \"emotion\": \"" + title + "\",\n"
            + "    \"date\": \"" + getDate() + "\"}";

    }

    private String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String date = dateFormat.format(new Date(System.currentTimeMillis()));
        return date;
    }

    @Override
    public String toString() {
        String resultString = "";
        if (id != null) {
            resultString += "[" + id + "] ";
        }

        if (title != null) {
            resultString += title + " ";
        }

        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f);
        }

        if (location != null) {
            resultString += location + " ";
        }

        return resultString.trim();
    }
}