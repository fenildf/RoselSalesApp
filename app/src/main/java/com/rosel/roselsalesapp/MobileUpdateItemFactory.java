package com.rosel.roselsalesapp;

import android.database.Cursor;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;

public class MobileUpdateItemFactory implements UpdateItemFactory {

    @Override
    public RoselUpdateItem fillFromJSONString(String jsonString) {
        RoselUpdateItem roselUpdateItem = new RoselUpdateItem();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            roselUpdateItem.id = jsonObject.getLong("id");
            roselUpdateItem.action = jsonObject.getInt("action");
            JSONArray jsonValues = jsonObject.getJSONArray("item_values");
            int len = jsonValues.length();
            for(int i=0;i<len;i++){
                JSONObject jsonValue = jsonValues.getJSONObject(i);
                String valueName = jsonValue.getString("name");
                String valueType = jsonValue.getString("type");
                String value = jsonValue.getString("value");
                roselUpdateItem.addItemValue(valueName, valueType, value);
            }
        } catch(Exception e){
            Log.e("JSON", e.getMessage());
            return null;
        }
        return roselUpdateItem;
    }

//    public RoselUpdateItem fillFromCursor(Cursor cursor, String tableName) throws SQLException {
//        RoselUpdateItem roselUpdateItem = new RoselUpdateItem();
//        roselUpdateItem.id = cursor.getLong(cursor.getColumnIndex("_id"));
//        roselUpdateItem.action = cursor.getInt(cursor.getColumnIndex("action"));
//
//        int columnsCount = cursor.getColumnCount();
//        for (int i = 2; i < columnsCount; i++) {
//            String curValue = cursor.getString(i);
//            if(curValue==null) {
//                curValue="null";
//            }
//            roselUpdateItem.addItemValue(cursor.getColumnName(i), transferType(cursor.getType(i)), String.format("%s",cursor.getString(i)));
//        }
//        return roselUpdateItem;
//    }

//    private static String transferType(int type){
//        String result="";
//        switch (type){
//            case Cursor.FIELD_TYPE_INTEGER:
//                result = "INTEGER";
//                break;
//            case Cursor.FIELD_TYPE_STRING:
//                result = "TEXT";
//                break;
//            case Cursor.FIELD_TYPE_FLOAT:
//                result = "REAL";
//                break;
//            default:
//                result = "TEXT";
//        }
//        return result;
//    }
}
