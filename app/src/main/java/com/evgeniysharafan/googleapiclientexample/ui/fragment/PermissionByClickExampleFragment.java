package com.evgeniysharafan.googleapiclientexample.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.evgeniysharafan.googleapiclientexample.R;
import com.evgeniysharafan.utils.Toasts;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.PermissionRequestCode;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.PermissionRequestCode.STORAGE;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.STORAGE_PERMISSIONS;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.getDeniedPermissions;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.hasAllPermissions;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.hasPermissionsResult;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.setPermissionsResult;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.shouldShowRationale;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.showSnackbar;
import static com.evgeniysharafan.googleapiclientexample.util.PermissionUtil.showSnackbarWithOpenDetails;

public class PermissionByClickExampleFragment extends Fragment {

    @InjectView(R.id.snackbar_container)
    CoordinatorLayout snackbarContainer;

    private Snackbar snackbar;

    public static PermissionByClickExampleFragment newInstance() {
        return new PermissionByClickExampleFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.permission_by_click_example, container, false);
        ButterKnife.inject(this, view);

        return view;
    }

    @OnClick(R.id.some_btn)
    void someClick() {
        if (!hasAllPermissions(STORAGE_PERMISSIONS)) {
            askForPermissionsIfNeeded(STORAGE, STORAGE_PERMISSIONS);
            return;
        }

        someAction();
    }

    private void askForPermissionsIfNeeded(@PermissionRequestCode int requestCode, String... permissions) {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }

        if (hasAllPermissions(permissions)) {
            someAction();
            return;
        }

        final String[] deniedPermissions = getDeniedPermissions(permissions);
        if (shouldShowRationale(getActivity(), deniedPermissions)) {
            snackbar = showSnackbarWithRequestPermissions(requestCode, deniedPermissions);
        } else {
            if (hasPermissionsResult(requestCode)) {
                snackbar = showSnackbarWithOpenDetails(snackbarContainer,
                        R.string.storage_permissions_rationale_text);
            } else {
                askForPermissions(requestCode, deniedPermissions);
            }
        }
    }

    private Snackbar showSnackbarWithRequestPermissions(@PermissionRequestCode final int requestCode,
                                                        final String... deniedPermissions) {
        return showSnackbar(snackbarContainer, R.string.storage_permissions_rationale_text,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        askForPermissions(requestCode, deniedPermissions);
                    }
                });
    }

    private void askForPermissions(@PermissionRequestCode int requestCode, String... deniedPermissions) {
        requestPermissions(deniedPermissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
                                           @NonNull int[] grantResults) {
        setPermissionsResult(requestCode);
        askForPermissionsIfNeeded(requestCode, permissions);
    }

    private void someAction() {
        Toasts.showLong("someAction");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

}
