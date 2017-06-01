package com.rosel.roselsalesapp;

import android.app.Application;
import android.preference.PreferenceManager;

/**
 * Created by nikiforovnikita on 26.10.2016.
 */

public class RoselApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //RoselDatabaseHelper.firstInit(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
}
