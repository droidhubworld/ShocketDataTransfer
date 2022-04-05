package com.droidhubworld.socketio;

import android.util.Log;

public class Logger {
    public static void e(String TAG, String message) {
        if (BuildConfig.DEBUG && message != null && TAG != null) {
            Log.e(TAG, message);
        }
    }

    public static void d(String TAG, String message) {
        if (BuildConfig.DEBUG && message != null && TAG != null) {
            Log.d(TAG, message);
        }
    }
}
