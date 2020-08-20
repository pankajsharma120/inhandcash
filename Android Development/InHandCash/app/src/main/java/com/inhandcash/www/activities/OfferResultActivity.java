package com.inhandcash.www.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.inhandcash.www.R;

import java.util.Timer;
import java.util.TimerTask;

public class OfferResultActivity extends AppCompatActivity {

    TextView messageTv;
    MaterialCardView card;
    ImageView imageView;
    TextView statusTv;
    LinearLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_result);

        Intent thisIntent = getIntent();
        String message = thisIntent.getStringExtra("message");
        String status = thisIntent.getStringExtra("status");

        card = findViewById(R.id.card);
        messageTv = findViewById(R.id.message);
        imageView = findViewById(R.id.image);
        statusTv = findViewById(R.id.status);
        mainLayout = findViewById(R.id.mainLayout);

        if(status.compareTo("202")!=0){
            mainLayout.setBackgroundColor(getColor(R.color.danger));
            card.setCardBackgroundColor(getColor(R.color.danger));
            imageView.setImageDrawable(getDrawable(R.drawable.ic_close_white_24dp));
            statusTv.setText("FAILED");
        }
        messageTv.setText(message);
        deadTimer();
    }
    private void deadTimer(){
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                t.cancel();
                finish();
            }
        },3000);
    }
}
