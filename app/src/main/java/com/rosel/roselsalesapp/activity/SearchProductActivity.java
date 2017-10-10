package com.rosel.roselsalesapp.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.rosel.roselsalesapp.Db.DbContract;
import com.rosel.roselsalesapp.objects.Product;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.Db.RoselDatabaseHelper;

public class SearchProductActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    RoselDatabaseHelper helper = null;
    private SQLiteDatabase db = null;
    private Cursor cursor = null;

    private String filterQuery;
    private ListView productsList;
    private boolean resultMode;
    private long clientId;

    public static final String EXTRA_RESULT_MODE = "com.rosel.EXTRA_RESULT_MODE";

    public static final String PRODUCT_CHOICE_EXTRA = "com.rosel.PRODUCT_CHOICE_EXTRA";
    public static final String PRODUCT_PRICE_CHOICE_EXTRA = "com.rosel.PRODUCT_PRICE_CHOICE_EXTRA";
    public static final String PRODUCT_STOCK_CHOICE_EXTRA = "com.rosel.PRODUCT_STOCK_CHOICE_EXTRA";

    public static final String RESULT_MODE_KEY = "com.rosel.RESULT_MODE_KEY";
    public static final String CLIENT_ID_KEY = "com.rosel.CLIENT_ID_KEY";

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CursorAdapter ca = (CursorAdapter) parent.getAdapter();
        Cursor localC = (Cursor) ca.getItem(position);
        if(resultMode) {
            Product item = new Product();
            item.loadFromCursor(localC);
            float price = localC.getFloat(cursor.getColumnIndex(DbContract.Prices.COLUMN_NAME_PRICE));
            long quantity = localC.getLong(cursor.getColumnIndex(DbContract.Stock.COLUMN_NAME_QUANTITY));
            Intent intent = new Intent(this, ProductsListActivity.class);
            intent.putExtra(PRODUCT_PRICE_CHOICE_EXTRA, price);
            intent.putExtra(PRODUCT_CHOICE_EXTRA, item);
            intent.putExtra(PRODUCT_STOCK_CHOICE_EXTRA, quantity);
            intent.putExtra(EXTRA_RESULT_MODE, resultMode);
            intent.putExtra(OrderDetailsActivity.EXTRA_ORDER_CLIENT_ID, clientId);
            startActivity(intent);
            finish();
        }
        else{
            Intent intent = new Intent(SearchProductActivity.this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, localC.getString(localC.getColumnIndex(DbContract.Products.COLUMN_NAME_PRODUCT_ID)));
            startActivity(intent);
        }
    }

    // background DB stuff

    private class SearchProductTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                if(helper==null) {
                    helper = new RoselDatabaseHelper(SearchProductActivity.this);
                }
                db = helper.getReadableDatabase();
                String productQuery;
                if (resultMode) {
                    productQuery = "SELECT " + DbContract.Products.TABLE_NAME + ".*, "
                            + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_PRICE + ", "
                            + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_QUANTITY
                            + " FROM " + DbContract.Products.TABLE_NAME +
                            " LEFT JOIN " + DbContract.Prices.TABLE_NAME +
                            " ON " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_PRODUCT_ID + " = " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                            " AND " + DbContract.Prices.TABLE_NAME + "." + DbContract.Prices.COLUMN_NAME_CLIENT_ID + " = ?" +
                            " LEFT JOIN " + DbContract.Stock.TABLE_NAME +
                            " ON " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID + " = " + DbContract.Products.TABLE_NAME + "." + DbContract.Products._ID +
                            " WHERE " + DbContract.Products.TABLE_NAME + "." + DbContract.Products.COLUMN_NAME_IS_GROUP + " = 0 AND "
                                + DbContract.Products.TABLE_NAME + "." + DbContract.Products.COLUMN_NAME_NAME + " LIKE " + "'%" + filterQuery + "%'";
                    cursor = db.rawQuery(productQuery, new String[]{Long.toString(clientId)});
                } else {
                    productQuery = "SELECT " + DbContract.Products.TABLE_NAME + ".* "
                            + " FROM " + DbContract.Products.TABLE_NAME +
                            " WHERE " + DbContract.Products.TABLE_NAME + "." + DbContract.Products.COLUMN_NAME_IS_GROUP + " = 0 AND "
                            + DbContract.Products.TABLE_NAME + "." + DbContract.Products.COLUMN_NAME_NAME + " LIKE " + "'%" + filterQuery + "%'";
                    cursor = db.rawQuery(productQuery, null);
                }

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                CursorAdapter adapter = new SimpleCursorAdapter(SearchProductActivity.this, android.R.layout.simple_list_item_1, cursor, new String[]{DbContract.Products.COLUMN_NAME_NAME}, new int[]{android.R.id.text1},0);
                productsList.setAdapter(adapter);
            } else {
                Toast.makeText(SearchProductActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // activity handlers

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RESULT_MODE_KEY, resultMode);
        outState.putLong(CLIENT_ID_KEY, clientId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_search);
        productsList = (ListView) findViewById(R.id.search_products_list_view);

        if (savedInstanceState != null) {
            resultMode = savedInstanceState.getBoolean(RESULT_MODE_KEY);
            clientId = savedInstanceState.getLong(CLIENT_ID_KEY);
        } else {
            Intent intent = getIntent();
            resultMode = intent.getBooleanExtra(EXTRA_RESULT_MODE, false);
            clientId = intent.getLongExtra(OrderDetailsActivity.EXTRA_ORDER_CLIENT_ID, 0);
            if(Intent.ACTION_SEARCH.equals(intent.getAction())){
                filterQuery = intent.getStringExtra(SearchManager.QUERY);
            }
        }
        productsList.setOnItemClickListener(this);
        new SearchProductTask().execute();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            filterQuery = intent.getStringExtra(SearchManager.QUERY);
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.products_list_options_menu, menu);
//        SearchManager sm = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView = (SearchView) menu.findItem(R.id.search_products_menu_item).getActionView();
//        searchView.setSearchableInfo(sm.getSearchableInfo(getComponentName()));
//        return true;
//    }
}