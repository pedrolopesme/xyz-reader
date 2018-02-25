package com.example.xyzreader.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DateUtil {

    private static final String TAG = DateUtil.class.toString();

    public static Date parse(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, "It as impossibile to parse date " + ex.getMessage());
        }
        return null;
    }

}
