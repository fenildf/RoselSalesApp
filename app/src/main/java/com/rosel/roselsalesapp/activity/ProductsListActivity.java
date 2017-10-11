package com.rosel.roselsalesapp.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.rosel.roselsalesapp.db.DbContract;
import com.rosel.roselsalesapp.objects.Order;
import com.rosel.roselsalesapp.objects.Product;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.db.RoselDatabaseHelper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

public class ProductsListActivity extends ActionBarActivity implements SearchView.OnQueryTextListener {

    private String filterQuery;
    private SQLiteDatabase db;
    private Cursor cursor;
    private ListView goodsList;
    private String lastParentId;
    private Stack<String> lastParentList = new Stack<>();
    private Long contextId;
    private boolean chooseMode;
    private boolean pickingMode;
    private long clientId;
    private boolean searchMode;
    SearchView searchView;
    Menu optionsMenu;
    private Order order = new Order();

    public static final String PRODUCTS_ORDER_CHOICE_EXTRA = "com.rosel.PRODUCTS_ORDER_CHOICE_EXTRA";
    public static final String PRODUCTS_ORDER_PICKING_EXTRA = "com.rosel.PRODUCTS_ORDER_CHOICE_EXTRA";
    public static final String PRODUCT_CHOICE_EXTRA = "com.rosel.PRODUCT_CHOICE_EXTRA";
    public static final String PRODUCT_PRICE_CHOICE_EXTRA = "com.rosel.PRODUCT_PRICE_CHOICE_EXTRA";
    public static final String PRODUCT_STOCK_CHOICE_EXTRA = "com.rosel.PRODUCT_STOCK_CHOICE_EXTRA";

    public static final String LAST_PARENT_ID_KEY = "com.rosel.LAST_PARENT_ID_KEY";
    public static final String LAST_PARENT_LIST_KEY = "com.rosel.LAST_PARENT_LIST_KEY";
    public static final String CHOOSE_MODE_KEY = "com.rosel.CHOOSE_MODE_KEY";
    public static final String PICKING_MODE_KEY = "com.rosel.PICKING_MODE_KEY";
    public static final String SEARCH_MODE_KEY = "com.rosel.SEARCH_MODE_KEY";
    public static final String CLIENT_ID_KEY = "com.rosel.CLIENT_ID_KEY";
    public static final String ORDER_KEY = "com.rosel.ORDER_KEY";

    // additional classes

    private class ProductsListAdapter extends SimpleCursorAdapter {

        public ProductsListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);

