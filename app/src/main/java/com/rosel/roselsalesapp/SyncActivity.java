package com.rosel.roselsalesapp;

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
import android.widget.Toast;
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
        //MobileDbItemFactory factory = new MobileDbItemFactory();
        ArrayList<Order> ordersToSend = new ArrayList<>();
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

                    mPublishProgress(5);

                    //expecting get device id request from server
                    String serverRequest = reader.readLine();
                    if(!serverRequest.equals(ExchangeProtocol.DEVICE_ID_REQUEST)){
                        return RESULT_CODE_SYNC_ERROR;
                    }
                    //send uid
                    writer.println(new DeviceUuidFactory(SyncActivity.this).getDeviceUuid().toString());
                    writer.flush();

                    mPublishProgress(20);

                    //expect confirmation for this device from server
                    serverRequest = reader.readLine();
                    switch(serverRequest){
                        case ExchangeProtocol.DEVICE_CONFIRMATION:
                            break;
                        case ExchangeProtocol.DEVICE_REJECTION:
                            return RESULT_CODE_SERVER_REJECT_DEVICE;
                        default:
                            return RESULT_CODE_SYNC_ERROR;
                    }

                    mPublishProgress(25);

                    //expect intention request
                    serverRequest = reader.readLine();
                    if(!serverRequest.equals(ExchangeProtocol.INTENTION_REQUEST)){
                        return RESULT_CODE_SYNC_ERROR;
                    }
                    mPublishProgress(30);

                    //send intention
                    writer.println(ExchangeProtocol.ClientIntention.SEND_ORDERS_STRING);
                    writer.flush();
                    //and then send orders
                    sendOrdersToServer();

                    mPublishProgress(80);

                    //confirm send success
                    String serverResponse = reader.readLine();
                    if(serverResponse==null || !serverResponse.equals(ExchangeProtocol.OK_RESPONSE)){
                        Log.e(getString(R.string.log_tag_send_orders), "Response missed");
                        return RESULT_CODE_SYNC_ERROR;
                    }
                } catch (Exception e) {
                    Log.e(getString(R.string.log_tag_send_orders), e.getMessage());
                    return RESULT_CODE_SYNC_ERROR;
                }
            } else {
                return RESULT_CODE_NO_NETWORK;
            }

            mPublishProgress(100);

            writer.close();
            db.execSQL("DELETE FROM " + DbContract.Updates.TABLE_NAME);

            return RESULT_CODE_SUCCESS;
        }

        private void sendOrdersToServer() throws Exception {
            RoselDatabaseHelper dbHelper = new RoselDatabaseHelper(SyncActivity.this);
            db = dbHelper.getReadableDatabase();
            ordersToSend.addAll(getOrdersUpdates());
            for(Order curOrder:ordersToSend){
                writer.println(curOrder.toJSONObject().toJSONString());
                writer.flush();
            }
            writer.println(ExchangeProtocol.CONFIRMATION_REQUEST);
            writer.flush();
        }

        public ArrayList<Order> getOrdersUpdates() {
            ArrayList<Order> ordersList = new ArrayList<>();
            String queryTxt = "SELECT T1.action AS action, T2.* FROM UPDATES AS T1 INNER JOIN ORDERS AS T2 ON T1.item_id = T2._id AND T1.table_name = 'ORDERS'";
            Cursor cursor  = db.rawQuery(queryTxt, null);
            Cursor tempCursor;
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
            }
            return ordersList;
        }

        void mPublishProgress(int progressToPublish){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            menuListView.setVisibility(View.VISIBLE);
            switch (progressMode){
                case 0:
                    sendingProgressBar.setVisibility(View.INVISIBLE);
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
        MobileDbItemFactory factory = new MobileDbItemFactory();
        SQLiteDatabase db=null;
        ArrayList<String> updatesList = new ArrayList<>();
        PrintWriter writer;
        BufferedReader reader;

        @Override
        protected void onPreExecute() {
            menuListView.setVisibility(View.INVISIBLE);
            switch (progressMode){
                case 0:
                    updateProgressBar.setVisibility(View.VISIBLE);
                    updateProgressBar.setProgress(0);
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
                    String serverAddressString = prefs.getString(getString(R.string.pref_server_address_key),null);
                    String serverPortString = prefs.getString(getString(R.string.pref_server_port_key),null);
                    if(serverAddressString!=null&&serverPortString!=null) {
                        socket.connect(new InetSocketAddress(serverAddressString, Integer.parseInt(serverPortString)), Integer.parseInt(prefs.getString(getString(R.string.pref_server_timeout_key), "3000")));
                    } else{
                        return RESULT_CODE_SYNC_ERROR;
                    }
                    writer = new PrintWriter(socket.getOutputStream());
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //expecting get device id request from server
                    String serverRequest = reader.readLine();
                    if(serverRequest==null || !serverRequest.equals(ExchangeProtocol.DEVICE_ID_REQUEST)){
                        return RESULT_CODE_SYNC_ERROR;
                    }

                    mPublishProgress(10);

                    //send uid
                    writer.println(new DeviceUuidFactory(SyncActivity.this).getDeviceUuid().toString());
                    writer.flush();

                    mPublishProgress(20);

                    //expect confirmation for this device from server
                    serverRequest = reader.readLine();
                    if(serverRequest!=null)
                    switch(serverRequest){
                        case ExchangeProtocol.DEVICE_CONFIRMATION:
                            break;
                        case ExchangeProtocol.DEVICE_REJECTION:
                            return RESULT_CODE_SERVER_REJECT_DEVICE;
                        default:
                            return RESULT_CODE_SYNC_ERROR;
                    }

                    mPublishProgress(25);

                    //expect intention request
                    serverRequest = reader.readLine();
                    if(serverRequest==null || !serverRequest.equals(ExchangeProtocol.INTENTION_REQUEST)){
                        return RESULT_CODE_SYNC_ERROR;
                    }

                    mPublishProgress(30);

                    //send intention
                    // (UPDATE) or (INIT)
                    try {
                        writer.println(getUpdateIntention());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return RESULT_CODE_SYNC_ERROR;
                    }
                    writer.flush();

                    //and get ready for incoming JSON strings
                    String line;
                    while((line = reader.readLine())!=null && !line.equals(ExchangeProtocol.CONFIRMATION_REQUEST)){
                         //in line we have JSON
                        updatesList.add(line);
                    }

                    if(line == null || !line.equals(ExchangeProtocol.CONFIRMATION_REQUEST)){
                        return RESULT_CODE_SYNC_ERROR;
                    }

                    mPublishProgress(50);

                    //write updates to mobile DB
                    try {
                        RoselDatabaseHelper helper = new RoselDatabaseHelper(SyncActivity.this);
                        db = helper.getWritableDatabase();
                        db.beginTransaction();
                        for(String updateString:updatesList){
                            handleUpdateString(updateString);
                        }
                        db.setTransactionSuccessful();
                    } catch (Exception e) {
                        Log.e(getString(R.string.socket_log), e.getMessage());
                        return RESULT_CODE_DB_UPDATE_ERROR;
                    } finally {
                        if(db!=null && db.inTransaction()){
                            db.endTransaction();
                        }
                    }

                    mPublishProgress(90);

                    //confirm update success
                    writer.println(ExchangeProtocol.OK_RESPONSE);
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    Log.e(getString(R.string.update_log), e.getMessage());
                    return RESULT_CODE_SYNC_ERROR;
                }
            } else {
                return RESULT_CODE_NO_NETWORK;
            }

            mPublishProgress(100);

            return RESULT_CODE_SUCCESS;
        }

        void mPublishProgress(int progressToPublish){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            publishProgress(progressToPublish);
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

            menuListView.setVisibility(View.VISIBLE);

            switch (progressMode){
                case 0:
                    updateProgressBar.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    progressDialog.dismiss();
                    break;
            }
        }

        void handleUpdateString(String updateString){
            DbItem dbItem = factory.fillFromJSONString(updateString);
            if(dbItem!=null){
                //TODO create action handle algorithm
                /*switch (dbItem.action){
                    case DbItem.ACTION_NEW:
                        //insert new row in DB
                        db.insert(dbItem.table_name, null,createContentValues(dbItem));
                        break;
                    case DbItem.ACTION_UPDATE:
                        //update row in DB
                        db.update(dbItem.table_name,,,,);
                        break;
                }*/
                db.insertWithOnConflict(dbItem.table_name, null,createContentValues(dbItem),SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        ContentValues createContentValues(DbItem dbItem){
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", dbItem.id);
            for(DbItem.ItemValue iv:dbItem.item_values){
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
                            contentValues.put(iv.name, iv.value.toString());
                            break;
                    }
                }
            }
            return contentValues;
        }

        String getUpdateIntention() throws Exception {
            RoselDatabaseHelper helper = new RoselDatabaseHelper(SyncActivity.this);
            db = helper.getWritableDatabase();
            String checkClearQuery = "SELECT " +
                    DbContract.Clients.TABLE_NAME + "." + DbContract.Clients._ID +
                    " FROM " + DbContract.Clients.TABLE_NAME +
                    " UNION " +
                    " SELECT " +
                    DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                    " FROM " + DbContract.Products.TABLE_NAME +
                    " LIMIT 1";
            Cursor cursor = db.rawQuery(checkClearQuery, null);
            if(cursor.moveToFirst()){
                return ExchangeProtocol.ClientIntention.GET_UPDATES_STRING;
            } else {
                return ExchangeProtocol.ClientIntention.INIT_STRING;
            }
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
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}