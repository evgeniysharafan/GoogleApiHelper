package com.evgeniysharafan.googleapiclientexample.ui.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.evgeniysharafan.googleapiclientexample.R;
import com.evgeniysharafan.utils.L;
import com.evgeniysharafan.utils.Res;
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
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private GoogleApiClient googleApiClient;
    private boolean isResolvingError;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        isResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
    }

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
        if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting() && !isResolvingError) {
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        L.i(connectionResult.toString());

        if (isResolvingError) {
            // Already attempting to resolve an error.
            return;
        }

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                isResolvingError = true;
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                L.e(e, "Could not resolve ConnectionResult");
                connect();
            }
        } else {
            // not resolvable... so show an error message
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    public void onConnectionSuspended(int i) {
        L.i("onConnectionSuspended " + i);
        connect();
    }

    private void showErrorDialog(int errorCode) {
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), DIALOG_ERROR);

        isResolvingError = true;
    }

    public void onDialogDismissed() {
        isResolvingError = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            isResolvingError = false;
            if (resultCode == RESULT_OK) {
                connect();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(STATE_RESOLVING_ERROR, isResolvingError);
    }

    public static class ErrorDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
            if (dialog != null) {
                return dialog;
            } else {
                // no built-in dialog
                return new AlertDialog.Builder(getActivity())
                        .setMessage(Res.getString(R.string.play_services_error_fmt, errorCode))
                        .setNeutralButton(R.string.ok, null)
                        .create();
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (getActivity() != null) {
                ((GoogleApiActivity) getActivity()).onDialogDismissed();
            }
        }
    }

}
