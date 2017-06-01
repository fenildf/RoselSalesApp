package com.rosel.roselsalesapp;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DbChecker {

    SQLiteDatabase db;

    public boolean checkStock(Order order, SQLiteDatabase db){
        if(!order.isCorrect() || db==null){
            return false;
        }
        this.db = db;
        Map<Long, Long> orderMap = order.getOrdersQuantityMap();
        Map<Long, Long> stockMap = getStock(order);
        for (Map.Entry<Long,Long> entry:orderMap.entrySet()) {
            if(stockMap.containsKey(entry.getKey())){
                if(stockMap.get(entry.getKey()) < entry.getValue()){
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public Map<Long, Long> getStock(Order order){
        Map<Long, Long> result = new HashMap<Long, Long>();
        String stockQuery = "SELECT " +
                DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_QUANTITY + ", " +
                DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID +
        " FROM " + DbContract.Stock.TABLE_NAME +
        " WHERE " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID + " IN (" +
                        "SELECT " + DbContract.Orderlines.TABLE_NAME + "." + DbContract.Orderlines.COLUMN_NAME_PRODUCT_ID +
                        " FROM " + DbContract.Orderlines.TABLE_NAME +
                        " WHERE " + DbContract.Orderlines.TABLE_NAME + "." + DbContract.Orderlines.COLUMN_NAME_ORDER_ID + " = ?)";
        Cursor cursor = db.rawQuery(stockQuery, new String[]{String.format("%d",order.getOrderId())});
        while(cursor.moveToNext()){
           result.put(cursor.getLong(1), cursor.getLong(0));
        }
        return result;
    }

}
