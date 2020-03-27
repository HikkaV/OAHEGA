package org.oahega.com.utils;

public class Settings {

    private static Settings settings;
    private int minClassificationPercentToShow;
    private int minDetectionPercentToShow;
    private int numOfAvarage;
    private boolean isfront = false;
    private long width = 0;
    private long height = 0;
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

    public long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public void setBeforChange(boolean beforChange) {
        isBeforChange = beforChange;
    }

    public boolean isBeforeChange() {

        return isBeforChange;
    }
}

