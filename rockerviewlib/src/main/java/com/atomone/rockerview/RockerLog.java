package com.atomone.rockerview;

import android.util.Log;

/**
 *@author atonOne
 */
public class RockerLog {

    private static final String TAG = "RockerView";

    private static final boolean DEBUG = false;

    public static void i(String msg) {
        if (DEBUG) {
            Log.i(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (DEBUG) {
            Log.w(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (DEBUG) {
            Log.e(TAG, msg);
        }
    }
}
