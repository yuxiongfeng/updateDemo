package com.proton.update.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.proton.update.R;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.wms.logger.Logger;

public class SplashActivity extends AppCompatActivity {
    public static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };
    final RxPermissions rxPermissions = new RxPermissions(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (!this.isTaskRoot()) {
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    Logger.w("程序销毁重新开启");
                    finish();
                    return;
                }
            }
        }
        initPermission();
    }

    @SuppressLint("CheckResult")
    private void initPermission() {
        rxPermissions.request(PERMISSIONS)
                .subscribe(granted -> {
                    if (granted) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        initPermission();
                    }
                });
    }
}
