package com.inhandcash.www.services;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.inhandcash.www.utils.HandelOffers;
import com.inhandcash.www.utils.Utils;

import static com.inhandcash.www.utils.Utils.sendFCMRegistrationToServer;


public class FcmMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        SharedPreferences sharedPreferences = Utils.getEncryptedSharedPreferences(getApplicationContext());
        boolean isLoggedIn = !sharedPreferences.getString("token","").isEmpty();

        if(isLoggedIn){
            sendFCMRegistrationToServer(s, this);
        }
        super.onNewToken(s);
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        SharedPreferences sharedPreferences = Utils.getEncryptedSharedPreferences(getApplicationContext());
        boolean isLoggedIn = !sharedPreferences.getString("token","").isEmpty();
        if(isLoggedIn){
            String to_activity = remoteMessage.getData().get("to_activity");
            if(to_activity.compareTo("offerSplashActivity")==0)
            {
                String room_url = remoteMessage.getData().get("room_url");
                HandelOffers.addToQueue(room_url);
            }
        }
    }
}