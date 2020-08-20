package com.inhandcash.www.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.inhandcash.www.R;
import com.inhandcash.www.activities.MainActivity;
import com.inhandcash.www.activities.UserProfileActivity;
import com.inhandcash.www.services.MySMSBroadcastReceiver;
import com.inhandcash.www.utils.Globals;
import com.inhandcash.www.utils.OtpReceivedInterface;
import com.inhandcash.www.utils.Utils;
import com.inhandcash.www.utils.WorkWithVolley;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InputPinFrag.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InputPinFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InputPinFrag extends Fragment implements OtpReceivedInterface {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Button verifyButton;
    private TextInputLayout pinTextView;
    private TextInputLayout tvPhnNo;
    private OnFragmentInteractionListener mListener;

    public InputPinFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InputPinFrag.
     */
    // TODO: Rename and change types and number of parameters
    public static InputPinFrag newInstance(String param1, String param2) {
        InputPinFrag fragment = new InputPinFrag();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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

        View view = inflater.inflate(R.layout.fragment_input_pin, container, false);

        startSMSListener();

        BroadcastReceiver mSmsBroadcastReceiver = new MySMSBroadcastReceiver();

        ((MySMSBroadcastReceiver) mSmsBroadcastReceiver).setOnOtpListeners(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
        getContext().registerReceiver(mSmsBroadcastReceiver, intentFilter);


        verifyButton = (view).findViewById(R.id.verify_btn);
        pinTextView = (view).findViewById(R.id.pin_field);
        tvPhnNo = (view).findViewById(R.id.pin_phn_tv);

        verifyButton.setOnClickListener(v -> {
            String pin = pinTextView.getEditText().getText().toString();
            if(pin.length()==0){
                pinTextView.setError("Please enter pin !");
            }
            else if(pin.length()!=6){
                pinTextView.setError("Invalid pin, please enter 6 digit received pin.");
            }
            else{
                WorkWithVolley wk = new WorkWithVolley(getActivity());
                JSONObject postObj = new JSONObject();
                try {
                    postObj.put("phonenumber",tvPhnNo.getEditText().getText().toString());
                    postObj.put("pin",pin);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                wk.postData(Globals.WEB_URL+"users/signin/", postObj, (jsonObject, status) -> {
                    if(status==400){
                        try {
                            pinTextView.setError(jsonObject.getString("pin"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(status==201 || status==208){
                        String token = "";
                        boolean is_profiled = false;
                        boolean is_seller = false;
                        try {
                            token = jsonObject.getString("token");
                            is_profiled = jsonObject.getBoolean("is_profiled");
                            is_seller = jsonObject.getBoolean("is_seller");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        SharedPreferences sharedPreferences = Utils.getEncryptedSharedPreferences(getActivity());
                        sharedPreferences.edit().putString("token",token)
                                .putBoolean("is_profiled",is_profiled)
                                .putBoolean("is_seller",is_seller)
                                .apply();
//                        Toast.makeText(getActivity(),"TOKEN:"+sharedPreferences.getString("token","no"),Toast.LENGTH_LONG).show();
                        if(status==208 && is_profiled)
                            getActivity().startActivity(new Intent(getActivity(), MainActivity.class));
                        else
                            getActivity().startActivity(new Intent(getActivity(), UserProfileActivity.class));
                        getActivity().finish();
                    }
                });
            }
        });

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public void startSMSListener() {
        SmsRetrieverClient mClient = SmsRetriever.getClient(getActivity());
        com.google.android.gms.tasks.Task<Void> mTask = mClient.startSmsRetriever();
        mTask.addOnSuccessListener((OnSuccessListener<? super Void>) aVoid -> Toast.makeText(getActivity(), "SMS Retriever starts", Toast.LENGTH_LONG).show());
        mTask.addOnFailureListener(e -> Toast.makeText(getActivity(), "Error", Toast.LENGTH_LONG).show());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
    public void onOtpReceived(String message) {
        String otp = message.substring(4,10);
        pinTextView.getEditText().setText(otp);
        verifyButton.performClick();
    }

    @Override
    public void onOtpTimeout() {
        Toast.makeText(getActivity(), "Time out, please resend", Toast.LENGTH_LONG).show();
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
}
