package com.rosel.roselsalesapp.util;

import com.rosel.roselsalesapp.util.RoselUpdateItem;

public interface UpdateItemFactory {

    public RoselUpdateItem fillFromJSONString(String jsonString);

}
