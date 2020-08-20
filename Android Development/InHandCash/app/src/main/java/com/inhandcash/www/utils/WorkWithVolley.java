package com.inhandcash.www.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.inhandcash.www.activities.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.android.volley.VolleyLog.TAG;


public class WorkWithVolley {

    Context context;
    SharedPreferences sharedPreferences;

    public WorkWithVolley(Context context) {
        this.context = context;
        sharedPreferences = Utils.getEncryptedSharedPreferences(context);
    }

    public void postData(String url, JSONObject object, final MyJsonCallback callback) {
        postData(url, object, callback, true);
    }

    public void postData(String url , JSONObject object, final MyJsonCallback callback, boolean toShowProgress) {
        handelRequest(url,object,callback,Request.Method.POST,toShowProgress);
    }


    public void getData(String url, final MyJsonCallback callback) {
        getData(url, callback, true);
    }

    public void getData(String url , final MyJsonCallback callback, boolean toShowProgress){
        handelRequest(url,new JSONObject(),callback, Request.Method.GET,toShowProgress);
    }

    private void handelRequest(String url, JSONObject object, final MyJsonCallback callback,
                                            int method,boolean toShowProgress){
        final int[] mStatusCode = {0};
        Dialog progress_spinner;
        progress_spinner = Utils.LoadingSpinner(context);
        if (toShowProgress)
            progress_spinner.show();
        final RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(method, url, object,
                response -> {
                    progress_spinner.dismiss();
                    callback.call(response,mStatusCode[0]);
                }, error -> {
            progress_spinner.dismiss();
            if(error instanceof NetworkError){
                String msg = "It seams that you are not connected to a active internet connection.";
                showNetworkError("No internet connection.",msg);
            }
            else if(error instanceof NoConnectionError){
                String msg = "Unable to establish connection, please check your internet connection and try again.";
                showNetworkError("Connection Error.",msg);
            }
            else if(error instanceof TimeoutError){
                String msg = "Could not connect to server, may be your internet connection is too slow, try again.";
                showNetworkError("Timeout !",msg);
            }
            else if(error instanceof AuthFailureError){
                handel401();
            }
            else if(error instanceof ServerError){
                JSONObject errors = null;
                Log.d(TAG, "handelRequest: "+error.networkResponse.statusCode);
                try {
                    String responseBody = new String(error.networkResponse.data, "utf-8");
                    JSONObject data = new JSONObject(responseBody);
                    errors = data.getJSONObject("errors");
                } catch (JSONException e) {
                } catch (UnsupportedEncodingException errorr) {
                } catch (NullPointerException e){
                } finally {
                    Log.d(TAG, "handelRequest: "+errors);
                    callback.call(errors,error.networkResponse.statusCode);
                }
            }
        }){
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                mStatusCode[0] = response.statusCode;
                return super.parseNetworkResponse(response);
            }
            @Override
            public Map<String, String> getHeaders() {
                String token = sharedPreferences.getString("token","");
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Token " + token);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    private void handel401(){
        sharedPreferences.edit().putString("token","").putBoolean("isFcmSet",false).apply();
        Toast.makeText(context, "You are logged out please login again.", Toast.LENGTH_SHORT).show();
        Intent newIntent = new Intent(context, LoginActivity.class);
        context.startActivity(newIntent);
        ((Activity)context).finish();
    }
    private void showNetworkError(String title, String msg){
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("GOT IT", (dialogInterface, i) -> {
                }).setBackground(new ColorDrawable(Color.WHITE))
                .show();
    }
}
