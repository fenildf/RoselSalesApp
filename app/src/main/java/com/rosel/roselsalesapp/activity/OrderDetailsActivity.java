package com.rosel.roselsalesapp.activity;

import com.rosel.roselsalesapp.objects.Client;
import com.rosel.roselsalesapp.db.DbContract.*;
import com.rosel.roselsalesapp.objects.Order;
import com.rosel.roselsalesapp.objects.Product;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.db.RoselDatabaseHelper;
import com.rosel.roselsalesapp.util.RoselUpdateItem;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends ActionBarActivity implements DatePickerDialog.OnDateSetListener{

    private int rowIndexContext;
    private Order order;
    private boolean isNew;
    private TextView commentText;
    private TextView addressText;
    private TextView shipDateTextView;
    public static final int CLIENT_CHANGE_REQUEST_CODE = 3;
    public static final int CLIENTS_REQUEST_CODE = 2;
    public static final int GOODS_REQUEST_CODE = 1;
    public static final int PRODUCTS_PICKING_REQUEST_CODE = 4;

    public static final String EXTRA_ORDER_ID = "com.rosel.EXTRA_ORDER_ID";
    public static final String EXTRA_ORDER_CLIENT_ID = "com.rosel.EXTRA_ORDER_CLIENT_ID";
    public static final String ORDER_KEY = "com.rosel.ORDER_KEY";
    public static final String IS_NEW_KEY = "com.rosel.IS_NEW_KEY";

    private class SaveOrderTask extends AsyncTask<Order, Void, Integer>{

        private static final int RESULT_CODE_ERROR = 0;
        private static final int RESULT_CODE_STOCK = 1;
        private static final int RESULT_CODE_SUCCESS = 21;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            order.setComment(commentText.getText().toString());
        }

        @Override
        protected Integer doInBackground(Order... params) {
            try{
                Order orderToSave = params[0];
                RoselDatabaseHelper helper = new RoselDatabaseHelper(OrderDetailsActivity.this);
                SQLiteDatabase db = helper.getWritableDatabase();
                ContentValues contentValues = new ContentValues();
                /*if(!orderToSave.checkStock(db)){
                    return RESULT_CODE_STOCK;
                }*/
                if(isNew) {
                    /*ContentValues contentValues = new ContentValues();
                    contentValues.put(Orders.COLUMN_NAME_DATE, Order.getDateForSQL(orderToSave.getDate()));
                    contentValues.put(Orders.COLUMN_NAME_SHIPPING_DATE, Order.getDateForSQL(orderToSave.getShippingDate()));
                    contentValues.put(Orders.COLUMN_NAME_SUM, orderToSave.getSum());
                    contentValues.put(Orders.COLUMN_NAME_COMMENT, orderToSave.getComment());
                    contentValues.put(Orders.COLUMN_NAME_CLIENT_ID, orderToSave.getClient().getId());
                    contentValues.put(Orders.COLUMN_NAME_ADDRESS_ID, orderToSave.getAddressId());*/

                    long orderId = db.insert(Orders.TABLE_NAME, null, orderToSave.getContentValues());

                    orderToSave.setOrderId(orderId);

                    //insert into updates
                    contentValues = new ContentValues();
                    contentValues.put(Updates.COLUMN_NAME_ITEM_ID, orderId);
                    contentValues.put(Updates.COLUMN_NAME_TABLE_NAME, Orders.TABLE_NAME);
                    contentValues.put(Updates.COLUMN_NAME_ACTION, RoselUpdateItem.ACTION_NEW);
                    contentValues.put(Updates.COLUMN_NAME_VERSION, 0);
                    db.insert(Updates.TABLE_NAME, null, contentValues);

                    long orderLineId;
                    for (int i = 0; i < orderToSave.getLinesCount(); i++) {
                        Order.OrderLine lineToSave = orderToSave.getLine(i);
                        /*contentValues.clear();
                        contentValues.put(Orderlines.COLUMN_NAME_PRODUCT_ID, lineToSave.getItem().getId());
                        contentValues.put(Orderlines.COLUMN_NAME_QUANTITY, Long.toString(lineToSave.getQuantity()));
                        contentValues.put(Orderlines.COLUMN_NAME_SUM, Float.toString(lineToSave.getSum()));
                        contentValues.put(Orderlines.COLUMN_NAME_PRICE, Float.toString(lineToSave.getPrice()));
                        contentValues.put(Orderlines.COLUMN_NAME_ORDER_ID, Long.toString(orderId));*/
                        orderLineId = db.insert(Orderlines.TABLE_NAME, null, lineToSave.getContentValues());

                        //insert into updates
                        contentValues = new ContentValues();
                        contentValues.put(Updates.COLUMN_NAME_ITEM_ID, orderLineId);
                        contentValues.put(Updates.COLUMN_NAME_TABLE_NAME, Orderlines.TABLE_NAME);
                        contentValues.put(Updates.COLUMN_NAME_ACTION, RoselUpdateItem.ACTION_NEW);
                        contentValues.put(Updates.COLUMN_NAME_VERSION, 0);
                        db.insert(Updates.TABLE_NAME, null, contentValues);
                    }
                    isNew = false;
                    order.setOrderId(orderId);
                } else {
                    /*ContentValues contentValues = new ContentValues();
                    contentValues.put(Orders.COLUMN_NAME_DATE, Order.getDateForSQL(orderToSave.getDate()));
                    contentValues.put(Orders.COLUMN_NAME_SHIPPING_DATE, Order.getDateForSQL(orderToSave.getShippingDate()));
                    contentValues.put(Orders.COLUMN_NAME_SUM, orderToSave.getSum());
                    contentValues.put(Orders.COLUMN_NAME_COMMENT, orderToSave.getComment());
                    contentValues.put(Orders.COLUMN_NAME_CLIENT_ID, orderToSave.getClient().getId());
                    contentValues.put(Orders.COLUMN_NAME_ADDRESS_ID, orderToSave.getAddressId());*/
                    long orderId = order.getOrderId();
                    db.update(Orders.TABLE_NAME, orderToSave.getContentValues(), Orders._ID + "=?", new String[]{Long.toString(orderId)});
                    db.delete(Orderlines.TABLE_NAME, Orderlines.COLUMN_NAME_ORDER_ID + "=?", new String[]{Long.toString(orderId)});
                    for (int i = 0; i < orderToSave.getLinesCount(); i++) {
                        Order.OrderLine lineToSave = orderToSave.getLine(i);
                        contentValues.clear();
                        /*contentValues.put(Orderlines.COLUMN_NAME_PRODUCT_ID, lineToSave.getItem().getId());
                        contentValues.put(Orderlines.COLUMN_NAME_QUANTITY, Long.toString(lineToSave.getQuantity()));
                        contentValues.put(Orderlines.COLUMN_NAME_SUM, Float.toString(lineToSave.getSum()));
                        contentValues.put(Orderlines.COLUMN_NAME_PRICE, Float.toString(lineToSave.getPrice()));
                        contentValues.put(Orderlines.COLUMN_NAME_ORDER_ID, Long.toString(orderId));*/
                        db.insert(Orderlines.TABLE_NAME, null, lineToSave.getContentValues());
                    }
                }
                db.close();
            } catch (Exception e){
                e.printStackTrace();
                return RESULT_CODE_ERROR;
            }
            return RESULT_CODE_SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch(result){
                case RESULT_CODE_SUCCESS:
                    Toast.makeText(OrderDetailsActivity.this, R.string.order_saved, Toast.LENGTH_SHORT).show();
                    updateOrderViews();
                    break;
                case RESULT_CODE_ERROR:
                    Toast.makeText(OrderDetailsActivity.this, R.string.order_save_error, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CODE_STOCK:
                    Toast.makeText(OrderDetailsActivity.this, R.string.not_enough_in_stock, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private class GetOrderFromDBTask extends AsyncTask<Long, Void, Boolean>{

        long orderId;

        @Override
        protected Boolean doInBackground(Long... params) {
            try{
                RoselDatabaseHelper helper = new RoselDatabaseHelper(OrderDetailsActivity.this);
                SQLiteDatabase db = helper.getReadableDatabase();

                orderId = params[0];
                String orderQuery = "SELECT " +
                        Orders.TABLE_NAME + "." + Orders._ID + ", " +
                        Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_DATE + ", " +
                        Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_SHIPPING_DATE + ", " +
                        Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_SUM + ", " +
                        Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_COMMENT + ", " +
                        Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_CLIENT_ID + ", " +
                        Clients.TABLE_NAME + "." + Clients.COLUMN_NAME_NAME + ", " +
                        Updates.TABLE_NAME + "." + Updates._ID + " ISNULL AS SENT, " +
                        Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_ADDRESS_ID + ", " +
                        Addresses.TABLE_NAME + "." + Addresses.COLUMN_NAME_ADDRESS +
                " FROM " + Orders.TABLE_NAME +
                    " LEFT JOIN " + Clients.TABLE_NAME + " ON (" + Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_CLIENT_ID + " = " + Clients.TABLE_NAME + "." + Clients._ID + ")" +
                    " LEFT JOIN " + Addresses.TABLE_NAME + " ON (" + Orders.TABLE_NAME + "." + Orders.COLUMN_NAME_ADDRESS_ID + " = " + Addresses.TABLE_NAME + "." + Addresses._ID + ")" +
                    " LEFT JOIN " + Updates.TABLE_NAME + " ON (" + Orders.TABLE_NAME + "." + Orders._ID + " = " + Updates.TABLE_NAME + "." + Updates.COLUMN_NAME_ITEM_ID + " AND " + Updates.TABLE_NAME + "." + Updates.COLUMN_NAME_TABLE_NAME + " = \"ORDERS\")" +
                " WHERE " + Orders.TABLE_NAME + "." + Orders._ID + " = ?";
                Cursor cursor  = db.rawQuery(orderQuery, new String[]{Long.toString(orderId)});
                if(cursor.moveToFirst()){
                    order = new Order();
                    order.setOrderId(orderId);
                    order.setDate(Order.computeDateFromSQL(cursor.getString(cursor.getColumnIndex(Orders.COLUMN_NAME_DATE))));
                    order.setShippingDate(Order.computeDateFromSQL(cursor.getString(cursor.getColumnIndex(Orders.COLUMN_NAME_SHIPPING_DATE))));
                    order.setSent(cursor.getLong(cursor.getColumnIndex("SENT"))==1);
                    order.setComment(cursor.getString(cursor.getColumnIndex(Orders.COLUMN_NAME_COMMENT)));
                    order.setAddressId(cursor.getLong(cursor.getColumnIndex(Orders.COLUMN_NAME_ADDRESS_ID)));
                    order.setAddress(cursor.getString(cursor.getColumnIndex(Addresses.COLUMN_NAME_ADDRESS)));

                    Client client = new Client();
                    client.setName(cursor.getString(cursor.getColumnIndex(Clients.COLUMN_NAME_NAME)));
                    client.setId(cursor.getLong(cursor.getColumnIndex(Orders.COLUMN_NAME_CLIENT_ID)));
                    order.setClient(client);

                    cursor = db.query(Orderlines.TABLE_NAME, null, Orderlines.COLUMN_NAME_ORDER_ID + " = ?", new String[]{Long.toString(orderId)}, null, null, null);
                    while(cursor.moveToNext()){
                        Order.OrderLine orderLine = order.addLine();
                        long productId = cursor.getLong(cursor.getColumnIndex(Orderlines.COLUMN_NAME_PRODUCT_ID));
                        Cursor productCursor = db.query(Products.TABLE_NAME, null, Products._ID + " = ?", new String[]{Long.toString(productId)}, null, null, null);
                        if(productCursor.moveToFirst()){
                            Product item = new Product();
                            item.loadFromCursor(productCursor);
                            orderLine.setItem(item);
                        }
                        orderLine.setQuantity(cursor.getInt(cursor.getColumnIndex(Orderlines.COLUMN_NAME_QUANTITY)));
                        orderLine.setPrice(cursor.getFloat(cursor.getColumnIndex(Orderlines.COLUMN_NAME_PRICE)));
                    }
                }

                cursor.close();
                db.close();

            } catch (Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean res) {
            if(res){
                updateOrderViews();
                updateVisibilityViews();
            } else {
                Toast.makeText(OrderDetailsActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteOrderTask extends AsyncTask<Long, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Long... params) {
            try{
                Long orderIdToDelete = params[0];
                RoselDatabaseHelper helper = new RoselDatabaseHelper(OrderDetailsActivity.this);
                SQLiteDatabase db = helper.getWritableDatabase();
                db.delete(Orderlines.TABLE_NAME, Orderlines.COLUMN_NAME_ORDER_ID + "=?", new String[]{Long.toString(orderIdToDelete)});
                db.delete(Orders.TABLE_NAME, Orders._ID + "=?", new String[]{Long.toString(orderIdToDelete)});
                db.close();
            } catch (Exception e){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                Toast.makeText(OrderDetailsActivity.this, R.string.order_deleted, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(OrderDetailsActivity.this, R.string.order_delete_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class EditOrderLineTask extends AsyncTask<Void, Void, Boolean>{

        int qty;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                RoselDatabaseHelper helper = new RoselDatabaseHelper(OrderDetailsActivity.this);
                SQLiteDatabase db = helper.getReadableDatabase();
                String getStockQuery = "SELECT " +
                Stock.TABLE_NAME + "." + Stock.COLUMN_NAME_QUANTITY +
                        " FROM " + Stock.TABLE_NAME +
                        " WHERE " + Stock.TABLE_NAME + "." + Stock.COLUMN_NAME_PRODUCT_ID + " = ?";
                Cursor cursor = db.rawQuery(getStockQuery, new String[]{Long.toString(order.getLine(rowIndexContext).getItem().getId())});
                if(cursor.moveToFirst()){
                    qty = cursor.getInt(cursor.getColumnIndex(Stock.COLUMN_NAME_QUANTITY));
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean res) {
            if(res){
                AlertDialog.Builder builder = new AlertDialog.Builder(OrderDetailsActivity.this);
                builder.setMessage(R.string.edit_this_row)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AlertDialog d = (AlertDialog) dialog;
                                try{
                                    TextView priceView = (TextView) d.findViewById(R.id.line_price);
                                    float p = Float.parseFloat(priceView.getText().toString());
                                    TextView quantityView = (TextView) d.findViewById(R.id.line_quantity);
                                    int q = Integer.parseInt(quantityView.getText().toString());
                                    editRow(rowIndexContext, q, p);
                                } catch (NumberFormatException nex){
                                    Toast.makeText(OrderDetailsActivity.this, "Wrong format!", Toast.LENGTH_SHORT) .show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setCancelable(false);
                View dialogView = getLayoutInflater().inflate(R.layout.order_line_dialog, null);

                TextView priceView = (TextView) dialogView.findViewById(R.id.line_price);
                String priceText = String.format(Locale.ENGLISH, "%.2f", order.getLine(rowIndexContext).getPrice());
                priceView.setText(priceText);

                TextView quantityView = (TextView) dialogView.findViewById(R.id.line_quantity);
                String quantityText = String.format(Locale.ENGLISH, "%d", order.getLine(rowIndexContext).getQuantity());
                quantityView.setText(quantityText);

                TextView stockTextView = (TextView) dialogView.findViewById(R.id.stock_text_view);
                stockTextView.setText("(" + getString(R.string.in_stock_label) + ": " + String.format("%d",qty) + ")");

                builder.setView(dialogView);

                AlertDialog acceptDialog = builder.create();
                acceptDialog.show();
            } else {
                Toast.makeText(OrderDetailsActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        commentText = (TextView) findViewById(R.id.comment_text_view);
        addressText = (TextView) findViewById(R.id.order_address_textview);
        addressText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(order.getClient()==null){
                        Toast.makeText(OrderDetailsActivity.this, R.string.select_client_first, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog.Builder dBuilder = new AlertDialog.Builder(OrderDetailsActivity.this);
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(OrderDetailsActivity.this);
                    final Map<String,Long> addressMap = order.getClient().getAddressMap(helper.getReadableDatabase());
                    if(addressMap.size()>0){
                        final String[] items = addressMap.keySet().toArray(new String[addressMap.size()]);
                        dBuilder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                order.setAddressId(addressMap.get(items[which]));
                                order.setAddress(items[which]);
                                updateAddressView();
                            }
                        })
                                .setTitle(R.string.Choose_address);
                        dBuilder.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        shipDateTextView = (TextView) findViewById(R.id.order_ship_date_view);
        shipDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(OrderDetailsActivity.this,OrderDetailsActivity.this,c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });

        if(savedInstanceState!=null){
            order = (Order) savedInstanceState.getSerializable(ORDER_KEY);
            isNew = savedInstanceState.getBoolean(IS_NEW_KEY);
            updateOrderViews();
            updateVisibilityViews();
        } else {
            Intent intent = getIntent();
            long curId = intent.getLongExtra(EXTRA_ORDER_ID, 0);
            isNew = curId==0;
            if(!isNew){
                new GetOrderFromDBTask().execute(curId);
            } else {
                order = new Order();
                Client curClient = (Client) intent.getSerializableExtra(ClientsListActivity.EXTRA_CLIENT);
                if(curClient!=null){
                    order.setClient(curClient);
                }
                updateOrderViews();
                updateVisibilityViews();
            }
        }

        /*int butVisibility;
        if(order!=null && !order.isSent()) {
            butVisibility = View.VISIBLE;
        } else {
            butVisibility = View.INVISIBLE;
        }
        setButtonsVisibility(butVisibility);

        if(order!=null && order.isSent()) {
            setReadOnly(commentText, true);
            setReadOnly(addressText, true);
        }*/

        updateVisibilityViews();
    }

    private void updateVisibilityViews(){
        int butVisibility;
        if(order!=null && !order.isSent()) {
            butVisibility = View.VISIBLE;
        } else {
            butVisibility = View.INVISIBLE;
        }
        setButtonsVisibility(butVisibility);

        if(order!=null && order.isSent()) {
            setReadOnly(commentText, true);
            setReadOnly(addressText, true);
            setReadOnly(shipDateTextView, true);
        }
    };

    private void updateOrderViews(){
        /*int butVisibility;
        if(order!=null && !order.isSent()) {
            butVisibility = View.VISIBLE;
        } else {
            butVisibility = View.INVISIBLE;
        }
        setButtonsVisibility(butVisibility);*/

        String title;
        if(!isNew) {
            title = getString(R.string.n) + " " + Long.toString(order.getOrderId()) + getString(R.string.orderfromdate) + Order.getDateString(order.getDate());
        } else {
            title = getString(R.string.new_order_title);
        }
        try{
            getActionBar().setTitle(title);
        } catch(Exception e){
            Log.e(getClass().getName(), e.getMessage());
        }

        commentText.setText(order.getComment());
        if(order.getShippingDate()!=null){
            shipDateTextView.setText(Order.getDateString(order.getShippingDate()));
        }

        /*addressText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AlertDialog.Builder dBuilder = new AlertDialog.Builder(OrderDetailsActivity.this);
                    RoselDatabaseHelper helper = new RoselDatabaseHelper(OrderDetailsActivity.this);
                    final Map<String,Long> addressMap = order.getClient().getAddressMap(helper.getReadableDatabase());
                    if(addressMap.size()>0){
                        final String[] items = addressMap.keySet().toArray(new String[addressMap.size()]);
                        dBuilder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                order.setAddressId(addressMap.get(items[which]));
                                order.setAddress(items[which]);
                                updateAddressView();
                            }
                        })
                                .setTitle(R.string.Choose_address);
                        dBuilder.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });*/

        /*if(order!=null && order.isSent()) {
            setReadOnly(commentText, true);
            setReadOnly(addressText, true);
        }*/

        loadOrderToTableLayout();
        updateClientView();
        updateAddressView();
        updateSumView();
        invalidateOptionsMenu();
    }

    public static void setReadOnly(final TextView view, final boolean readOnly) {
        view.setFocusable(!readOnly);
        view.setFocusableInTouchMode(!readOnly);
        view.setClickable(!readOnly);
        view.setLongClickable(!readOnly);
        view.setCursorVisible(!readOnly);
    }

    private void setButtonsVisibility(int visibility){
        Button clientButton = (Button) findViewById(R.id.choose_client_button);
        clientButton.setVisibility(visibility);
        Button addRowButton = (Button) findViewById(R.id.addRowButton);
        addRowButton.setVisibility(visibility);
    }

    private void setMeuItemsVisibility(Menu menu, boolean visibility){
        int itemsCount = menu.size();
        for(int i=0;i<itemsCount;i++){
            MenuItem curItem = menu.getItem(i);
            curItem.setVisible(visibility);
        }
//        MenuItem deleteOption = (MenuItem) menu.findItem(R.id.delete_order_detail_menu_item);
//        deleteOption.setVisible(visibility);
//        MenuItem saveOption = (MenuItem) menu.findItem(R.id.save_order_detail_menu_item);
//        saveOption.setVisible(visibility);
//        MenuItem clearOption = (MenuItem) menu.findItem(R.id.clear_order_detail_menu_item);
//        clearOption.setVisible(visibility);
    }

    public void loadOrderToTableLayout(){
        clearOrderTable();
        for(int i=0; i<order.getLinesCount(); i++){
            addLineView(order.getLine(i));
        }
    }

    public void clearOrderTable(){
        TableLayout orderTable = getOrderTableLayout();
        int orderTableRowsCount = orderTable.getChildCount();
        for(int i=1; i < orderTableRowsCount; i++){
            unregisterForContextMenu(orderTable.getChildAt(i));
        }
        orderTable.removeViews(1, orderTableRowsCount - 1);
    }

    public void addLineView(Order.OrderLine orderLine){
        TableLayout orderTable = getOrderTableLayout();
        TableRow newRowView = (TableRow) getLayoutInflater().inflate(R.layout.order_line, null);
        orderTable.addView(newRowView);
        registerForContextMenu(newRowView);
        updateOrderLineView(orderLine);
    }

    private void updateOrderLineView(Order.OrderLine orderLine) {

        TableLayout orderTable = getOrderTableLayout();
        TableRow newRow = (TableRow) orderTable.getChildAt(order.getLineIndex(orderLine)+1);

        TextView textView = (TextView) newRow.getChildAt(0);
        textView.setText(String.format(Locale.ENGLISH, "%d", order.getLineIndex(orderLine)+1));

        textView = (TextView) newRow.getChildAt(1);
        if(orderLine.getItem()!=null) {
            textView.setText(orderLine.getItem().getName());
        }

        textView = (TextView) newRow.getChildAt(2);
        textView.setText(String.format(Locale.ENGLISH, "%d", orderLine.getQuantity()));

        textView = (TextView) newRow.getChildAt(3);
        textView.setText(String.format(Locale.ENGLISH, "%.2f",orderLine.getPrice()));

        textView = (TextView) newRow.getChildAt(4);
        textView.setText(String.format(Locale.ENGLISH, "%.2f", orderLine.getSum()));
    }

    public TableLayout getOrderTableLayout(){
        return (TableLayout) findViewById(R.id.order_table);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.order_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setMeuItemsVisibility(menu, isNew || (order!=null && !order.isSent()));
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        if(order!=null && !order.isSent()) {
            getMenuInflater().inflate(R.menu.order_table_context_menu, menu);
            rowIndexContext = getOrderTableLayout().indexOfChild(v) - 1;
        }
    }

    public void onClickEditOrderLine(MenuItem item) {
        if(rowIndexContext!=-2){
            new EditOrderLineTask().execute();
        }
    }

    public void onClickDeleteOrderLine(MenuItem item) {
        if(rowIndexContext!=-2){
            AlertDialog.Builder builder = new AlertDialog.Builder(OrderDetailsActivity.this);
            builder.setMessage(R.string.delete_this_row)
                    .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //clearOrderTable();
                            order.removeLine(order.getLine(rowIndexContext));
                            updateOrderViews();
                        }
                    })
                    .setNegativeButton(R.string.No, null)
                    .setCancelable(false);
            builder.create().show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.clear_order_detail_menu_item:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.order_clear_accept)
                        .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                order = new Order();
                                updateOrderViews();
                            }
                        })
                        .setNegativeButton(R.string.No, null)
                        .setCancelable(false);
                AlertDialog acceptDialog = builder.create();
                acceptDialog.show();
                return true;
            case R.id.save_order_detail_menu_item:
                if(order.isCorrect()) {
                    new SaveOrderTask().execute(order);
                } else {
                    Toast.makeText(this, R.string.check_order_error, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.delete_order_detail_menu_item:
                if(isNew){
                    finish();
                } else {
                    new DeleteOrderTask().execute(order.getOrderId());
                }
                return true;
            case R.id.add_comment_order_detail_menu_item:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog d = (AlertDialog) dialog;
                        EditText comment_edit_text = (EditText) d.findViewById(R.id.comment_edit_text);
                        commentText.setText(comment_edit_text.getText().toString());
                    }
                });
                View dView = getLayoutInflater().inflate(R.layout.order_comment_dialog, null);
                ((EditText) dView.findViewById(R.id.comment_edit_text)).setText(order.getComment());
                builder1.setView(dView);
                builder1.create().show();
                return true;
            case R.id.pick_order_detail_menu_item:
                if(order.getClient()==null){
                    Toast.makeText(this, R.string.select_client_first, Toast.LENGTH_SHORT).show();
                } else {
                    //Intent intent = new Intent(this, ProductsPickingListActivity.class);
                    Intent intent = new Intent(this, ProductsListActivity.class);
                    intent.putExtra(EXTRA_ORDER_CLIENT_ID, order.getClient().getId());
                    intent.putExtra(ProductsListActivity.PRODUCTS_ORDER_PICKING_EXTRA, true);
                    startActivityForResult(intent, PRODUCTS_PICKING_REQUEST_CODE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ORDER_KEY, order);
        outState.putBoolean(IS_NEW_KEY, isNew);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case GOODS_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    final Product curItem = (Product) data.getSerializableExtra(ProductsListActivity.PRODUCT_CHOICE_EXTRA);
                    float price = data.getFloatExtra(ProductsListActivity.PRODUCT_PRICE_CHOICE_EXTRA, 0);
                    long quantity = data.getLongExtra(ProductsListActivity.PRODUCT_STOCK_CHOICE_EXTRA, 0);
                    AlertDialog.Builder builder = new AlertDialog.Builder(OrderDetailsActivity.this);
                    builder.setMessage(R.string.add_order_line)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog d = (AlertDialog) dialog;
                                    try {
                                        TextView priceView = (TextView) d.findViewById(R.id.line_price);
                                        float p = Float.parseFloat(priceView.getText().toString());
                                        TextView quantityView = (TextView) d.findViewById(R.id.line_quantity);
                                        int q = Integer.parseInt(quantityView.getText().toString());
                                        addNewRow(curItem, q, p);
                                    } catch (NumberFormatException nex){
                                        Toast.makeText(OrderDetailsActivity.this, "Wrong format!", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .setCancelable(false);
                    View dialogView = getLayoutInflater().inflate(R.layout.order_line_dialog, null);
                    TextView priceView = (TextView) dialogView.findViewById(R.id.line_price);
                    priceView.setText(String.format(Locale.ENGLISH, "%.2f", price));
                    TextView stockTextView = (TextView) dialogView.findViewById(R.id.stock_text_view);
                    stockTextView.setText("(" + getString(R.string.in_stock_label) + ": " + String.format("%d",quantity) + ")");
                    TextView stockView = (TextView) dialogView.findViewById(R.id.line_quantity);
                    stockView.setText("0");
                    builder.setView(dialogView);
                    AlertDialog acceptDialog = builder.create();
                    acceptDialog.show();
                }
                break;
            case PRODUCTS_PICKING_REQUEST_CODE:
                Order tempOrder = (Order) data.getSerializableExtra(ProductsListActivity.PRODUCTS_ORDER_CHOICE_EXTRA);
                Order.OrderLine tempLine;
                for(int i=0;i < tempOrder.getLinesCount(); i++){
                    tempLine = tempOrder.getLine(i);
                    order.addOrderLine(tempLine.getItem(), tempLine.getQuantity(), tempLine.getPrice());
                    updateOrderViews();
                }
                break;
            case CLIENTS_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    order.setClient((Client) data.getSerializableExtra(ClientsListActivity.EXTRA_CLIENT));
                    updateClientView();
                }
                break;
            case CLIENT_CHANGE_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    long clientWasId = order.getClient().getId();
                    order.setClient((Client) data.getSerializableExtra(ClientsListActivity.EXTRA_CLIENT));
                    if(clientWasId != order.getClient().getId()) {
                        order.clearLines();
                        order.setAddressId(0);
                        order.setAddress(null);
                        updateOrderViews();
                    }
                }
                break;
        }
    }

    private void updateClientView(){
        if(order.getClient()!=null) {
            TextView clientName = (TextView) findViewById(R.id.client_name_view);
            clientName.setText(order.getClient().getName());
        }
    }

    private void updateAddressView(){
        if(order.getAddress()!=null && !order.getAddress().isEmpty()){
            addressText.setText(order.getAddress());
        } else {
            addressText.setText(R.string.choose_address);
        }
    }

    private void chooseGoods(){
        Intent intent = new Intent(this, ProductsListActivity.class);
        intent.putExtra(EXTRA_ORDER_CLIENT_ID, order.getClient().getId());
        intent.putExtra(ProductsListActivity.PRODUCTS_ORDER_PICKING_EXTRA, false);
        startActivityForResult(intent, GOODS_REQUEST_CODE);
    }

    private void chooseClient(){
        Intent intent = new Intent(this, ClientsListActivity.class);
        startActivityForResult(intent, CLIENTS_REQUEST_CODE);
    }

    private void changeClient(){
        AlertDialog.Builder builder = new AlertDialog.Builder(OrderDetailsActivity.this);
        builder.setMessage(R.string.order_change_client_dialog_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(OrderDetailsActivity.this, ClientsListActivity.class);
                        startActivityForResult(intent, CLIENT_CHANGE_REQUEST_CODE);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false);
        AlertDialog changeClientDialog = builder.create();
        changeClientDialog.show();
    }

    private void addNewRow(Product item, int q, float p){
        // model part
        Order.OrderLine newLine = order.addLine();
        if(item!=null) {
            newLine.setItem(item);
            newLine.setQuantity(q);
            newLine.setPrice(p);
            // view part
            addLineView(newLine);
            updateSumView();
        }
    }

    private void editRow(int rowIndex, int q, float p){
        // model part
        Order.OrderLine curLine = order.getLine(rowIndex);
        curLine.setQuantity(q);
        curLine.setPrice(p);
        // view part
        updateOrderLineView(curLine);
        updateSumView();
    }

    private void updateSumView(){
        TextView sumTextView = (TextView) findViewById(R.id.order_sum_text);
        String sumText = String.format(Locale.ENGLISH, "%.2f", order.getSum());
        sumTextView.setText(sumText);
    }

    public void onClickAddRowButton(View view) {
        if(order.getClient()==null){
            Toast.makeText(this, R.string.select_client_first, Toast.LENGTH_SHORT).show();
            return;
        }
        chooseGoods();
    }

    public void onClickChooseClient(View view) {
        if(order.getClient()==null) {
            chooseClient();
        } else {
            changeClient();
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        GregorianCalendar shippingDate = new GregorianCalendar(year,month,dayOfMonth);
        order.setShippingDate(shippingDate);
        if(order.getShippingDate()!=null) {
            shipDateTextView.setText(Order.getDateString(order.getShippingDate()));
        }
    }
}
