package com.geniuslead.attendance.utils;

import com.google.gson.Gson;

/**
 * Created by AJ on 9/25/15.
 */
public class MyException extends Exception{
    private String Message;

    public MyException(String message) {

    }

    public static MyException parse(String json) {
        return new Gson().fromJson(json, MyException.class);
    }

    @Override
    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }
}
