package org.tensorflow.lite.examples.detection.utils;

public class Settings {

    private static Settings settings;
    private int minClassificationPercentToShow;
    private int minDetectionPercentToShow;
    private int numOfAvarage;
    private boolean isfront = true;

    private Settings(){
        this.minClassificationPercentToShow = 70;
        this.minDetectionPercentToShow = 70;
        this.numOfAvarage = 1;
        isfront = true;
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
}

