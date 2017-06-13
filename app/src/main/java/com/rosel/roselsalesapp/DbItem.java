package com.rosel.roselsalesapp;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DbItem {
    public static final int ACTION_NEW = 1;
    public static final int ACTION_UPDATE = 2;
    public static final int ACTION_DELETE = 3;

    long id;
    String table_name;
    int action;
    ArrayList<ItemValue> item_values = new ArrayList();

    class ItemValue{
        String name;
        String type;
        String value;
    }

    public void addItemValue(String name, String type, String value) {
        ItemValue iv = new ItemValue();
        iv.name = name;
        iv.type = type;
        iv.value = value;
        this.item_values.add(iv);
    }

    public JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("table_name", table_name);
        jsonObject.put("action", action);
        JSONArray values = new JSONArray();
        for(ItemValue iv:item_values){
            JSONObject jsonValue = new JSONObject();
            jsonValue.put("name", iv.name);
            jsonValue.put("type", iv.type);
            jsonValue.put("value", iv.value);
            values.add(jsonValue);
        }
        jsonObject.put("item_values", values);
        return jsonObject;
    }
}