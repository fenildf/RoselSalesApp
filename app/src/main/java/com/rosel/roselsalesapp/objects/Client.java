package com.rosel.roselsalesapp.objects;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rosel.roselsalesapp.Db.DbContract;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Client implements Serializable{

    private long id;
    private String name;
    private String client_id;
    private String address;

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_id() {
        return client_id;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void loadFromCursor(Cursor cursor){
        setId(cursor.getLong(cursor.getColumnIndex(DbContract.Clients._ID)));
        setName(cursor.getString(cursor.getColumnIndex(DbContract.Clients.COLUMN_NAME_NAME)));
        setClient_id(cursor.getString(cursor.getColumnIndex(DbContract.Clients.COLUMN_NAME_CLIENT_ID)));
        setAddress(cursor.getString(cursor.getColumnIndex(DbContract.Clients.COLUMN_NAME_ADDRESS)));
    }

    public Map<String, Long> getAddressMap(SQLiteDatabase db){
        Map<String, Long> res = new HashMap<String, Long>();
        String query = "SELECT " + DbContract.Addresses.COLUMN_NAME_ADDRESS + ", " + DbContract.Addresses._ID + " FROM " + DbContract.Addresses.TABLE_NAME + " WHERE " + DbContract.Addresses.COLUMN_NAME_CLIENT_ID + " = " + Long.toString(id);
        Cursor cursor = db.rawQuery(query,null);
        while(cursor.moveToNext()){
            res.put(cursor.getString(cursor.getColumnIndex(DbContract.Addresses.COLUMN_NAME_ADDRESS)), cursor.getLong(cursor.getColumnIndex(DbContract.Addresses._ID)));
        }
        return res;
    }
}
