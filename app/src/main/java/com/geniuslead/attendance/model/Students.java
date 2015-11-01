package com.geniuslead.attendance.model;

import com.google.gson.Gson;

/**
 * Created by Dream on 01-Nov-15.
 */
public class Students {
    private String UniqueIdentifier;

    public String getUniqueIdentifier() {
        return UniqueIdentifier;
    }

    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.UniqueIdentifier = uniqueIdentifier;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
