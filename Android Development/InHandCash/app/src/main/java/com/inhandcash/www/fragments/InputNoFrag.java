package com.inhandcash.www.fragments;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.textfield.TextInputLayout;
import com.inhandcash.www.R;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.WorkWithVolley;

import org.json.JSONException;
import org.json.JSONObject;

import static android.app.Activity.RESULT_OK;


public class InputNoFrag extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private OnGetPhoneNumber thisActivity;
    private String mParam1;
    private String mParam2;
    Button contiButton;
    TextInputLayout phnInput;
    private GoogleApiClient googleApiClient;
    private static final int PHONE_NUMBER_RC = 15;
    private OnFragmentInteractionListener mListener;
    Bundle savedInstanceState;

    public InputNoFrag() {
        // Required empty public constructor
    }

    public static InputNoFrag newInstance(String param1, String param2) {
        InputNoFrag fragment = new InputNoFrag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnGetPhoneNumber{
        public void setPhoneNoToPinFrag(String phnNo);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.savedInstanceState = savedInstanceState;
        View view = inflater.inflate(R.layout.fragment_input_no, container, false);
        contiButton = (view).findViewById(R.id.continueButton);
        phnInput = (view).findViewById(R.id.phn_no_input);

        contiButton.setOnClickListener(v -> {
                    String no = phnInput.getEditText().getText().toString();
                    if(no.length()<10 || no.length()>13 ){
                        phnInput.setError("Please Enter Your 10 digit phone number.");
                    }
                    else{
                        handel_phonenumber(no);
                    }
                }
        );
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
        requestPhoneNumber();
        return view;
    }

    private void handel_phonenumber(String phn_no){
        WorkWithVolley wk = new WorkWithVolley(getContext());
        JSONObject jsObj = new JSONObject();
        try {
            jsObj.put("phonenumber",phn_no);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        wk.postData( Globals.WEB_URL+"users/signin/", jsObj, (jsonObject, status) -> {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            if(status==206){
                phnInput.setError(null);
                thisActivity.setPhoneNoToPinFrag(phn_no);
                fm.beginTransaction()
                        .hide(fm.findFragmentById(R.id.input_no_frag))
                        .show(fm.findFragmentById(R.id.input_pin_frag))
                        .addToBackStack(null)
                        .commit();
            }
            else if (status==400){
                try {
                    phnInput.setError(jsonObject.getString("phonenumber"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                Toast.makeText(getActivity(),"SERVER RUN INTO PROBLEM ! WE ARE FIXING.",Toast.LENGTH_LONG);
            }
        });
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        thisActivity = (OnGetPhoneNumber)context;
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHONE_NUMBER_RC) {
            if (resultCode == RESULT_OK) {
                Credential cred = data.getParcelableExtra(Credential.EXTRA_KEY);
                phnInput.getEditText().setText(cred.getId());
                contiButton.performClick();
            }
        }
    }

    public void requestPhoneNumber() {
        HintRequest hintRequest = new HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build();

        PendingIntent intent = Auth.CredentialsApi.getHintPickerIntent(googleApiClient, hintRequest);
        try {
            startIntentSenderForResult(intent.getIntentSender(), PHONE_NUMBER_RC, null, 0, 0, 0,savedInstanceState);
        } catch (IntentSender.SendIntentException e) {
            Log.e("TAG", "Could not start hint picker Intent", e);
        }
    }

}

