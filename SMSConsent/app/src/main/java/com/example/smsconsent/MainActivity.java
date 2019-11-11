package com.example.smsconsent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private TextView txtSMSData;
    private Button btnLoadSMSData, btnRequestHint;

    private static final int CREDENTIAL_PICKER_REQUEST = 1;  // Set to an unused request code
    private static final int RC_SAVE = 23;

   private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSMSData = findViewById(R.id.SMSText);
        btnLoadSMSData = findViewById(R.id.btnLoadSMSData);
        btnRequestHint = findViewById(R.id.btnRequestHint);

        btnLoadSMSData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNumberWithSmartLock("7709550305"); //9552377522
                Log.e("onClick", "onClick");
            }
        });

        btnRequestHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //
            }
        });

        try {
            requestHint();
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }

        AppSignatureHashHelper appSignatureHashHelper = new AppSignatureHashHelper(this);

        // This code requires one time to get Hash keys do comment and share key
        Log.e("HashKey", "HashKey: " + appSignatureHashHelper.getAppSignatures().get(0));

        //Toast.makeText(appSignatureHashHelper, appSignatureHashHelper.getAppSignatures().get(0), Toast.LENGTH_SHORT).show();
        txtSMSData.setText(appSignatureHashHelper.getAppSignatures().get(0));


        /*<#> Your ExampleApp code is: 123ABC78
                    FA+9qCX9VSu*/
        //https://github.com/WaveTechStudio/SMSRetrieverAPIMaster

        setUIGoogleClient();

        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        registerReceiver(smsVerificationReceiver, intentFilter);

    }

    private void setUIGoogleClient(){

        CredentialsOptions options =
                new CredentialsOptions.Builder().forceEnableSaveDialog().build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.CREDENTIALS_API, options)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();

    }

    public void startSMSRetriver(){

        // Start listening for SMS User Consent broadcasts from senderPhoneNumber
        // The Task<Void> will be successful if SmsRetriever was able to start
        // SMS User Consent, and will error if there was an error starting.
        Task<Void> taskS = SmsRetriever.getClient(MainActivity.this).startSmsUserConsent(null/*"8698656699" *//* or null */);

        // Get an instance of SmsRetrieverClient, used to start listening for a matching
        // SMS message.
        SmsRetrieverClient client = SmsRetriever.getClient(this /* context */);

        // Starts SmsRetriever, which waits for ONE matching SMS message until timeout
        // (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
        // action SmsRetriever#SMS_RETRIEVED_ACTION.
        Task<Void> task = client.startSmsRetriever();

        // Listen for success/failure of the start Task. If in a background thread, this
        // can be made blocking using Tasks.await(task, [timeout]);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Successfully started retriever, expect broadcast intent
                // ...
                Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Failed to start retriever, inspect Exception for more details
                // ...
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Construct a request for phone numbers and show the picker
    private void requestHint() throws IntentSender.SendIntentException {
        try {
            HintRequest hintRequest = new HintRequest.Builder()
                    .setPhoneNumberIdentifierSupported(true)
                    .build();
            PendingIntent intent = Credentials.getClient(this).getHintPickerIntent(hintRequest);
            startIntentSenderForResult(intent.getIntentSender(),
                    CREDENTIAL_PICKER_REQUEST, null, 0, 0, 0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREDENTIAL_PICKER_REQUEST:
                // Obtain the phone number from the result
                if (resultCode == RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    String id = credential.getId();  // will need to process phone number string
                    txtSMSData.setText(id);
                    startSMSRetriver();
                }
                break;
            // ...
            case SMS_CONSENT_REQUEST:
                if (resultCode == RESULT_OK) {
                    // Get SMS message content
                    String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                    // Extract one-time code from the message and complete verification
                    // `sms` contains the entire text of the SMS message, so you will need
                    // to parse the string.
                    String oneTimeCode = parseOneTimeCode(message); // define this function

                    // send one time code to the server
                } else {
                    // Consent canceled, handle the error ...
                }
                break;
            case RC_SAVE:
                if (resultCode == RESULT_OK) {

                }else{
                    Toast.makeText(this, "Error in storing", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    private static final int SMS_CONSENT_REQUEST = 2;  // Set to an unused request code
    private final BroadcastReceiver smsVerificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                switch (smsRetrieverStatus.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        // Get consent intent
                        // Get SMS message contents
                        String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                        String oneTimeCode = parseOneTimeCode(message);
                        // Extract one-time code from the message and complete verification
                        // by sending the code back to your server.
                        /*Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                        try {
                            // Start activity to show consent dialog to user, activity must be started in
                            // 5 minutes, otherwise you'll receive another TIMEOUT intent
                            startActivityForResult(consentIntent, SMS_CONSENT_REQUEST);
                        } catch (ActivityNotFoundException e) {
                            // Handle the exception ...
                        }*/
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        // Time out occurred, handle the error.
                        break;
                }
            }
        }
    };

    public String parseOneTimeCode(String message){

        txtSMSData.setText(message);
        return message;
    }

    public void saveNumberWithSmartLock(String phoneNumberString){
        try {
            Credential credential = new Credential.Builder(phoneNumberString)
                    .setAccountType("https://www.gmail.com") // "https://www.gmail.com" // a URL specific to the app
                    .setName("Ajinkya Jagadale")// optional: a display name if available
                    .build();
            Auth.CredentialsApi.save(mGoogleApiClient, credential).setResultCallback(  // check out other methods too e.g. delete
                    new ResultCallback() {
                        public void onResult(Result result) {
                            Status status = result.getStatus();
                            if (status.isSuccess()) {
                                Log.d("TAG", "SAVE: OK");  // already saved
                                Toast.makeText(MainActivity.this, "Already saved", Toast.LENGTH_SHORT).show();
                            } else if (status.hasResolution()) {
                                // Prompt the user to save
                                try {
                                    status.startResolutionForResult(MainActivity.this, RC_SAVE );
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getSavedNumberFromSmartLock(){
        // On the next install, retrieve the phone number
        CredentialRequest mCredentialRequest = new CredentialRequest.Builder()
                .setAccountTypes("https://www.gmail.com")  // the URL specific to the developer
                .build();
        Auth.CredentialsApi.request(mGoogleApiClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    public void onResult(CredentialRequestResult credentialRequestResult) {
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            txtSMSData.setText(credentialRequestResult.getCredential().getId());  // this is the phone number
                        }else if (credentialRequestResult.getStatus().hasResolution()) {
                            // Prompt the user to save
                            try {
                                credentialRequestResult.getStatus().startResolutionForResult(MainActivity.this, CREDENTIAL_PICKER_REQUEST/*RC_SAVE*/);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

            // Then, initiate verification and sign the user in (same as original verification logic)
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "Google API client connected.", Toast.LENGTH_SHORT).show();
        getSavedNumberFromSmartLock();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


}
