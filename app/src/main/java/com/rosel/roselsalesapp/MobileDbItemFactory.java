package com.rosel.roselsalesapp;

import android.database.Cursor;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

public class MobileDbItemFactory extends DbItemFactory {

    @Override
    public DbItem fillFromJSONString(String jsonString) {
        DbItem dbItem = new DbItem();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            dbItem.id = jsonObject.getLong("id");
            dbItem.table_name = jsonObject.getString("table_name");
            dbItem.action = jsonObject.getInt("action");
            JSONArray jsonValues = jsonObject.getJSONArray("item_values");
            int len = jsonValues.length();
            for(int i=0;i<len;i++){
                JSONObject jsonValue = jsonValues.getJSONObject(i);
                String valueName = jsonValue.getString("name");
                String valueType = jsonValue.getString("type");
                String value = jsonValue.getString("value");
                dbItem.addItemValue(valueName, valueType, value);
            }
        } catch(Exception e){
            Log.e("JSON", e.getMessage());
            return null;
        }
        return dbItem;
    }

    public DbItem fillFromCursor(Cursor cursor, String tableName) throws SQLException {
        DbItem dbItem = new DbItem();
        dbItem.id = cursor.getLong(cursor.getColumnIndex("_id"));
        dbItem.table_name = tableName;
        dbItem.action = cursor.getInt(cursor.getColumnIndex("action"));

        int columnsCount = cursor.getColumnCount();
        for (int i = 2; i < columnsCount; i++) {
            String curValue = cursor.getString(i);
            if(curValue==null) {
                curValue="null";
            }
            dbItem.addItemValue(cursor.getColumnName(i), transferType(cursor.getType(i)), String.format("%s",cursor.getString(i)));
        }
        return dbItem;
    }

    private static String transferType(int type){
        String result="";
        switch (type){
            case Cursor.FIELD_TYPE_INTEGER:
                result = "INTEGER";
                break;
            case Cursor.FIELD_TYPE_STRING:
                result = "TEXT";
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                result = "REAL";
                break;
            default:
                result = "TEXT";
        }
        return result;
    }
}
