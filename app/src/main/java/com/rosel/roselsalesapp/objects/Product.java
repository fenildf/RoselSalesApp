package com.rosel.roselsalesapp.objects;

import android.database.Cursor;

import com.rosel.roselsalesapp.db.DbContract;

import java.io.Serializable;

/**
 * Created by nikiforovnikita on 18.11.2016.
 */

public class Product implements Serializable{

    private long id;
    private String productId;
    private String name;
    private String code;
    private boolean isGroup;
    private long groupId;
    private boolean show;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public void loadFromCursor(Cursor productCursor){
        setId(productCursor.getLong(productCursor.getColumnIndex(DbContract.Products._ID)));
        setName(productCursor.getString(productCursor.getColumnIndex(DbContract.Products.COLUMN_NAME_NAME)));
        setCode(productCursor.getString(productCursor.getColumnIndex(DbContract.Products.COLUMN_NAME_CODE)));
        setProductId(productCursor.getString(productCursor.getColumnIndex(DbContract.Products.COLUMN_NAME_PRODUCT_ID)));
        setGroupId(productCursor.getLong(productCursor.getColumnIndex(DbContract.Products.COLUMN_NAME_GROUP_ID)));
        setGroup(productCursor.getInt(productCursor.getColumnIndex(DbContract.Products.COLUMN_NAME_IS_GROUP))!=0);
        setShow(productCursor.getInt(productCursor.getColumnIndex(DbContract.Products.COLUMN_NAME_SHOW))!=0);
    }
}
