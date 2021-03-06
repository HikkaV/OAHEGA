package org.oahega.com.utils;

import android.graphics.Bitmap;
import java.util.ArrayList;

public class DataHelper {

    private static DataHelper dataHelper;

    private int avarageNum;
    private ArrayList<Bitmap> listOne = new ArrayList<>();

    private DataHelper() {

    }

    public static DataHelper getInstance() {
        if (dataHelper == null) {
            dataHelper = new DataHelper();
        }
        return dataHelper;
    }

    public ArrayList<Bitmap> getListOne() {
        return listOne;
    }

    public void setListOne(ArrayList<Bitmap> listOne) {
        this.listOne = listOne;
    }

    public int getAvarageNum() {
        return avarageNum;
    }

    public void setAvarageNum(int avarageNum) {
        this.avarageNum = avarageNum;
    }
}
