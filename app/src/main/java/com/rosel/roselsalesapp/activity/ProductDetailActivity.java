package com.rosel.roselsalesapp.activity;

import com.rosel.roselsalesapp.Db.DbContract.*;
import com.rosel.roselsalesapp.R;
import com.rosel.roselsalesapp.Db.RoselDatabaseHelper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


public class ProductDetailActivity extends ActionBarActivity {

    public static final String EXTRA_PRODUCT_ID = "com.rosel.EXTRA_PRODUCT_ID";

    private class GetGoodsFromDBTask extends AsyncTask<String, Void, Boolean>{

        SQLiteDatabase db;
        Cursor cursor;

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                RoselDatabaseHelper helper = new RoselDatabaseHelper(ProductDetailActivity.this);
                db = helper.getReadableDatabase();
                cursor = db.query(Products.TABLE_NAME, null, Products.COLUMN_NAME_PRODUCT_ID + " = ?", new String[]{params[0]}, null, null, null);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
                if(cursor.moveToFirst()){
                    TextView textView;
                    textView = (TextView) findViewById(R.id.goods_code_text_view);
                    textView.setText(cursor.getString(cursor.getColumnIndex(Products.COLUMN_NAME_CODE)));
                    textView = (TextView) findViewById(R.id.goods_name_text_view);
                    textView.setText(cursor.getString(cursor.getColumnIndex(Products.COLUMN_NAME_NAME)));
                    cursor.close();
                    db.close();
                }
            } else {
                Toast.makeText(ProductDetailActivity.this, getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Intent intent = getIntent();
        String curProductId = intent.getStringExtra(EXTRA_PRODUCT_ID);
        new GetGoodsFromDBTask().execute(curProductId);
    }

}
