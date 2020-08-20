package com.inhandcash.www.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.inhandcash.www.R;
import com.inhandcash.www.adapters.TranisitionAdapter;
import com.inhandcash.www.models.Transition;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.PaginationScrollListener;
import com.inhandcash.www.utils.Utils;
import com.inhandcash.www.utils.WorkWithVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static com.inhandcash.www.utils.Utils.sendFCMRegistrationToServer;
import static com.inhandcash.www.utils.Utils.showComingSoon;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TranisitionAdapter tAdapter;
    private LinearLayoutManager layoutManager;
    private BottomAppBar bottomAppBar;
    private FloatingActionButton floatingActionButton;
    private WorkWithVolley workWithVolley;

    private static final int PAGE_START = 1;
    private boolean isLoading = false;
    private boolean isFirst = true;
    private boolean isLastPage = false;
    private int TOTAL_PAGES = 0;
    private int currentPage = PAGE_START;

    private Request request;
    private OkHttpClient client;
    private SharedPreferences sharedPreferences;
    private WebSocket ws;


    private class MyWsListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        private static final String TAG = "WorkWithWebsocket";

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "onOpen: ");
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(TAG, "onMessage: "+text);
            try {
                JSONObject jsonObject = new JSONObject(text);
                String statuss = jsonObject.getString("status").trim();
                if (statuss.compareTo("403")==0) {
                    JSONObject transition = jsonObject.getJSONObject("transition");
                    runOnUiThread(() -> {
                        try {
                            tAdapter.addAtPosition(new Transition(
                                    Double.parseDouble(transition.getString("amount")),
                                    transition.getString("sender_name"),
                                    transition.getString("receiver_name"),
                                    transition.getString("created_at"),
                                    transition.getString("status_name"),
                                    transition.getString("status")
                            ),0);
                        } catch (JSONException e) {
                            Log.d(TAG, "onMessage: "+e.getMessage());
                            e.printStackTrace();
                        }
                    });

                    Log.d(TAG, "onMessage: after adding");
                }
            }
            catch (JSONException e) {
                Log.d(TAG, "onMessage: "+e.getMessage());
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = Utils.getEncryptedSharedPreferences(this);

        workWithVolley = new WorkWithVolley(MainActivity.this);

        setFcm();

        setupWs();

        floatingActionButton = findViewById(R.id.bottom_floating_btn);
        floatingActionButton.setOnClickListener((v)->{
            String[] items = {"UPI ID", "OPEN CODE SCANNER"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Pay to").setItems(items,(dialog,which)->{
                switch (which){
                    case 0:
                        showUpiInput();
                        break;
                    case 1:
                        new IntentIntegrator(MainActivity.this).setCaptureActivity(ScannerActivity.class).initiateScan();
                        break;
                }
            }).setBackground(new ColorDrawable(Color.WHITE)).show();
        });

        bottomAppBar = findViewById(R.id.bottomAppBar);

        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()){
                case R.id.myInfo:{
                    startActivity(new Intent(MainActivity.this, ShowInfoActivity.class));
                    break;
                }
                case R.id.more:
                    showComingSoon(MainActivity.this);
            }
            return false;
        });

        bottomAppBar.setNavigationOnClickListener((v)->{
            showComingSoon(MainActivity.this);
        });

        Spinner dropdown = findViewById(R.id.planets_spinner);

        String[] items = new String[]{"All Transitions"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                        R.layout.custom_spinner, items);

        dropdown.setAdapter(adapter);

        dropdown.setSelection(0);

        recyclerView =  findViewById(R.id.ts_recycler_view);

        layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false);
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        tAdapter = new TranisitionAdapter(new ArrayList<>(), getApplicationContext());

        recyclerView.setAdapter(tAdapter);

        recyclerView.addOnScrollListener(new PaginationScrollListener(layoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;
                showTransitions();
            }
            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });

        showTransitions();

    }

    void setupWs(){
        client = new OkHttpClient();
        String token = sharedPreferences.getString("token","");
        request = new Request.Builder().header("Authorization", token)
                .url(Globals.WS_URL+"ws/update-room/").build();
        MainActivity.MyWsListener listener = new MainActivity.MyWsListener();
        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    private void showUpiInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ENTER UPI");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) ->
                handelAfterScan(input.getText().toString()));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void setFcm() {
        SharedPreferences sharedPreferences = Utils.getEncryptedSharedPreferences(MainActivity.this);
        Boolean isFcmSet = sharedPreferences.getBoolean("isFcmSet",false);
        if(!isFcmSet){
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w("TAGG", "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        Log.d("TAG", "setFcm: "+token);
                        sendFCMRegistrationToServer(token,MainActivity.this);
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case 49374:
            {
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (result != null) {
                    if (result.getContents() == null) {
                        Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_LONG).show();
                    } else {
                        String resultStr = result.getContents();
                        Toast.makeText(this, resultStr, Toast.LENGTH_LONG).show();
                        handelAfterScan(resultStr);
//                        startActivity(new Intent(MainActivity.this,EnterAmountActivity.class));
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    private void handelAfterScan(String resultStr) {
        workWithVolley.getData(Globals.WEB_URL + "users/get-upi-detail/"+resultStr.replace("inhandcash://","")+"/", (jsonObject, status) -> {
            if(status==202){
                Intent newIntent = new Intent(MainActivity.this,EnterAmountActivity.class);
                try {
                    newIntent.putExtra("vpa",jsonObject.getString("vpa"));
                    newIntent.putExtra("name",jsonObject.getString("name"));
                    startActivity(newIntent);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Something went wrong at our end, we are resolving it.",
                            Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(this, String.valueOf(jsonObject), Toast.LENGTH_SHORT).show();
            }
        },true);
    }

    private void showTransitions(){
        ArrayList<Transition> products = new ArrayList<>();
        workWithVolley.getData(Globals.WEB_URL+"transitions/listTrans/?page_size=10&page="+currentPage,
                ((jsonObject, status) -> {
            if(status==200){
                try {
                    JSONArray transitions = jsonObject.getJSONArray("results");
                    for(int i=0;i<transitions.length();i++){
                        JSONObject transition = transitions.getJSONObject(i);
                        products.add(new Transition(Double.parseDouble(transition.getString("amount")),
                                transition.getString("sender_name"),
                                transition.getString("receiver_name"),
                                transition.getString("created_at"),
                                transition.getString("status_name"),
                                transition.getString("status"))
                        );
                    }
                } catch (JSONException e) {
                    Log.d("TAG", "showTransitions: "+e.getMessage());
                    e.printStackTrace();
                }
                tAdapter.addAll(products);
//                tAdapter.removeLoadingFooter();
                isLoading = false;
                if(isFirst){
                    isFirst = false;
                    try {
                        TOTAL_PAGES = jsonObject.getInt("total_pages");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }),false);
    }

}
