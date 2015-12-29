package com.evgeniysharafan.googleapiclientexample.ui.activity;

import android.os.Bundle;

import com.evgeniysharafan.googleapiclientexample.R;
import com.evgeniysharafan.googleapiclientexample.ui.fragment.MapFragment;
import com.evgeniysharafan.utils.Fragments;

public class MapActivity extends GoogleApiActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_for_fragment);
        setFragment(savedInstanceState);
    }

    private void setFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragments.replace(getSupportFragmentManager(), R.id.content, MapFragment.newInstance(), null);
//            Fragments.replace(getSupportFragmentManager(), R.id.content,
//                    PermissionByClickExampleFragment.newInstance(), null);
        }
    }
}
