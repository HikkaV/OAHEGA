package org.oahega.com.utils;

import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;

public class Settings {

    private static Settings settings;
    private int minClassificationPercentToShow;
    private int minDetectionPercentToShow;
    private int numOfAvarage;
    private boolean isfront = false;
    private double width = 0;
    private double height = 0;
    private double ratio = 0.0;
    private boolean isBeforChange = true;

    private Settings(){
        this.minClassificationPercentToShow = 70;
        this.minDetectionPercentToShow = 70;
        this.numOfAvarage = 1;
        isfront = false;
    }

    public static Settings getInstance() {
        if(settings == null){
            settings = new Settings();
        }
        return settings;
    }


    public static Point getScreenSize() {
        int pxWidth;
        int pxHeight;
        DisplayMetrics outMetrics = Resources.getSystem().getDisplayMetrics();
        if (outMetrics.widthPixels < outMetrics.heightPixels
            || outMetrics.widthPixels > outMetrics.heightPixels) {
            pxWidth = outMetrics.widthPixels;
            pxHeight = outMetrics.heightPixels;
        } else {
            pxWidth = outMetrics.heightPixels;
            pxHeight = outMetrics.widthPixels;
        }
        return new Point(pxWidth, pxHeight);
    }

    public int getMinClassificationPercentToShow() {
        return minClassificationPercentToShow;
    }

    public void setMinClassificationPercentToShow(int minClassificationPercentToShow) {
        this.minClassificationPercentToShow = minClassificationPercentToShow;
    }

    public int getMinDetectionPercentToShow() {
        return minDetectionPercentToShow;
    }

    public void setMinDetectionPercentToShow(int minDetectionPercentToShow) {
        this.minDetectionPercentToShow = minDetectionPercentToShow;
    }

    public boolean isIsfront() {
        return isfront;
    }

    public void setIsfront(boolean isfront) {
        this.isfront = isfront;
    }

    public int getNumOfAvarage() {
        return numOfAvarage;
    }

    public void setNumOfAvarage(int numOfAvarage) {
        this.numOfAvarage = numOfAvarage;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setBeforChange(boolean beforChange) {
        isBeforChange = beforChange;
    }

    public boolean isBeforeChange() {

        return isBeforChange;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }
}

