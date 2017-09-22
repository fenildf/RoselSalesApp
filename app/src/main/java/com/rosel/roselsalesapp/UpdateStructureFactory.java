package com.rosel.roselsalesapp;

public abstract class UpdateStructureFactory {

    UpdateItemFactory updateItemFactory;

    public UpdateStructureFactory(UpdateItemFactory updateItemFactory) {
        this.updateItemFactory = updateItemFactory;
    }

    public abstract RoselUpdateStructure fillFromJSONString(String JSONString);

}
