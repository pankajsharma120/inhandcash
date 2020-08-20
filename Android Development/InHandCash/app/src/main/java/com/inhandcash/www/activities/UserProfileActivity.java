package com.inhandcash.www.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.inhandcash.www.R;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.Utils;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.Builders;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;

public class UserProfileActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private String token;
    private ImageView changeProfile;
    private ImageView profileImageIv;
    private Button submitBtn;
    private TextInputLayout fullNameTv;
    private Uri profileImageUri = null;
    Dialog progress_spinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        sharedPreferences = Utils.getEncryptedSharedPreferences(this);
        token = sharedPreferences.getString("token","");

        progress_spinner = Utils.LoadingSpinner(this);

        fullNameTv = findViewById(R.id.full_name_tv);
        profileImageIv = findViewById(R.id.profileImageIv);
        submitBtn = findViewById(R.id.submitBtn);

        changeProfile = findViewById(R.id.changeProfileBtn);

        changeProfile.setOnClickListener(v -> {
            CropImage.activity()
                    .setAspectRatio(1,1)
                    .setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        });

        submitBtn.setOnClickListener(v-> sendToServer());
    }

    private void sendToServer(){
        progress_spinner.show();
        Builders.Any.B builder = Ion.with(UserProfileActivity.this)
                .load("PUT", Globals.WEB_URL +"users/update/")
                .addHeader("Authorization", "Token " + token);

        if(profileImageUri!=null)
        {builder.setMultipartFile("profile_image",new File(profileImageUri.getPath()));}

        builder.setMultipartParameter("name", fullNameTv.getEditText().getText().toString())
                .asJsonObject().withResponse()
                .setCallback(((e, result) -> {
                    progress_spinner.dismiss();
                    int status = result.getHeaders().code();
                    if(status==202){
                        sharedPreferences.edit().putBoolean("is_profiled",true).apply();
                        startActivity(new Intent(UserProfileActivity.this,MainActivity.class));
                        finish();
                    }
                    else if(status==400){
                        JsonArray nameError = result.getResult().getAsJsonObject("errors").getAsJsonArray("name");
                        JsonArray profileError = result.getResult().getAsJsonObject("errors").getAsJsonArray("profile_image");
                        if(nameError!=null)
                            fullNameTv.setError(nameError.get(0).getAsString());
                        if(profileError!=null)
                            Toast.makeText(this,"Profile Image : "+profileError.get(0).getAsString(), Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(this, "Server get into trouble ! We are solving this issue, try again later.", Toast.LENGTH_LONG).show();
                    }
                }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                profileImageUri = result.getUri();
                Picasso.get().load(profileImageUri).into(profileImageIv);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.d("CROP ERROR", "onActivityResult: "+error);
            }
        }
    }

}

