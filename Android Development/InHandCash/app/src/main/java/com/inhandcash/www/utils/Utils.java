package com.inhandcash.www.utils;


import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.inhandcash.www.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class Utils {

    public static void showComingSoon(Context context){
        new MaterialAlertDialogBuilder(context)
                .setTitle("Coming Soon")
                .setMessage("This feature will be available soon.")
                .setPositiveButton("GOT IT", (dialogInterface, i) -> {
                }).setBackground(new ColorDrawable(Color.WHITE))
                .show();
    }

    public static SharedPreferences getEncryptedSharedPreferences(Context mContext){
        String masterKeyAlias = null;
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    "arth_data",
                    masterKeyAlias,
                    mContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sharedPreferences;
    }

    public static void sendFCMRegistrationToServer(String s, Context mContext) {
        WorkWithVolley workWithVolley = new WorkWithVolley(mContext);
        JSONObject sObject = new JSONObject();
        try {
            sObject.put("fcm",s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        workWithVolley.postData(Globals.WEB_URL+"users/set-fcm/",sObject,(jsonObject, status)->{
            if(status==202){
                SharedPreferences sharedPreferences = Utils.getEncryptedSharedPreferences(mContext);
                sharedPreferences.edit().putBoolean("isFcmSet",true).apply();
            }
        });
    }

    public static Dialog LoadingSpinner(Context mContext){
        Dialog pd = new Dialog(mContext, android.R.style.Theme_Black);
        View view = LayoutInflater.from(mContext).inflate(R.layout.aux_progress_spinner, null);
        pd.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pd.getWindow().setBackgroundDrawableResource(R.color.transparent_black);
        pd.setContentView(view);
        return pd;
    }

    public static boolean isInternetConnected(Context mContext){
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        Log.d("TAG", "isInternetConnected: "+isConnected);
        return isConnected;
    }

}



