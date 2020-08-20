package com.inhandcash.www.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.inhandcash.www.utils.HandelOffers;
import com.inhandcash.www.utils.Utils;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandelOffers.setContext(this);

        SharedPreferences sharedPreferences = Utils.getEncryptedSharedPreferences(this);
        String token = sharedPreferences.getString("token","").trim();
        Boolean is_profiled = sharedPreferences.getBoolean("is_profiled",false);
        Log.d("Splash Activity", "onCreate: "+token+is_profiled);
        if(token.length()>0 && !is_profiled)
            startActivity(new Intent(SplashActivity.this,UserProfileActivity.class));
        else if(token.length()==0)
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        else
            startActivity(new Intent(SplashActivity.this, MainActivity.class));

        Log.d("TAG", "onCreate: SPLASH ACTIVITY"+getIntent().getExtras());
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                String value = getIntent().getExtras().getString(key);
                if(key.compareTo("to_activity")==0 && value.compareTo("offerSplashActivity")==0){
                    HandelOffers.addToQueue(getIntent().getExtras().getString("room_url"));
                }
            }
        }

        finish();
    }
}
