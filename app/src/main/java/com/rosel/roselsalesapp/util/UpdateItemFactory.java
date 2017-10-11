package com.rosel.roselsalesapp.util;

import com.rosel.roselsalesapp.util.RoselUpdateItem;

public interface UpdateItemFactory {

    RoselUpdateItem fillFromJSONString(String jsonString);

}
