package com.evgeniysharafan.googleapiclientexample.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;

import com.evgeniysharafan.googleapiclientexample.R;
import com.evgeniysharafan.googleapiclientexample.util.lib.L;
import com.evgeniysharafan.googleapiclientexample.util.lib.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String RECEIVER = Utils.getPackageName() + ".RECEIVER";
    public static final String RESULT_DATA_KEY = "result_data_key";
    public static final String LOCATION_DATA_EXTRA = "location_data_extra";
    public static final String IS_FULL_FORMAT_EXTRA = "is_full_format_extra";


    protected ResultReceiver receiver;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";
        receiver = intent.getParcelableExtra(RECEIVER);

        if (receiver == null) {
            L.e("No receiver received. There is nowhere to send the results.");
            return;
        }

        LatLng location = intent.getParcelableExtra(LOCATION_DATA_EXTRA);
        boolean isFullFormat = intent.getBooleanExtra(IS_FULL_FORMAT_EXTRA, true);

        if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            L.d(errorMessage);
            deliverResultToReceiver(FAILURE_RESULT, errorMessage);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            L.e(errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            L.e(errorMessage + ". " + "Latitude = " + location.latitude +
                    ", Longitude = " + location.longitude, illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.isEmpty()) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                L.d(errorMessage);
            }

            deliverResultToReceiver(FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            if (isFullFormat) {
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
            } else {
                addressFragments.add(address.getAddressLine(0));
            }

            deliverResultToReceiver(SUCCESS_RESULT, TextUtils.join(", ", addressFragments));
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_DATA_KEY, message);
        receiver.send(resultCode, bundle);
    }
}
