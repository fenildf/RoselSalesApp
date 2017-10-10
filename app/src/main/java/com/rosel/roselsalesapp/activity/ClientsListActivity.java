package com.rosel.roselsalesapp.activity;

import com.rosel.roselsalesapp.objects.Client;
import com.rosel.roselsalesapp.db.DbContract;
import com.rosel.roselsalesapp.db.DbContract.*;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.db.RoselDatabaseHelper;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ClientsListActivity extends ActionBarActivity {

    private SQLiteDatabase db;
    private Cursor cursor;
    private String filterText;
    private boolean chooseMode;
    private long contextId;

    public static final String CHOOSE_MODE_KEY = "com.rosel.CHOOSE_MODE_KEY";
    public static final String EXTRA_CLIENT_ID = "com.rosel.EXTRA_CLIENT_ID";
    public static final String EXTRA_CLIENT = "com.rosel.EXTRA_CLIENT";

    private class GetClientsListTask extends AsyncTask<Void, Void, Boolean>{

        boolean isNewCursor = false;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String selectionText = null;
                if(filterText!=null&&filterText.length()>0){
                    selectionText = Clients.COLUMN_NAME_NAME + " LIKE '%"+ filterText + "%'";
                }
                isNewCursor = cursor == null;
                if(isNewCursor) {
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(ClientsListActivity.this);
                    db = helper.getReadableDatabase();
                }
                cursor = db.query(DbContract.Clients.TABLE_NAME, null, selectionText, null, null, null, null);
            } catch(Exception e){
                Log.e(DbContract.Clients.TABLE_NAME, e.getMessage());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                ListView clientsListView = (ListView) findViewById(R.id.clients_list_view);
                if(isNewCursor){
                    CursorAdapter adapter = new SimpleCursorAdapter(ClientsListActivity.this, android.R.layout.simple_list_item_1, cursor, new String[]{DbContract.Clients.COLUMN_NAME_NAME}, new int[]{android.R.id.text1},0);
                    clientsListView.setAdapter(adapter);
                } else {
                    ((CursorAdapter) clientsListView.getAdapter()).changeCursor(cursor);
                }
            } else {
                Toast.makeText(ClientsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GetClientAndCreateOrderTask extends AsyncTask<Long, Void, Boolean>{

        Cursor cursorForGet;

        @Override
        protected Boolean doInBackground(Long... params) {
            try {
                if(db==null) {
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(ClientsListActivity.this);
                    db = helper.getReadableDatabase();
                }
                cursorForGet = db.query(Clients.TABLE_NAME, null, Clients._ID + " = ?", new String[]{params[0].toString()}, null, null, null);
            } catch (Exception e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                if(cursorForGet.moveToFirst()){
                    Client contextClient = new Client();
                    contextClient.loadFromCursor(cursorForGet);
                    Intent intent = new Intent(ClientsListActivity.this, OrderDetailsActivity.class);
                    intent.putExtra(EXTRA_CLIENT, contextClient);
                    startActivity(intent);
                }
                cursorForGet.close();
            } else {
                Toast.makeText(ClientsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GetClientAndSetResultTask extends AsyncTask<Long, Void, Boolean>{

        Cursor cursorForGet;

        @Override
        protected Boolean doInBackground(Long... params) {
            try {
                if(db==null) {
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(ClientsListActivity.this);
                    db = helper.getReadableDatabase();
                }
                cursorForGet = db.query(Clients.TABLE_NAME, null, Clients._ID + " = ?", new String[]{params[0].toString()}, null, null, null);
            } catch (Exception e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                if(cursorForGet.moveToFirst()){
                    Client curClient = new Client();
                    curClient.loadFromCursor(cursorForGet);
                    Intent intent = new Intent(ClientsListActivity.this, OrderDetailsActivity.class);
                    intent.putExtra(EXTRA_CLIENT, curClient);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                cursorForGet.close();
            } else {
                Toast.makeText(ClientsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.clients_options_menu, menu);

        SearchManager sm = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView sv = (SearchView) menu.findItem(R.id.search_clients_menu_item).getActionView();
        sv.setSearchableInfo(sm.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if(filterText!=null){
                    filterText = null;
                    new GetClientsListTask().execute();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            filterText = intent.getStringExtra(SearchManager.QUERY);
            new GetClientsListTask().execute();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clients_list);

        if(savedInstanceState!=null){
            chooseMode = savedInstanceState.getBoolean(CHOOSE_MODE_KEY);
        } else {
            chooseMode = getCallingActivity()!=null;
        }

        ListView clientsListView = (ListView) findViewById(R.id.clients_list_view);
        registerForContextMenu(clientsListView);
        clientsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ClientsListActivity.this, ClientDetailActivity.class);
                intent.putExtra(EXTRA_CLIENT_ID, id);
                if(chooseMode){
                    new GetClientAndSetResultTask().execute(id);
                } else {
                    startActivity(intent);
                }
            }
        });
        new GetClientsListTask().execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(CHOOSE_MODE_KEY, chooseMode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
        if (db != null) {
            if (db.isOpen()) {
                db.close();
            }
        }
    }

    // context menu

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.clients_context_menu, menu);
        contextId = ((AdapterView.AdapterContextMenuInfo) menuInfo).id;
    }

    public void onCreateOrderFromContextMenu(MenuItem item) {
        new GetClientAndCreateOrderTask().execute(contextId);
    }

}
