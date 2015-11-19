package com.adsnative.sampleads;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;

public class ANSampleActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragmentactivity_main);

        DefaultFragment defaultFragment = new DefaultFragment();
        loadFragment(defaultFragment, false, false);

        Button btnListView = (Button) findViewById(R.id.btn_list_view);
        btnListView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListViewFragment listViewFragment = new ListViewFragment();
                loadFragment(listViewFragment, true, true);
            }
        });

        Button btnNativeAd = (Button) findViewById(R.id.btn_native_ad);
        btnNativeAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NativeAdFragment nativeAdFragment = new NativeAdFragment();
                loadFragment(nativeAdFragment, true, true);
            }
        });
    }

    private void loadFragment(Fragment adFragment, boolean replace, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (replace) {
            fragmentTransaction.replace(R.id.fragment_container, adFragment);
            if (addToBackStack) {
                fragmentTransaction.addToBackStack(null);
            }
        } else {
            fragmentTransaction.add(R.id.fragment_container, adFragment);
        }
        fragmentTransaction.commit();
    }
}