            if (!cursor.getString(cursor.getColumnIndex(DbContract.Products.COLUMN_NAME_SHOW)).equals("1")) {
                view.setAlpha((float) 0.5);
            } else {
                view.setAlpha((float) 1);
            }
            ImageView iw = (ImageView) view.findViewById(R.id.product_list_item_image);
            if (cursor.getString(cursor.getColumnIndex(DbContract.Products.COLUMN_NAME_IS_GROUP)).equals("1")) {
                iw.setImageResource(R.drawable.ic_folder_open_black_48dp);
            } else {
                iw.setImageDrawable(null);
            }
        }
    }

    // background DB stuff

    private class GetProductsList extends AsyncTask<String, Void, Boolean> {

        Cursor groupCursor;
        boolean newCursor;

        @Override
        protected Boolean doInBackground(String... params) {
            String curGroupId = params[0];
            String queryShowText = "";
            String whereIdText;
            ArrayList<String> queryParams = new ArrayList<>();
            newCursor = db == null;
            if (newCursor) {
                try {
                    RoselDatabaseHelper databaseHelper = new RoselDatabaseHelper(ProductsListActivity.this);
                    db = databaseHelper.getReadableDatabase();
                } catch (Exception e) {
                    return false;
                }
            }
            if(filterQuery!=null&&filterQuery.length()>0){
                whereIdText = DbContract.Products.COLUMN_NAME_NAME + " LIKE " + "'%" + filterQuery + "%' AND " + DbContract.Products.COLUMN_NAME_IS_GROUP + " = 0";
            } else {
                whereIdText = DbContract.Products.COLUMN_NAME_GROUP_ID + " = ?";
                queryParams.add(curGroupId);
                lastParentId = curGroupId;
            }
            if (!showHiddenElements()) {
                queryShowText = " AND " + DbContract.Products.COLUMN_NAME_SHOW +" = 1";
            }
            groupCursor = db.query(DbContract.Products.TABLE_NAME, null, whereIdText + queryShowText,queryParams.toArray(new String[queryParams.size()]), null, null, DbContract.Products.COLUMN_NAME_PRODUCT_ID);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            filterQuery = "";
            if (result) {
                if (newCursor) {
                    //CursorAdapter adapter = new ProductsListAdapter(ProductsListActivity.this, android.R.layout.simple_list_item_1, groupCursor, new String[]{DbContract.Products.COLUMN_NAME_NAME}, new int[]{android.R.id.text1}, 0);
                    CursorAdapter adapter = new ProductsListAdapter(ProductsListActivity.this, R.layout.products_list_item_view, groupCursor, new String[]{DbContract.Products.COLUMN_NAME_NAME}, new int[]{R.id.product_list_item_text}, 0);
                    goodsList = (ListView) findViewById(R.id.goods_listview);
                    goodsList.setAdapter(adapter);
                } else {
                    CursorAdapter adapter = (CursorAdapter) goodsList.getAdapter();
                    adapter.changeCursor(groupCursor);
                }
                cursor = groupCursor;
            } else {
                Toast.makeText(ProductsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateShowProducts extends AsyncTask<Boolean, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                ContentValues contentValues = new ContentValues();
                if (params[0]) {
                    contentValues.put(DbContract.Products.COLUMN_NAME_SHOW, "0");
                } else {
                    contentValues.put(DbContract.Products.COLUMN_NAME_SHOW, "1");
                }
                SQLiteOpenHelper helper = new RoselDatabaseHelper(ProductsListActivity.this);
                SQLiteDatabase dbForUpdate = helper.getWritableDatabase();
                dbForUpdate.update(DbContract.Products.TABLE_NAME, contentValues, DbContract.Products._ID + " = ?", new String[]{contextId.toString()});
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Toast.makeText(ProductsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GetItemAndSetResultTask extends AsyncTask<String, Void, Boolean> {

        SQLiteDatabase db;
        Cursor cursor;

        @Override
        protected Boolean doInBackground(String... params) {
            String productId = params[0];
            String orderClientID = params[1];
            try {
                RoselDatabaseHelper helper = new RoselDatabaseHelper(ProductsListActivity.this);
                db = helper.getReadableDatabase();
                String productQuery = "SELECT " + DbContract.Products.TABLE_NAME + ".*, " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_PRICE + ", " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_QUANTITY +
                        " FROM " + DbContract.Products.TABLE_NAME +
                        " LEFT JOIN " + DbContract.Prices.TABLE_NAME +
                            " ON " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_PRODUCT_ID + " = " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                                " AND " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_CLIENT_ID + " = ?" +
                        " LEFT JOIN " + DbContract.Stock.TABLE_NAME +
                            " ON " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID + " = " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                        " WHERE " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID + " = ?";
                cursor = db.rawQuery(productQuery, new String[]{orderClientID, productId});
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                Product item;
                float price;
                long quantity;
                if (cursor.moveToFirst()) {
                    item = new Product();
                    item.loadFromCursor(cursor);
                    price = cursor.getFloat(cursor.getColumnIndex(DbContract.Prices.COLUMN_NAME_PRICE));
                    quantity = cursor.getLong(cursor.getColumnIndex(DbContract.Stock.COLUMN_NAME_QUANTITY));
                    Intent intent = new Intent();
                    intent.putExtra(PRODUCT_PRICE_CHOICE_EXTRA, price);
                    intent.putExtra(PRODUCT_CHOICE_EXTRA, item);
                    intent.putExtra(PRODUCT_STOCK_CHOICE_EXTRA, quantity);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                cursor.close();
                db.close();
            } else {
                Toast.makeText(ProductsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class NewOrderLineTask extends AsyncTask<String, Void, Boolean> {

        SQLiteDatabase db;
        Cursor cursor;

        @Override
        protected Boolean doInBackground(String... params) {
            String productId = params[0];
            String orderClientID = params[1];
            try {
                RoselDatabaseHelper helper = new RoselDatabaseHelper(ProductsListActivity.this);
                db = helper.getReadableDatabase();
                String productQuery = "SELECT " + DbContract.Products.TABLE_NAME + ".*, " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_PRICE + ", " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_QUANTITY +
                        " FROM " + DbContract.Products.TABLE_NAME +
                        " LEFT JOIN " + DbContract.Prices.TABLE_NAME +
                        " ON " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_PRODUCT_ID + " = " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                        " AND " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_CLIENT_ID + " = ?" +
                        " LEFT JOIN " + DbContract.Stock.TABLE_NAME +
                        " ON " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID + " = " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                        " WHERE " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID + " = ?";
                cursor = db.rawQuery(productQuery, new String[]{orderClientID, productId});
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                float price;
                long quantity;
                if (cursor.moveToFirst()) {
                    final Product curItem = new Product();
                    curItem.loadFromCursor(cursor);
                    price = cursor.getFloat(cursor.getColumnIndex(DbContract.Prices.COLUMN_NAME_PRICE));
                    quantity = cursor.getLong(cursor.getColumnIndex(DbContract.Stock.COLUMN_NAME_QUANTITY));

                    AlertDialog.Builder builder = new AlertDialog.Builder(ProductsListActivity.this);
                    builder.setMessage(R.string.add_order_line)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog d = (AlertDialog) dialog;
                                    TextView priceView = (TextView) d.findViewById(R.id.line_price);
                                    float p = Float.parseFloat(priceView.getText().toString());
                                    TextView quantityView = (TextView) d.findViewById(R.id.line_quantity);
                                    int q = Integer.parseInt(quantityView.getText().toString());

                                    Order.OrderLine newLine = order.addLine();
                                    newLine.setItem(curItem);
                                    newLine.setQuantity(q);
                                    newLine.setPrice(p);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .setCancelable(false);
                    View dialogView = getLayoutInflater().inflate(R.layout.order_line_dialog, null);
                    TextView priceView = (TextView) dialogView.findViewById(R.id.line_price);
                    priceView.setText(String.format(Locale.ENGLISH, "%.2f", price));
                    TextView stockTextView = (TextView) dialogView.findViewById(R.id.stock_text_view);
                    stockTextView.setText("(" + getString(R.string.in_stock_label) + ": " + String.format("%d",quantity) + ")");
                    builder.setView(dialogView);
                    AlertDialog acceptDialog = builder.create();
                    acceptDialog.show();
                }
                cursor.close();
                db.close();
            } else {
                Toast.makeText(ProductsListActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // activity handlers

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LAST_PARENT_ID_KEY, lastParentId);
        outState.putSerializable(LAST_PARENT_LIST_KEY, lastParentList);
        outState.putBoolean(CHOOSE_MODE_KEY, chooseMode);
        outState.putBoolean(PICKING_MODE_KEY, pickingMode);
        outState.putBoolean(SEARCH_MODE_KEY, searchMode);
        outState.putLong(CLIENT_ID_KEY, clientId);
        outState.putSerializable(ORDER_KEY, order);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_list);

        if (savedInstanceState != null) {
            lastParentList = (Stack<String>) savedInstanceState.getSerializable(LAST_PARENT_LIST_KEY);
            lastParentId = savedInstanceState.getString(LAST_PARENT_ID_KEY);
            chooseMode = savedInstanceState.getBoolean(CHOOSE_MODE_KEY);
            pickingMode = savedInstanceState.getBoolean(PICKING_MODE_KEY);
            searchMode = savedInstanceState.getBoolean(SEARCH_MODE_KEY);
            clientId = savedInstanceState.getLong(CLIENT_ID_KEY);
            order = (Order) savedInstanceState.get(ORDER_KEY);
        } else {
            lastParentId = "";
            chooseMode = getCallingActivity() != null;
            if(chooseMode){
                Intent callingIntent = getIntent();
                clientId = callingIntent.getLongExtra(OrderDetailsActivity.EXTRA_ORDER_CLIENT_ID,0);
                pickingMode = callingIntent.getBooleanExtra(PRODUCTS_ORDER_PICKING_EXTRA,false);
            }
        }
        new GetProductsList().execute(lastParentId);

        goodsList = (ListView) findViewById(R.id.goods_listview);
        registerForContextMenu(goodsList);
        goodsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CursorAdapter ca = (CursorAdapter) parent.getAdapter();
                Cursor localC = (Cursor) ca.getItem(position);
                if (localC.getInt(localC.getColumnIndex(DbContract.Products.COLUMN_NAME_IS_GROUP)) == 1) {
                    lastParentList.push(lastParentId);
                    new GetProductsList().execute(localC.getString(localC.getColumnIndex(DbContract.Products.COLUMN_NAME_PRODUCT_ID)));
                } else if(chooseMode) {
                    if(pickingMode){
                        new NewOrderLineTask().execute(Long.toString(id), Long.toString(clientId));
                    } else {
                        new GetItemAndSetResultTask().execute(Long.toString(id), Long.toString(clientId));
                    }
                }
                else{
                    Intent intent = new Intent(ProductsListActivity.this, ProductDetailActivity.class);
                    intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, localC.getString(localC.getColumnIndex(DbContract.Products.COLUMN_NAME_PRODUCT_ID)));
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            if(!searchMode){
                searchMode = true;
                lastParentList.push(lastParentId);
            }
            filterQuery = intent.getStringExtra(SearchManager.QUERY);
            new GetProductsList().execute("");
        }
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
        getMenuInflater().inflate(R.menu.products_list_context_menu, menu);

        contextId = ((AdapterContextMenuInfo) menuInfo).id;

        View childView = ((AdapterContextMenuInfo) menuInfo).targetView;

        MenuItem showhideItem = menu.findItem(R.id.show_hide_menu_item);
        if (childView.getAlpha() < 1) {
            showhideItem.setTitle(R.string.show);
        } else {
            showhideItem.setTitle(R.string.hide);
        }
    }

    public void onClickShowHideMenuItem(MenuItem item) {
        setGoodsHidden(item.getTitle().equals(getString(R.string.hide)));
    }

    public void onShowHiddenClick(View view) {
        new GetProductsList().execute(lastParentId);
    }

    private boolean showHiddenElements() {
        CheckBox sh = (CheckBox) findViewById(R.id.show_hidden);
        return sh.isChecked();
    }

    private void setGoodsHidden(boolean hide) {
        new UpdateShowProducts().execute(hide);
        new GetProductsList().execute(lastParentId);
    }

    // action bar

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!lastParentList.empty()) {
                    new GetProductsList().execute(lastParentList.pop());
                    return true;
                }
            case R.id.complete_picking_menu_item:
                Intent intent = new Intent();
                intent.putExtra(PRODUCTS_ORDER_CHOICE_EXTRA, order);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;
        getMenuInflater().inflate(R.menu.products_list_options_menu, menu);

        menu.findItem(R.id.complete_picking_menu_item).setVisible(pickingMode);

        SearchManager sm = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search_products_menu_item).getActionView();
        searchView.setSearchableInfo(sm.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.setIconified(true);
        searchView.clearFocus();
        optionsMenu.findItem(R.id.search_products_menu_item).collapseActionView();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {return false;}

}