package com.rosel.roselsalesapp.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.rosel.roselsalesapp.util.DeviceUuidFactory;
import com.rosel.roselsalesapp.R;

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
