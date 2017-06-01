package com.rosel.roselsalesapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;

public class DatabaseActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
    }

    public void onClickClearAll(View view) {
        try{
            RoselDatabaseHelper helper = new RoselDatabaseHelper(this);
            helper.clearTables();
            helper.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
