package com.rosel.roselsalesapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.TextView;

public class InformationActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        String device_id = new DeviceUuidFactory(this).getDeviceUuid().toString();
        TextView deviceIdTextView = (TextView) findViewById(R.id.device_id_text_view);
        deviceIdTextView.setText(device_id);
    }
}
