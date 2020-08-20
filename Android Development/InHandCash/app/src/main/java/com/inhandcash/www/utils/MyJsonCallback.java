package com.inhandcash.www.utils;

import org.json.JSONObject;

public interface MyJsonCallback {
    void call(JSONObject jsonObject,int status);
}