package com.inhandcash.www.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.inhandcash.www.activities.OfferSplashActivity;

import java.util.LinkedList;
import java.util.Queue;

public class HandelOffers {
    private static Queue<String> queue = new LinkedList<>();;
    private static Context mContext;

    public static void setContext(Context context){
        if(mContext==null){
            mContext = context;
        }
    }

    public static void addToQueue(String room_url){
        queue.add(room_url);
        if(queue.size()==1){
            runRequired();
        }
    }
    public static void runRequired(){
        if(queue.size()==0){
            return;
        }
        String room_url = queue.peek();
        Intent intent = new Intent(mContext, OfferSplashActivity.class);
        intent.putExtra("room_url", room_url);
        Log.d("TAG", "onMessageReceived: starting activity");
        mContext.startActivity(intent);
    }
    public static void onFinishActivity(){
        queue.remove();
        runRequired();
    }
}
