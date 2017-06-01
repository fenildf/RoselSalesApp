package com.rosel.roselsalesapp;

import android.provider.BaseColumns;

public final class DbContract {

    public DbContract(){}

    public static String[] getTables(){
        return new String[]{Products.TABLE_NAME, Orders.TABLE_NAME, Orderlines.TABLE_NAME, Clients.TABLE_NAME, Prices.TABLE_NAME,
            Stock.TABLE_NAME, Updates.TABLE_NAME};
    }

    public static abstract class Products implements BaseColumns{
        public static final String TABLE_NAME = "PRODUCTS";
        public static final String COLUMN_NAME_PRODUCT_ID = "rosel_id";
        public static final String COLUMN_NAME_CODE = "code";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_IS_GROUP = "isgroup";
        public static final String COLUMN_NAME_GROUP_ID = "group_id";
        public static final String COLUMN_NAME_SHOW = "show";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_PRODUCT_ID + " TEXT UNIQUE, " +
                COLUMN_NAME_CODE + "  TEXT, " +
                COLUMN_NAME_NAME + " TEXT, " +
                COLUMN_NAME_IS_GROUP + " INTEGER, " +
                COLUMN_NAME_GROUP_ID + " TEXT, " +
                COLUMN_NAME_SHOW + " INTEGER)";
    }

    public static abstract class Clients implements BaseColumns{
        public static final String TABLE_NAME = "CLIENTS";
        public static final String COLUMN_NAME_CLIENT_ID = "rosel_id";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_MANAGER_ID = "manager_id";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_CLIENT_ID + " TEXT UNIQUE, " +
                COLUMN_NAME_ADDRESS + " TEXT, " +
                COLUMN_NAME_NAME + " TEXT," +
                COLUMN_NAME_MANAGER_ID + " INTEGER)";
    }

    public static abstract class Addresses implements BaseColumns{
        public static final String TABLE_NAME = "ADDRESSES";
        public static final String COLUMN_NAME_ADDRESS_ID = "rosel_id";
        public static final String COLUMN_NAME_CLIENT_ID = "client_id";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_ADDRESS_ID + " TEXT UNIQUE," +
                COLUMN_NAME_CLIENT_ID + " INTEGER REFERENCES " + Clients.TABLE_NAME + "(" + Clients._ID + "), "+
                COLUMN_NAME_ADDRESS + " TEXT)";
    }

    public static abstract class Orders implements BaseColumns{
        public static final String TABLE_NAME = "ORDERS";
        public static final String COLUMN_NAME_CLIENT_ID = "client_id";
        public static final String COLUMN_NAME_ADDRESS_ID = "address_id";
        public static final String COLUMN_NAME_DATE = "order_date";
        public static final String COLUMN_NAME_SHIPPING_DATE = "shipping_date";
        public static final String COLUMN_NAME_SUM = "sum";
        public static final String COLUMN_NAME_COMMENT = "comment";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_CLIENT_ID + " INTEGER REFERENCES " + Clients.TABLE_NAME + "(" + Clients._ID + "), " +
                COLUMN_NAME_ADDRESS_ID + " INTEGER REFERENCES " + Addresses.TABLE_NAME + "(" + Addresses._ID + "), " +
                COLUMN_NAME_DATE + " TEXT, " +
                COLUMN_NAME_SHIPPING_DATE + " TEXT, " +
                COLUMN_NAME_COMMENT + " TEXT, " +
                COLUMN_NAME_SUM + " REAL)";
    }

    public static abstract class Orderlines implements BaseColumns{
        public static final String TABLE_NAME = "ORDERLINES";
        public static final String COLUMN_NAME_ORDER_ID = "order_id";
        public static final String COLUMN_NAME_PRODUCT_ID = "product_id";
        public static final String COLUMN_NAME_QUANTITY = "quantity";
        public static final String COLUMN_NAME_PRICE = "price";
        public static final String COLUMN_NAME_SUM = "sum";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_ORDER_ID + " INTEGER REFERENCES " + Orders.TABLE_NAME+  "(" + Orders._ID + "), " +
                COLUMN_NAME_PRODUCT_ID + " INTEGER REFERENCES " + Products.TABLE_NAME + "(" + Products._ID + "), " +
                COLUMN_NAME_QUANTITY + " INTEGER, " +
                COLUMN_NAME_PRICE + " REAL, " +
                COLUMN_NAME_SUM + " REAL)";
    }

    public static abstract class Prices implements BaseColumns{
        public static final String TABLE_NAME = "PRICES";
        public static final String COLUMN_NAME_PRODUCT_ID = "product_id";
        public static final String COLUMN_NAME_CLIENT_ID = "client_id";
        public static final String COLUMN_NAME_PRICE = "price";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_PRODUCT_ID + " INTEGER REFERENCES " + Products.TABLE_NAME + "(" + Products._ID + "), " +
                COLUMN_NAME_CLIENT_ID + " INTEGER REFERENCES " + Clients.TABLE_NAME + "(" + Clients._ID + "), "+
                COLUMN_NAME_PRICE + " REAL, " +
                "UNIQUE (" + COLUMN_NAME_PRODUCT_ID + ", " + COLUMN_NAME_CLIENT_ID + "))";
    }

    public static abstract class Stock implements BaseColumns{
        public static final String TABLE_NAME = "STOCK";
        public static final String COLUMN_NAME_PRODUCT_ID = "product_id";
        public static final String COLUMN_NAME_QUANTITY = "quantity";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_PRODUCT_ID + " INTEGER UNIQUE REFERENCES " + Products.TABLE_NAME + "(" + Products._ID + "), " +
                COLUMN_NAME_QUANTITY + " REAL)";
    }

    public static abstract class Updates implements BaseColumns{
        public static final String TABLE_NAME = "UPDATES";
        public static final String COLUMN_NAME_ITEM_ID = "item_id";
        public static final String COLUMN_NAME_TABLE_NAME = "table_name";
        public static final String COLUMN_NAME_ACTION = "action";
        public static final String COLUMN_NAME_VERSION = "version";
        public static final String SQL_CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME +
                " (" + _ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_ITEM_ID + " INTEGER NOT NULL, " +
                COLUMN_NAME_TABLE_NAME + " TEXT, " +
                COLUMN_NAME_ACTION + " INTEGER, " +
                COLUMN_NAME_VERSION + " INTEGER, " +
                "UNIQUE (" + COLUMN_NAME_ITEM_ID + ", " + COLUMN_NAME_TABLE_NAME + ", " + COLUMN_NAME_VERSION + "));";
    }

}
