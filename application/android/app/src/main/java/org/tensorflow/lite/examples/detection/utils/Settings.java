package org.tensorflow.lite.examples.detection.utils;

public class Settings {

    private static Settings settings;
    private int minPersentToShow;

    private Settings(){
        this.minPersentToShow = 70;
    }

    public static Settings getInstance() {
        if(settings == null){
            settings = new Settings();
        }
        return settings;
    }

    public int getMinPersentToShow() {
        return minPersentToShow;
    }

    public void setMinPersentToShow(int minPersentToShow) {
        this.minPersentToShow = minPersentToShow;
    }
}

