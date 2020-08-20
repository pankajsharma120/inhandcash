package com.inhandcash.www.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.inhandcash.www.R;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.WorkWithVolley;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

public class ShowInfoActivity extends AppCompatActivity {

    private WorkWithVolley workWithVolley;
    private ImageView qrImg;
    private TextView vpa;
    private TextView name;
    private TextView phn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_info);

        qrImg = findViewById(R.id.qrImg);
        vpa =  findViewById(R.id.vpaTv);
        name = findViewById(R.id.nameTv);
        phn = findViewById(R.id.phoneTv);

        workWithVolley = new WorkWithVolley(this);
        workWithVolley.getData(Globals.WEB_URL+"users/getmyinfo/",((jsonObject, status) -> {
            if(status==202){
                try {
                    Picasso.get().load(Globals.WEB_URL_WOS+jsonObject.getString("qr_url")).into(qrImg);
                    vpa.setText(jsonObject.getString("vpa"));
                    name.setText(jsonObject.getString("name"));
                    phn.setText(jsonObject.getString("phonenumber"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                Toast.makeText(ShowInfoActivity.this, "Something bad happened, if continues please contact.",
                        Toast.LENGTH_SHORT).show();
            }
        }));

    }
}
