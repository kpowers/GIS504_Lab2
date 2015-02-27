package com.example.kelsey.gis504_lab2b_part3;

import android.app.Fragment;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
//import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.TextView;
import android.location.Location;

//import com.google.android.gms.common.api.ConnectionResult;
//import com.google.android.gms.api.GoogleApiClient;
//import com.google.android.gms.api.GoogleApiClient.ConnectionCallbacks;
//import com.google.android.gms.api.GoogleApiClient.OnConnectionFailedListener;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.android.support.v4.app.FragmentActivity;


public class MainActivity extends ActionBarActivity
    implements ConnectionCallbacks,OnConnectionFailedListener {
   //not sure if I need this line...
    private GoogleApiClient mGoogleApiClient;
    //request code to use when launching the error resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    //unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    //Boolean to track whether the app is already resolving an error. For example if the screen is rotated and the activity reloads
    private boolean mResolvingError = false;
    //Boolean to track across activities
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    //keep track of boolean across activity restarts (user rotating screen)
    //save the boolean in the activity's saved instance data using onSaveInstanceState().
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //this class provides methods that allow you to specify the Google APIs and the OAuth 2.0 scopes
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
    }


    @Override
    public void onConnected(Bundle connectionHint){
        //provides a simple way of getting a device's location and is well suited for apps that do not require fine-grained location and that do not need location updates
        //gets the best and most recent location currently available, which may be null in rare cases when  a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null){
            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        }else {
            Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int cause){
        //The connection has been interrupted. Disable any UI components that depend on Google APIs, until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result){
        //This callback is important for handling errors that may occur while attempting to connect with Google.
        if (mResolvingError){
            //already attempting to resolve an error.
            return;
        } else if (result.hasResolution()){
            try {
                mResolvingError=true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (SendIntentException e) {
                //there was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            //Show dialog using GOoglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }
//Building the error dialog:
    /*creates the dialog for an error message */
    private void showErrorDialog(int errorCode) {
        //create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        //pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }
    //called from the ErrorDialog when the dialog is dismissed
    public void onDialogDismissed(){
        mResolvingError = false;
    }

    // a fragment to display an error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment(){
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            //get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog){
            ((MainActivity)getActivity()).onDialogDismissed();
        }
    }

    //once the user completes the resolution provided by startResolutionForResult() or GooglePlayServicesUtil.getErrorDialog()
    //your activity receives the onActivityResult() callback with the RESULT_OK result code. You can then call connect()
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_RESOLVE_ERROR){
            //boolean mResolvingError keeps track of app state while the user is resolving the error to avoid repetitive attempts to resolve the same error.
            mResolvingError = false;
            if (resultCode == RESULT_OK){
                //make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleAPiClient.isConnected()){
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        if (!mResolvingError){
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
