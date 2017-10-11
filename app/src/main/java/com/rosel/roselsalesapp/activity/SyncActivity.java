package com.rosel.roselsalesapp.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rosel.roselsalesapp.objects.Client;
import com.rosel.roselsalesapp.db.DbContract;
import com.rosel.roselsalesapp.util.DeviceUuidFactory;
import com.rosel.roselsalesapp.util.MobileUpdateItemFactory;
import com.rosel.roselsalesapp.objects.Order;
import com.rosel.roselsalesapp.objects.Product;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.db.RoselDatabaseHelper;
import com.rosel.roselsalesapp.util.RoselUpdateInfo;
import com.rosel.roselsalesapp.util.RoselUpdateItem;
import com.rosel.roselsalesapp.util.TransportProtocol;
import com.rosel.roselsalesapp.util.UpdateItemFactory;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class SyncActivity extends ActionBarActivity implements AdapterView.OnItemClickListener{

    private ProgressBar sendingProgressBar;
    private ProgressBar updateProgressBar;
    private ListView menuListView;
    private int progressMode;
    private ProgressDialog progressDialog;
    private TextView progressInfo;

    //TODO notification after sync
    //TODO cancel loading on back/home button (maybe on pause activity?)

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        parent.setClickable(false);
        switch(position){
            case 0:
                progressMode = 0;
                new GetUpdateTask().execute();
                break;
            case 1:
                progressMode = 0;
                new SendOrdersTask().execute();
                break;
//            case 2:
//                progressMode = 1;
//                new GetUpdateTask().execute();
//                break;
//            case 3:
//                progressMode = 1;
//                new SendOrdersTask().execute();
//                break;
        }
    }

    private class SendOrdersTask extends AsyncTask<Void, Integer, Integer>{

        private static final int RESULT_CODE_SUCCESS = 0;
        private static final int RESULT_CODE_SYNC_ERROR = 1;
        private static final int RESULT_CODE_NO_NETWORK = 2;
        private static final int RESULT_CODE_SERVER_REJECT_DEVICE = 4;

        Socket socket = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        BufferedReader reader;
        PrintWriter writer;

        @Override
        protected void onPreExecute() {
            menuListView.setVisibility(View.INVISIBLE);
            switch (progressMode){
                case 0:
                    sendingProgressBar.setVisibility(View.VISIBLE);
                    sendingProgressBar.setProgress(0);
                    progressInfo.setVisibility(View.VISIBLE);
                    progressInfo.setText("Sending orders...");
                    break;
                case 1:
                    progressDialog = new ProgressDialog(SyncActivity.this);
                    progressDialog.setTitle(getString(R.string.update_dialog_title));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                    break;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (progressMode){
                case 0:
                    sendingProgressBar.setProgress(values[0]);
                    break;
                case 1:
                    progressDialog.setProgress(values[0]);
                    break;
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
            if(networkInfo!=null && networkInfo.isConnected()) {
                try {
                    socket = new Socket();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SyncActivity.this);
                    socket.connect(new InetSocketAddress(prefs.getString(getString(R.string.pref_server_address_key),""), Integer.parseInt(prefs.getString(getString(R.string.pref_server_port_key),""))), Integer.parseInt(prefs.getString(getString(R.string.pref_server_timeout_key),"")));
                    writer = new PrintWriter(socket.getOutputStream());
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    RoselDatabaseHelper dbHelper = new RoselDatabaseHelper(SyncActivity.this);
                    db = dbHelper.getReadableDatabase();

                    writer.println(TransportProtocol.POST);
                    writer.println(new DeviceUuidFactory(SyncActivity.this).getDeviceUuid().toString());
                    writer.flush();

                    String serverResponseString;
                    serverResponseString = reader.readLine();
                    switch (serverResponseString){
                        case TransportProtocol.NOT_REG:
                            return RESULT_CODE_SERVER_REJECT_DEVICE;
                        case TransportProtocol.START_POST:
                            for(Order curOrder:getOrdersUpdates()){
                                writer.println(curOrder.toJSONObject().toJSONString());
                            }
                            writer.println(TransportProtocol.COMMIT);
                            writer.flush();
                            serverResponseString = reader.readLine();
                            if(serverResponseString == null || !serverResponseString.equals(TransportProtocol.COMMIT)){
                                return RESULT_CODE_SYNC_ERROR;
                            }
                            mPublishProgress(100);
                            db.execSQL("DELETE FROM " + DbContract.Updates.TABLE_NAME);
                            break;
                        default:
                            return RESULT_CODE_SYNC_ERROR;
                    }
                } catch (Exception e) {
                    Log.e(getString(R.string.log_tag_send_orders), e.getMessage());
                    return RESULT_CODE_SYNC_ERROR;
                } finally{
                    if(writer!=null){
                        writer.close();
                    }
                    if(cursor!=null){
                        cursor.close();
                    }
                    if(db!=null){
                        db.close();
                    }
                    if(socket!=null){
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                return RESULT_CODE_NO_NETWORK;
            }
            return RESULT_CODE_SUCCESS;
        }

        public ArrayList<Order> getOrdersUpdates() {
            ArrayList<Order> ordersList = new ArrayList<>();
            String queryTxt = "SELECT T1.action AS action, T2.* FROM UPDATES AS T1 INNER JOIN ORDERS AS T2 ON T1.item_id = T2._id AND T1.table_name = 'ORDERS'";
            Cursor cursor  = db.rawQuery(queryTxt, null);
            Cursor tempCursor;
            int sendSize = cursor.getCount();
            int i=0;
            while(cursor.moveToNext()){
                Order order = new Order();
                order.setOrderId(cursor.getLong(cursor.getColumnIndex(DbContract.Orders._ID)));
                order.setDate(Order.computeDateFromSQL(cursor.getString(cursor.getColumnIndex(DbContract.Orders.COLUMN_NAME_DATE))));
                order.setShippingDate(Order.computeDateFromSQL(cursor.getString(cursor.getColumnIndex(DbContract.Orders.COLUMN_NAME_SHIPPING_DATE))));
                order.setComment(cursor.getString(cursor.getColumnIndex(DbContract.Orders.COLUMN_NAME_COMMENT)));
                order.setAddressId(cursor.getLong(cursor.getColumnIndex(DbContract.Orders.COLUMN_NAME_ADDRESS_ID)));
                order.setSent(false);

                Client client = new Client();
                //client.setName(cursor.getString(cursor.getColumnIndex(DbContract.Clients.COLUMN_NAME_NAME)));
                client.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Orders.COLUMN_NAME_CLIENT_ID)));
                order.setClient(client);

                tempCursor = db.query(DbContract.Orderlines.TABLE_NAME, null, DbContract.Orderlines.COLUMN_NAME_ORDER_ID + " = ?", new String[]{Long.toString(order.getOrderId())}, null, null, null);
                while(tempCursor.moveToNext()){
                    Order.OrderLine orderLine = order.addLine();
                    Product item = new Product();
                    item.setId(tempCursor.getLong(tempCursor.getColumnIndex(DbContract.Orderlines.COLUMN_NAME_PRODUCT_ID)));
                    orderLine.setItem(item);
                    orderLine.setQuantity(tempCursor.getInt(tempCursor.getColumnIndex(DbContract.Orderlines.COLUMN_NAME_QUANTITY)));
                    orderLine.setPrice(tempCursor.getFloat(tempCursor.getColumnIndex(DbContract.Orderlines.COLUMN_NAME_PRICE)));
                }
                tempCursor.close();
                ordersList.add(order);
                i++;
                mPublishProgress(Math.round(i*75/sendSize));
            }
            return ordersList;
        }

        void mPublishProgress(int progressToPublish){
            publishProgress(progressToPublish);
        }

        protected void onPostExecute(Integer res) {
            switch (res){
                case RESULT_CODE_SUCCESS:
                    Toast.makeText(SyncActivity.this, R.string.orders_sent, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_SYNC_ERROR:
                    Toast.makeText(SyncActivity.this, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_NO_NETWORK:
                    Toast.makeText(SyncActivity.this, R.string.cant_use_network, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_SERVER_REJECT_DEVICE:
                    Toast.makeText(SyncActivity.this, R.string.device_not_reg_on_server, Toast.LENGTH_SHORT).show();
                    break;
            }

            menuListView.setVisibility(View.VISIBLE);
            switch (progressMode){
                case 0:
                    sendingProgressBar.setVisibility(View.INVISIBLE);
                    progressInfo.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    progressDialog.dismiss();
                    break;
            }
        }
    }

    private class GetUpdateTask extends AsyncTask<Void, Integer, Integer>{

        private static final int RESULT_CODE_SUCCESS = 0;
        private static final int RESULT_CODE_SYNC_ERROR = 1;
        private static final int RESULT_CODE_NO_NETWORK = 2;
        private static final int RESULT_CODE_DB_UPDATE_ERROR = 3;
        private static final int RESULT_CODE_SERVER_REJECT_DEVICE = 4;

        Socket socket = null;
        UpdateItemFactory updateItemFactory = new MobileUpdateItemFactory();
        SQLiteDatabase db=null;
        PrintWriter writer;
        BufferedReader reader;

        @Override
        protected void onPreExecute() {
            menuListView.setVisibility(View.INVISIBLE);
            switch (progressMode){
                case 0:
                    updateProgressBar.setVisibility(View.VISIBLE);
                    updateProgressBar.setProgress(0);
                    progressInfo.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    progressDialog = new ProgressDialog(SyncActivity.this);
                    progressDialog.setTitle(getString(R.string.update_dialog_title));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.show();
                    break;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (progressMode){
                case 0:
                    updateProgressBar.setProgress(values[0]);
                    if(values.length==2) {
                        String tableName = "";
                        switch (values[1]) {
                            case 0:
                                tableName = DbContract.Clients.TABLE_NAME;
                                break;
                            case 1:
                                tableName = DbContract.Products.TABLE_NAME;
                                break;
                            case 2:
                                tableName = DbContract.Addresses.TABLE_NAME;
                                break;
                            case 3:
                                tableName = DbContract.Stock.TABLE_NAME;
                                break;
                            case 4:
                                tableName = DbContract.Prices.TABLE_NAME;
                                break;
                        }
                        progressInfo.setText("Updating " + tableName);
                    }
                    break;
                case 1:
                    progressDialog.setProgress(values[0]);
                    break;
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conMgr.getActiveNetworkInfo();
            if(networkInfo!=null && networkInfo.isConnected()) {
                try {
                    socket = new Socket();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SyncActivity.this);
                    socket.connect(new InetSocketAddress(prefs.getString(getString(R.string.pref_server_address_key),null), Integer.parseInt(prefs.getString(getString(R.string.pref_server_port_key),null))), Integer.parseInt(prefs.getString(getString(R.string.pref_server_timeout_key), "3000")));

                    writer = new PrintWriter(socket.getOutputStream());
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(SyncActivity.this);
                    db = helper.getWritableDatabase();

                    writer.println(TransportProtocol.GET);
                    writer.println(new DeviceUuidFactory(SyncActivity.this).getDeviceUuid());
                    writer.flush();

                    String serverResponseString = reader.readLine();
                    switch (serverResponseString){
                        case TransportProtocol.NOT_REG:
                            return RESULT_CODE_SERVER_REJECT_DEVICE;
                        case TransportProtocol.START_UPDATE:
                            //starting update process
                            int updRes;

                            //updating CLIENTS
                            updRes = UpdateTable(DbContract.Clients.TABLE_NAME, 0);
                            if(updRes!=RESULT_CODE_SUCCESS){
                                return updRes;
                            }

                            //updating PRODUCTS
                            updRes = UpdateTable(DbContract.Products.TABLE_NAME, 1);
                            if(updRes!=RESULT_CODE_SUCCESS){
                                return updRes;
                            }

                            //updating ADDRESSES
                            updRes = UpdateTable(DbContract.Addresses.TABLE_NAME, 2);
                            if(updRes!=RESULT_CODE_SUCCESS){
                                return updRes;
                            }

                            //updating STOCK
                            updRes = UpdateTable(DbContract.Stock.TABLE_NAME, 3);
                            if(updRes!=RESULT_CODE_SUCCESS){
                                return updRes;
                            }

                            //updating PRICES
                            updRes = UpdateTable(DbContract.Prices.TABLE_NAME, 4);
                            if(updRes!=RESULT_CODE_SUCCESS){
                                return updRes;
                            }

                            break;

                        default:
                            return RESULT_CODE_SYNC_ERROR;
                    }

                } catch (Exception e) {
                    Log.e(getString(R.string.update_log), e.getMessage());
                    return RESULT_CODE_SYNC_ERROR;
                } finally{
                    if(writer!=null) {
                        writer.close();
                    }
                    if(reader!=null){
                        try {
                            reader.close();
                        } catch (IOException ignore) {
                        }
                    }
                    if(db!=null){
                        db.close();
                        db = null;
                    }
                    if(socket!=null){
                        try {
                            socket.close();
                            socket = null;
                        } catch (IOException ignore) {
                        }
                    }
                }

            } else {
                return RESULT_CODE_NO_NETWORK;
            }

            return RESULT_CODE_SUCCESS;
        }

        void mPublishProgress(Integer... values){
            publishProgress(values);
        }

        @Override
        protected void onPostExecute(Integer res) {
            switch (res){
                case RESULT_CODE_SUCCESS:
                    Toast.makeText(SyncActivity.this, R.string.updates_loaded, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_SYNC_ERROR:
                    Toast.makeText(SyncActivity.this, R.string.connection_error, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_NO_NETWORK:
                    Toast.makeText(SyncActivity.this, R.string.cant_use_network, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_DB_UPDATE_ERROR:
                    Toast.makeText(SyncActivity.this, R.string.update_db_error, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_SERVER_REJECT_DEVICE:
                    Toast.makeText(SyncActivity.this, R.string.device_not_reg_on_server, Toast.LENGTH_SHORT).show();
                    break;
            }

            menuListView.setVisibility(View.VISIBLE);

            switch (progressMode){
                case 0:
                    updateProgressBar.setVisibility(View.INVISIBLE);
                    progressInfo.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    progressDialog.dismiss();
                    break;
            }
        }

        private int UpdateTable(String tableName, int progressCode) throws IOException {
            String serverResponseString;

            mPublishProgress(0, progressCode);

            writer.println(getRequestUpdateInfo(tableName).toJSON());
            writer.flush();

            serverResponseString = reader.readLine(); //contains RoselUpdateInfo in JSON
            if(serverResponseString==null){
                return RESULT_CODE_SYNC_ERROR;
            }
            RoselUpdateInfo serverUpdateInfo;
            try {
                serverUpdateInfo = RoselUpdateInfo.fromJSONString(serverResponseString);
            } catch (ParseException e) {
                Log.e(getString(R.string.update_log),e.getMessage());
                return RESULT_CODE_SYNC_ERROR;
            }
            for(int i=0;i<serverUpdateInfo.getAmount();i++){
                serverResponseString = reader.readLine(); //contains RoselUpdateInfo in JSON
                if(serverResponseString==null){
                    return RESULT_CODE_SYNC_ERROR;
                }
                serverUpdateInfo.addUpdateItem(updateItemFactory.fillFromJSONString(serverResponseString));
            }

            int i = 0;
            try {
                db.beginTransaction();
                for (RoselUpdateItem updateItem : serverUpdateInfo.getUpdateItems()) {
                    db.insertWithOnConflict(serverUpdateInfo.getTable(), null, createContentValues(updateItem), SQLiteDatabase.CONFLICT_REPLACE);
                    mPublishProgress(Math.round(i++ * 100 / serverUpdateInfo.getAmount()));
                }
                ContentValues newTableVersionContentValues = new ContentValues();
                newTableVersionContentValues.put(DbContract.Versions.COLUMN_NAME_VERSION, serverUpdateInfo.getVersion());
                db.update(DbContract.Versions.TABLE_NAME, newTableVersionContentValues, DbContract.Versions.COLUMN_NAME_TABLE_NAME + " = ?", new String[]{serverUpdateInfo.getTable()});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(getString(R.string.socket_log), e.getMessage());
                return RESULT_CODE_DB_UPDATE_ERROR;
            } finally {
                if (db != null && db.inTransaction()) {
                    db.endTransaction();
                }
            }
            return RESULT_CODE_SUCCESS;
        }

        private RoselUpdateInfo getRequestUpdateInfo(String table){
            RoselUpdateInfo res = null;
            Cursor updcursor = db.query(DbContract.Versions.TABLE_NAME, new String[]{DbContract.Versions.COLUMN_NAME_TABLE_NAME, DbContract.Versions.COLUMN_NAME_VERSION}, DbContract.Versions.COLUMN_NAME_TABLE_NAME + " = ?", new String[]{table}, null, null, null);
            while(updcursor.moveToNext()){
                res = new RoselUpdateInfo(table, updcursor.getLong(updcursor.getColumnIndex(DbContract.Versions.COLUMN_NAME_VERSION)), 0);
            }
            updcursor.close();
            return res;
        }

        private ContentValues createContentValues(RoselUpdateItem roselUpdateItem){
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", roselUpdateItem.id);
            for(RoselUpdateItem.ItemValue iv: roselUpdateItem.item_values){
                if(!iv.value.equals("null")) {
                    switch (iv.type) {
                        case "INTEGER":
                            contentValues.put(iv.name, Long.valueOf(iv.value));
                            break;
                        case "TEXT":
                            contentValues.put(iv.name, iv.value);
                            break;
                        case "REAL":
                            contentValues.put(iv.name, Float.valueOf(iv.value));
                            break;
                        default:
                            contentValues.put(iv.name, iv.value);
                            break;
                    }
                }
            }
            return contentValues;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sync);
        menuListView = (ListView) findViewById(R.id.sync_menu_list_view);
        menuListView.setOnItemClickListener(this);
        updateProgressBar = (ProgressBar) findViewById(R.id.update_progress_bar);
        updateProgressBar.setVisibility(View.INVISIBLE);
        sendingProgressBar = (ProgressBar) findViewById(R.id.sending_progress_bar);
        sendingProgressBar.setVisibility(View.INVISIBLE);
        progressInfo = (TextView) findViewById(R.id.progress_info_text);
        progressInfo.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}