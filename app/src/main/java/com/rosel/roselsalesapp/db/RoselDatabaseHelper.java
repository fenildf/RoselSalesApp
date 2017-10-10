package com.rosel.roselsalesapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.rosel.roselsalesapp.R;


public class RoselDatabaseHelper extends SQLiteOpenHelper {

    private Context curContext;
    private static final String DB_NAME = "Rosel";
    private static final int DB_VERSION = 1;

    public RoselDatabaseHelper(Context context) throws Exception{
        super(context, DB_NAME, null, DB_VERSION);
        curContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        initTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL(DbContract.Products.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Clients.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Prices.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Stock.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Addresses.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Orders.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Orderlines.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Updates.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Versions.SQL_CREATE_STATEMENT);
    }

    private void initTables(SQLiteDatabase db){
        //fill VERSIONS
        db.execSQL(DbContract.Versions.getInsertQuery(DbContract.Clients.TABLE_NAME, 0));
        db.execSQL(DbContract.Versions.getInsertQuery(DbContract.Products.TABLE_NAME, 0));
        db.execSQL(DbContract.Versions.getInsertQuery(DbContract.Addresses.TABLE_NAME, 0));
        db.execSQL(DbContract.Versions.getInsertQuery(DbContract.Prices.TABLE_NAME, 0));
        db.execSQL(DbContract.Versions.getInsertQuery(DbContract.Stock.TABLE_NAME, 0));
    }

    public void clearTables(){
        new ClearTableAsyncTask().execute(DbContract.Updates.TABLE_NAME,
                DbContract.Orderlines.TABLE_NAME,
                DbContract.Orders.TABLE_NAME,
                DbContract.Prices.TABLE_NAME,
                DbContract.Stock.TABLE_NAME,
                DbContract.Clients.TABLE_NAME,
                DbContract.Products.TABLE_NAME,
                DbContract.Addresses.TABLE_NAME,
                DbContract.Versions.TABLE_NAME);
    }

    private class ClearTableAsyncTask extends AsyncTask<String, Void, Boolean>{

        SQLiteDatabase db = null;

        @Override
        protected Boolean doInBackground(String... tables) {
            try{
                db = getWritableDatabase();
                for(int i=0;i<tables.length;i++) {
                    String deleteStmt = "DELETE FROM " + tables[i];
                    db.execSQL(deleteStmt);
                }
                db.close();
            } catch(Exception e){
                Log.e(e.getClass().getName(), e.getMessage());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result){
                Toast.makeText(curContext, R.string.toast_clear_all_data, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(curContext, curContext.getString(R.string.db_error), Toast.LENGTH_SHORT).show();
            }
        }
    }
}