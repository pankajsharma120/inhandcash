package com.inhandcash.www.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.inhandcash.www.R;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.WorkWithVolley;

import org.json.JSONException;
import org.json.JSONObject;

public class EnterAmountActivity extends AppCompatActivity {

    private FloatingActionButton floating_pay_btn;
    private TextView payingToTv;
    private String rvVPA;
    private String rvName;
    private WorkWithVolley workWithVolley;
    private TextView amountTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_amount);

        Intent thisIntent = getIntent();

        rvName = thisIntent.getStringExtra("name");
        rvVPA = thisIntent.getStringExtra("vpa");

        amountTv = findViewById(R.id.amount_tv);

        workWithVolley = new WorkWithVolley(this);

        floating_pay_btn = findViewById(R.id.floating_pay_button);
        floating_pay_btn.setOnClickListener((v)->{
            int amount = Integer.parseInt(amountTv.getText().toString());
            if(amountTv.getText().length()!=0 && amount>0 && amount<100000)
                handleOnPay();
            else
                Toast.makeText(EnterAmountActivity.this,
                        "Amount should be greater then 0 and less then 1 lakh.", Toast.LENGTH_SHORT).show();
        });

        payingToTv = findViewById(R.id.payingto);
        payingToTv.setText(rvName);
    }

    private void handleOnPay() {
        JSONObject sJson = new JSONObject();
        try {
            sJson.put("vpa",rvVPA);
            sJson.put("amount",amountTv.getText());
        } catch (JSONException e) {
            Toast.makeText(EnterAmountActivity.this, "Something went wrong please report, if continues.",
                            Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        workWithVolley.postData(Globals.WEB_URL+"transitions/offer/",sJson, (jsonObject, status)->{
            if(status==202){
                Intent newIntent = new Intent(EnterAmountActivity.this,OfferIntermediateActivity.class);
                try {
                    newIntent.putExtra("room_url",jsonObject.getString("room_url"));
                    newIntent.putExtra("name",rvName);
                    newIntent.putExtra("amount",String.valueOf(jsonObject.getDouble("amount")));
                    startActivity(newIntent);
                    finish();
                } catch (JSONException e) {
                    Toast.makeText(EnterAmountActivity.this, "Something went wrong please report, if continues.",
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            else{
                Toast.makeText(EnterAmountActivity.this, String.valueOf(jsonObject), Toast.LENGTH_SHORT).show();
            }
        },true);
    }
}
