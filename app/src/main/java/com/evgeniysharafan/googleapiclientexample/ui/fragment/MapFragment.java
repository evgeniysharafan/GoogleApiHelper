package com.evgeniysharafan.googleapiclientexample.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.evgeniysharafan.googleapiclientexample.R;
import com.evgeniysharafan.googleapiclientexample.service.FetchAddressIntentService;
import com.evgeniysharafan.googleapiclientexample.ui.activity.GoogleApiActivity;
import com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.PermissionRequestCode;
import com.evgeniysharafan.googleapiclientexample.util.lib.L;
import com.evgeniysharafan.googleapiclientexample.util.lib.RandomUtils;
import com.evgeniysharafan.googleapiclientexample.util.lib.Res;
import com.evgeniysharafan.googleapiclientexample.util.lib.Toasts;
import com.evgeniysharafan.googleapiclientexample.util.lib.Utils;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.LOCATION_PERMISSIONS;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.PermissionRequestCode.LOCATION;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.STATE_IS_PERMISSIONS_DIALOG_SHOWING;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.getDeniedPermissions;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.hasAllPermissions;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.hasPermissionsResult;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.setPermissionsResult;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.shouldShowRationale;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.showSnackbar;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.showSnackbarWithOpenDetails;
import static com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

public class MapFragment extends Fragment implements ConnectionCallbacks, OnMapReadyCallback,
        OnMapClickListener, OnInfoWindowClickListener {

    private static final String STATE_LOCATION = "state_location";
    private static final String STATE_NEED_TO_ANIMATE = "state_need_to_animate";

    private GoogleMap map;
    private LatLng lastLocation;
    // usually here will be a some model instead of String
    private Map<Marker, String> markers = new HashMap<>();
    private AddressResultReceiver resultReceiver;

    private boolean isPermissionsDialogShowing;
    private boolean needToAnimate = true;
    private Snackbar permissionsSnackbar;

    private float markerColor;

    @InjectView(R.id.snackbar_container)
    CoordinatorLayout snackbarContainer;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        ButterKnife.inject(this, view);
        restoreState(savedInstanceState);
        initUI();

        return view;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            lastLocation = savedInstanceState.getParcelable(STATE_LOCATION);
            isPermissionsDialogShowing = savedInstanceState.getBoolean(STATE_IS_PERMISSIONS_DIALOG_SHOWING);
            needToAnimate = savedInstanceState.getBoolean(STATE_NEED_TO_ANIMATE);
        }
    }

    private void initUI() {
        float[] hsv = new float[3];
        Color.colorToHSV(Res.getColor(R.color.primary), hsv);
        markerColor = hsv[0];

        ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        resultReceiver = new AddressResultReceiver(new Handler());
    }

    @Override
    public void onStart() {
        super.onStart();
        askForPermissionsIfNeeded(LOCATION, LOCATION_PERMISSIONS);
    }

    private void askForPermissionsIfNeeded(@PermissionRequestCode int requestCode, String... permissions) {
        if (isPermissionsDialogShowing) {
            return;
        }

        if (permissionsSnackbar != null && permissionsSnackbar.isShown()) {
            permissionsSnackbar.dismiss();
        }

        if (hasAllPermissions(permissions)) {
            if (requestCode == LOCATION) {
                buildAndConnectClient();
            }

            return;
        }

        final String[] deniedPermissions = getDeniedPermissions(permissions);
        if (shouldShowRationale(getActivity(), deniedPermissions)) {
            permissionsSnackbar = showSnackbarWithRequestPermissions(requestCode, deniedPermissions);
        } else {
            if (hasPermissionsResult(requestCode)) {
                permissionsSnackbar = showSnackbarWithOpenDetails(snackbarContainer,
                        R.string.location_permissions_rationale_text);
            } else {
                askForPermissions(requestCode, deniedPermissions);
            }
        }
    }

    private void buildAndConnectClient() {
        if (getGoogleApiActivity().getClient() == null) {
            getGoogleApiActivity().buildLocationClient(this);
        }

        getGoogleApiActivity().connect();
    }

    private Snackbar showSnackbarWithRequestPermissions(@PermissionRequestCode final int requestCode,
                                                        final String... deniedPermissions) {
        return showSnackbar(snackbarContainer, R.string.location_permissions_rationale_text,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForPermissions(requestCode, deniedPermissions);
                    }
                });
    }

    private void askForPermissions(@PermissionRequestCode int requestCode, String... deniedPermissions) {
        requestPermissions(deniedPermissions, requestCode);
        isPermissionsDialogShowing = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        isPermissionsDialogShowing = false;
        setPermissionsResult(requestCode);
        askForPermissionsIfNeeded(requestCode, permissions);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (lastLocation == null) {
            updateLocation();
        }
    }

    private void updateLocation() {
        if (!hasAllPermissions(LOCATION_PERMISSIONS)) {
            L.e("No location permission");
            return;
        }

        if (getGoogleApiActivity() != null && getGoogleApiActivity().getClient() != null) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(getGoogleApiActivity().getClient());
            if (location != null && map != null) {
                lastLocation = new LatLng(location.getLatitude(), location.getLongitude());

                if (markers.isEmpty()) {
                    showPins();
                }

                if (needToAnimate) {
                    animateToLastLocation();
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (getGoogleApiActivity() != null) {
            getGoogleApiActivity().onConnectionSuspended(i);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapClickListener(this);

        if (lastLocation != null) {
            if (markers.isEmpty()) {
                showPins();
            }

            if (needToAnimate) {
                animateToLastLocation();
            }
        }
    }

    private void animateToLastLocation() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers.keySet()) {
            builder.include(marker.getPosition());
        }

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),
                Res.convertToIntPixels(56)));
        needToAnimate = false;
    }

    private GoogleApiActivity getGoogleApiActivity() {
        return (GoogleApiActivity) getActivity();
    }

    private void showPins() {
        if (map == null) {
            L.w("map == null, return");
            return;
        }

        map.clear();

        for (int i = 0; i < 20; i++) {
            String title = i + "";
            Marker m = map.addMarker(new MarkerOptions()
                    .position(RandomUtils.getCoordinates(lastLocation, 500))
                    .title(title)
                    .snippet(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(RandomUtils.getInt(360))));
            markers.put(m, title);
        }

        map.setMyLocationEnabled(true);
        map.setOnInfoWindowClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        addMarker(latLng, true);
        startFetchAddressService(latLng);
    }

    private void addMarker(LatLng location, boolean needToAnimate) {
        if (map == null) {
            L.w("map == null, return");
            return;
        }

        String title = location.toString();
        Marker m = map.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
                .snippet(title)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        markers.put(m, title);

        if (needToAnimate) {
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Toasts.showShort(markers.get(marker));
    }

    protected void startFetchAddressService(LatLng location) {
        if (!Utils.hasInternetConnection()) {
            L.d("no internet connection");
            return;
        }

        if (!Geocoder.isPresent()) {
            Toasts.showLong(R.string.no_geocoder_available);
            return;
        }

        getActivity().startService(new Intent(getActivity(), FetchAddressIntentService.class)
                .putExtra(FetchAddressIntentService.RECEIVER, resultReceiver)
                .putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, location)
                .putExtra(FetchAddressIntentService.IS_FULL_FORMAT_EXTRA, false));
    }

    @Override
    public void onStop() {
        getGoogleApiActivity().disconnect();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(STATE_LOCATION, lastLocation);
        outState.putBoolean(STATE_IS_PERMISSIONS_DIALOG_SHOWING, isPermissionsDialogShowing);
        outState.putBoolean(STATE_NEED_TO_ANIMATE, needToAnimate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == FetchAddressIntentService.SUCCESS_RESULT) {
                String address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
                Toasts.showLong(address);
            } else {
                //error
                Toasts.showLong(FetchAddressIntentService.RESULT_DATA_KEY);
            }
        }
    }
}
