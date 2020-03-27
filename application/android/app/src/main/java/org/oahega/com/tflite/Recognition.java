package org.oahega.com.tflite;

import android.graphics.RectF;

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