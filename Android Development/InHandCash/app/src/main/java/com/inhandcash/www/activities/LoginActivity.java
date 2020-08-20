package com.inhandcash.www.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.textfield.TextInputLayout;
import com.inhandcash.www.R;
import com.inhandcash.www.fragments.InputNoFrag;
import com.inhandcash.www.fragments.InputPinFrag;

public class LoginActivity extends AppCompatActivity implements InputNoFrag.OnFragmentInteractionListener,
        InputNoFrag.OnGetPhoneNumber,
        InputPinFrag.OnFragmentInteractionListener{
    FragmentManager fragmentManager = getSupportFragmentManager();
    TextInputLayout tvPhnNoPin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        tvPhnNoPin = findViewById(R.id.pin_phn_tv);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void setPhoneNoToPinFrag(String phnNo) {
        tvPhnNoPin.getEditText().setText(phnNo);
    }
}
