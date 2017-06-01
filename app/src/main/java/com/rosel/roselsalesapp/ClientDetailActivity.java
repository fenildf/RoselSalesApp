package com.rosel.roselsalesapp;

import com.rosel.roselsalesapp.DbContract.*;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class ClientDetailActivity extends ActionBarActivity {

    private long clientId;
    private Client client;
    private Map<String, Long> addresses;

    public static final String CLIENT_ID_KEY = "com.rosel.CLIENT_ID_KEY";

    private class GetClientFromDbTask extends AsyncTask<Long, Void, Boolean>{

        Cursor cursor;
        SQLiteDatabase db;

        @Override
        protected Boolean doInBackground(Long... params) {
            try {
                RoselDatabaseHelper helper = new RoselDatabaseHelper(ClientDetailActivity.this);
                db = helper.getReadableDatabase();
                cursor = db.query(Clients.TABLE_NAME, null, Clients._ID + " = ?", new String[]{params[0].toString()}, null, null, null);
                if(cursor.moveToFirst()){
                    client = new Client();
                    client.loadFromCursor(cursor);
                    addresses = client.getAddressMap(db);
                }
                cursor.close();
                db.close();
            } catch (Exception e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                TextView nameTextView = (TextView) findViewById(R.id.client_name_text_view);
                nameTextView.setText(client.getName());
                ListView addressesListView = (ListView) findViewById(R.id.addresses_list_view);
                String[] items = addresses.keySet().toArray(new String[addresses.keySet().size()]);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(ClientDetailActivity.this, android.R.layout.simple_list_item_1, items);
                addressesListView.setAdapter(adapter);
            } else {
                Toast.makeText(ClientDetailActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);

        if(savedInstanceState==null){
            Intent intent = getIntent();
            if (intent != null) {
                clientId = intent.getLongExtra(ClientsListActivity.EXTRA_CLIENT_ID, 0);
            }
        } else {
            clientId = savedInstanceState.getLong(CLIENT_ID_KEY);
        }
        new GetClientFromDbTask().execute(clientId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(CLIENT_ID_KEY, clientId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
