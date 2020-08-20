package com.inhandcash.www.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.inhandcash.www.R;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class OfferIntermediateActivity extends AppCompatActivity {

    private Request request;
    private OkHttpClient client;
    private Button cancelButton;
    private SharedPreferences sharedPreferences;
    private TextView amountTv;
    private WebSocket ws;
    private TextView offerdTo;

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
                if(statuss.compareTo("303")!=0){
                    Log.d(TAG, "onMessage: intermedia"+statuss);
                    Intent newIntent = new Intent(OfferIntermediateActivity.this, OfferResultActivity.class);
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
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_intermediate);

        Intent thisIntent = getIntent();

        sharedPreferences = Utils.getEncryptedSharedPreferences(this);

        offerdTo = findViewById(R.id.offerd_to);
        cancelButton = findViewById(R.id.cancel_button);
        amountTv = findViewById(R.id.amount_tv);
        amountTv.setText(thisIntent.getStringExtra("amount"));
        offerdTo.setText(thisIntent.getStringExtra("name"));

        client = new OkHttpClient();
        String token = sharedPreferences.getString("token","");
        request = new Request.Builder().header("Authorization", token)
                .url(Globals.WS_URL_WOS+thisIntent.getStringExtra("room_url")).build();
        MyWsListener listener = new MyWsListener();
        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

        cancelButton.setOnClickListener((v)-> ws.send("400"));
    }
}
