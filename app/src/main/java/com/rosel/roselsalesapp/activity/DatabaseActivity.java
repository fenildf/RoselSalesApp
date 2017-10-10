package com.rosel.roselsalesapp.activity;

import android.os.Bundle;
import android.view.View;

import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.db.RoselDatabaseHelper;

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
