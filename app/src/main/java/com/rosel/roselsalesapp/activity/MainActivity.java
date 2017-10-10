package com.rosel.roselsalesapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.rosel.roselsalesapp.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView mainNavList = (ListView) findViewById(R.id.main_navigation_list);
        mainNavList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this, OrdersListActivity.class));
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, ClientsListActivity.class));
                        break;
                    case 3:
                        startActivity(new Intent(MainActivity.this, SyncActivity.class));
                        break;
                    case 4:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.create_order_action:
                Intent intent = new Intent(this, OrderDetailsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
