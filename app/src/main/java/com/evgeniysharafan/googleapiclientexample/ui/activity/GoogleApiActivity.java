package com.evgeniysharafan.googleapiclientexample.ui.activity;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.evgeniysharafan.utils.L;
import com.evgeniysharafan.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.LOCATION_PERMISSIONS;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.hasAllPermissions;
import static com.google.android.gms.common.api.GoogleApiClient.Builder;
import static com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

@SuppressWarnings("unused")
public class GoogleApiActivity extends AppCompatActivity implements OnConnectionFailedListener {

    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private GoogleApiClient googleApiClient;

    public synchronized void buildLocationClient(ConnectionCallbacks callbacks) {
        if (hasAllPermissions(LOCATION_PERMISSIONS)) {
            googleApiClient = new Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    public GoogleApiClient getClient() {
        return googleApiClient;
    }

    public void connect() {
        if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    public void disconnect() {
        if (googleApiClient != null && (googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.disconnect();
        }
    }

    public void destroyClient() {
        if (googleApiClient != null && (googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.disconnect();
            googleApiClient = null;
        }
    }

    public static boolean isGooglePlayServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(Utils.getApp()) == ConnectionResult.SUCCESS;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        L.i("onConnectionFailed = " + connectionResult.toString());

        //TODO It looks like GPS 9.4 have a method showErrorNotification (Context context, ConnectionResult result).
        // Check it and create another one OnConnectionFailedListener for handling errors in background
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                L.e(e, "Could not resolve ConnectionResult");
                connect();
            }
        } else {
            GoogleApiAvailability.getInstance().showErrorDialogFragment(this, connectionResult.getErrorCode(), REQUEST_RESOLVE_ERROR);
        }
    }

    public void onConnectionSuspended(int i) {
        L.i("onConnectionSuspended " + i);
        connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            if (resultCode == RESULT_OK) {
                connect();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
