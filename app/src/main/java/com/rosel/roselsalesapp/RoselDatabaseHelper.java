package com.rosel.roselsalesapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


public class RoselDatabaseHelper extends SQLiteOpenHelper {

    private Context curContext;
    private static final String DB_NAME = "Rosel";
    private static final int DB_VERSION = 5;

    RoselDatabaseHelper(Context context) throws Exception{
        super(context, DB_NAME, null, DB_VERSION);
        curContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion==1){
            if(newVersion==2){
                upgrade_v2(db);
            }
            if(newVersion==3){
                upgrade_v2(db);
                upgrade_v3(db);
            }
            if(newVersion==4){
                upgrade_v2(db);
                upgrade_v3(db);
                upgrade_v4(db);
            }
            if(newVersion==5){
                upgrade_v2(db);
                upgrade_v3(db);
                upgrade_v4(db);
                upgrade_v5(db);
            }
        }
        if(oldVersion==2) {
            if(newVersion==3){
                upgrade_v3(db);
            }
            if(newVersion==4){
                upgrade_v3(db);
                upgrade_v4(db);
            }
            if(newVersion==5){
                upgrade_v3(db);
                upgrade_v4(db);
                upgrade_v5(db);
            }
        }
        if(oldVersion==3) {
            if(newVersion==4){
                upgrade_v4(db);
            }
            if(newVersion==5){
                upgrade_v4(db);
                upgrade_v5(db);
            }
        }
    }

    private static void upgrade_v2(SQLiteDatabase db){
        String updateString = "ALTER TABLE " + DbContract.Orders.TABLE_NAME +
                " ADD " + DbContract.Orders.COLUMN_NAME_COMMENT + " TEXT";
        db.execSQL(updateString);
    }

    public static void upgrade_v3(SQLiteDatabase db) {
        db.execSQL(DbContract.Addresses.SQL_CREATE_STATEMENT);
        String updateString = "ALTER TABLE " + DbContract.Orders.TABLE_NAME +
                " ADD " + DbContract.Orders.COLUMN_NAME_ADDRESS_ID + " INTEGER";
        db.execSQL(updateString);
    }

    public static void upgrade_v4(SQLiteDatabase db) {
        String updateString = "ALTER TABLE " + DbContract.Orders.TABLE_NAME +
                " ADD " + DbContract.Orders.COLUMN_NAME_SHIPPING_DATE + " TEXT";
        db.execSQL(updateString);
    }

    public static void upgrade_v5(SQLiteDatabase db) {
        String updateString = "ALTER TABLE " + DbContract.Clients.TABLE_NAME +
                " ADD " + DbContract.Clients.COLUMN_NAME_MANAGER_ID + " INTEGER";
        db.execSQL(updateString);
    }

    private void createTables(SQLiteDatabase db){
        db.execSQL(DbContract.Products.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Clients.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Prices.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Stock.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Addresses.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Orders.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Orderlines.SQL_CREATE_STATEMENT);
        db.execSQL(DbContract.Updates.SQL_CREATE_STATEMENT);
    }

    public void clearTables(){
        new ClearTableAsyncTask().execute(DbContract.Updates.TABLE_NAME,
                DbContract.Orderlines.TABLE_NAME,
                DbContract.Orders.TABLE_NAME,
                DbContract.Prices.TABLE_NAME,
                DbContract.Stock.TABLE_NAME,
                DbContract.Clients.TABLE_NAME,
                DbContract.Products.TABLE_NAME,
                DbContract.Addresses.TABLE_NAME);
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