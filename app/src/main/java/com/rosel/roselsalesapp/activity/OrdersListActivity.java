package com.rosel.roselsalesapp.activity;

import com.rosel.roselsalesapp.db.DbContract.*;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.db.RoselDatabaseHelper;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class OrdersListActivity extends ActionBarActivity {

    long contextId;
    SQLiteDatabase db;
    Cursor cursor;

    private class OrdersSimpleCursorAdapter extends SimpleCursorAdapter{

        public OrdersSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            long id = cursor.getLong(cursor.getColumnIndex(Orders._ID));
            String date = cursor.getString(cursor.getColumnIndex(Orders.COLUMN_NAME_DATE));
            String clientTitle = cursor.getString(cursor.getColumnIndex(Clients.COLUMN_NAME_NAME));
            textView.setText("#" + id + "  " + date + " " + clientTitle);
            if(cursor.getLong(cursor.getColumnIndex("SENT"))==1){
                textView.setAlpha((float) 0.5);
            } else {
                textView.setAlpha(1);
            }
        }
    }

    private class GetOrdersListTask extends AsyncTask<Void, Void, Boolean>{

        private boolean isNewCursor;

        @Override
        protected Boolean doInBackground(Void... params) {
            try{
                isNewCursor = cursor==null;
                if(db==null) {
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(OrdersListActivity.this);
                    db = helper.getReadableDatabase();
                }
                String orderQuery = "select O.*, C." + Clients.COLUMN_NAME_NAME + ", U." + Updates._ID + " ISNULL AS SENT " +
                        " from " + Orders.TABLE_NAME + " as O" +
                        " inner join " + Clients.TABLE_NAME + " as C on O." + Orders.COLUMN_NAME_CLIENT_ID + " = C." + Clients._ID +
                        " left join " + Updates.TABLE_NAME + " as U " + " ON (O." + Orders._ID + " = U." + Updates.COLUMN_NAME_ITEM_ID + " AND U." + Updates.COLUMN_NAME_TABLE_NAME + " = \"ORDERS\") ORDER BY O._id DESC";
                cursor = db.rawQuery(orderQuery, null);
            } catch (Exception e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean res) {
            if(res){
                ListView ordersList = (ListView) findViewById(R.id.orders_list_view);
                if(isNewCursor){
                    CursorAdapter adapter = new OrdersSimpleCursorAdapter(OrdersListActivity.this, android.R.layout.simple_list_item_1, cursor, new String[]{Orders._ID}, new int[]{android.R.id.text1}, 0);
                    ordersList.setAdapter(adapter);
                } else {
                    CursorAdapter adapter = (CursorAdapter) ordersList.getAdapter();
                    adapter.changeCursor(cursor);
                }
            } else {
                Toast.makeText(OrdersListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteOrderTask extends AsyncTask<Long, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Long... params) {
            try{
                Long orderIdToDelete = params[0];
                RoselDatabaseHelper helper = new RoselDatabaseHelper(OrdersListActivity.this);
                SQLiteDatabase db = helper.getWritableDatabase();
                db.delete(Orderlines.TABLE_NAME, Orderlines.COLUMN_NAME_ORDER_ID + "=?", new String[]{Long.toString(orderIdToDelete)});
                db.delete(Orders.TABLE_NAME, Orders._ID + "=?", new String[]{Long.toString(orderIdToDelete)});
                String orderQuery = "select O.*, C." + Clients.COLUMN_NAME_NAME + " from " + Orders.TABLE_NAME + " as O inner join " + Clients.TABLE_NAME + " as C on O." +Orders.COLUMN_NAME_CLIENT_ID + " = C." + Clients._ID;
                cursor = db.rawQuery(orderQuery, null);
            } catch (Exception e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                ListView ordersList = (ListView) findViewById(R.id.orders_list_view);
                OrdersSimpleCursorAdapter adapter = (OrdersSimpleCursorAdapter) ordersList.getAdapter();
                adapter.changeCursor(cursor);
                Toast.makeText(OrdersListActivity.this, R.string.order_deleted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OrdersListActivity.this, R.string.order_delete_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClickDeleteOrder(MenuItem item) {
        new DeleteOrderTask().execute(contextId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_list);
        final ListView ordersListView = (ListView) findViewById(R.id.orders_list_view);
        ordersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(OrdersListActivity.this, OrderDetailsActivity.class);
                intent.putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, id);
                startActivity(intent);
            }
        });
        registerForContextMenu(ordersListView);
        new GetOrdersListTask().execute();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.orders_list_context_menu, menu);
        contextId = ((AdapterView.AdapterContextMenuInfo) menuInfo).id;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.orders_list_options_menu, menu);
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

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        new GetOrdersListTask().execute();
//    }

    @Override
    protected void onResume() {
        new GetOrdersListTask().execute();
        super.onResume();
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
}
