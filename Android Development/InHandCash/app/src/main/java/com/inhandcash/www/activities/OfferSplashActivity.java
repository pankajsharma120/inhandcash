package com.inhandcash.www.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.inhandcash.www.R;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.HandelOffers;
import com.inhandcash.www.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class OfferSplashActivity extends AppCompatActivity {

    private int seconds = 0;
    private int minutes = 2;
    private TextView tv;
    private Button acceptBtn;
    private Button rejectBtn;
    private TextView amountTv;
    private TextView offered_from;

    private Request request;
    private OkHttpClient client;
    private SharedPreferences sharedPreferences;
    private WebSocket ws;
    private Dialog progress_spinner;

    private class MyWsListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        private static final String TAG = "WorkWithWebsocket";

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {

            try {
                JSONObject jsonObject = new JSONObject(text);
                String statuss = jsonObject.getString("status").trim();
                if(statuss.compareTo("303")==0){
                    JSONObject messageObj = jsonObject.getJSONObject("message");
                    minutes = messageObj.getInt("minutes");
                    seconds = messageObj.getInt("seconds");
                    runTimerOn(String.valueOf(messageObj.getDouble("amount")),
                                messageObj.getString("sender"));
                }
                else if(!statuss.isEmpty()){
                    Log.d(TAG, "onMessage: OFFERSPLASH"+statuss+(statuss=="303"));
                    Intent newIntent = new Intent(OfferSplashActivity.this, OfferResultActivity.class);
                    newIntent.putExtra("status",statuss);
                    newIntent.putExtra("message",jsonObject.getString("message"));
                    startActivity(newIntent);
                    webSocket.close(NORMAL_CLOSURE_STATUS,"bye!");
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.d(TAG, "onMessage: "+"Receiving bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.d(TAG, "onClosing: "+"Closing : " + code + " / " + reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d(TAG, "onFailure: "+"Error : " + t.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        ws.close(1000,"bye!");
        HandelOffers.onFinishActivity();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_splash);

        Log.d("TAG", "onCreate: OfferSplashActivity");

        Intent thisIntent = getIntent();

        progress_spinner = Utils.LoadingSpinner(OfferSplashActivity.this);
        progress_spinner.show();

        amountTv = findViewById(R.id.amount_tv);
        offered_from = findViewById(R.id.offerd_from);
        tv = findViewById(R.id.main_timer_text);
        acceptBtn = findViewById(R.id.accept);
        rejectBtn = findViewById(R.id.reject);

        sharedPreferences = Utils.getEncryptedSharedPreferences(this);

        client = new OkHttpClient();
        String token = sharedPreferences.getString("token","");
        request = new Request.Builder().header("Authorization", token)
                .url(Globals.WS_URL_WOS+thisIntent.getStringExtra("room_url")).build();
        OfferSplashActivity.MyWsListener listener = new OfferSplashActivity.MyWsListener();
        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

        acceptBtn.setOnClickListener((v)->ws.send("202"));
        rejectBtn.setOnClickListener((v)-> ws.send("400"));

    }
    private void runTimerOn(String amount, String sender){
        runOnUiThread(() -> {
            amountTv.setText(amount);
            offered_from.setText(sender);
            progress_spinner.dismiss();
        });
        Timer t = new Timer();
        //Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    tv.setText(minutes +":"+ seconds);
                    if(minutes<=-1){
                        ws.close(1000,"bye!");
                        t.cancel();
                        finish();
                    }
                    if(seconds == 0)
                    {
                        tv.setText(minutes +":"+ seconds);
                        seconds=60;
                        minutes=minutes-1;
                    }
                    seconds -= 1;
                });
            }
        }, 0, 1000);
    }
}
