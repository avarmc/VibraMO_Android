package com.psymaker.vibraimage.vibrama;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.psymaker.vibraimage.vibrama.med.R;

import java.util.Map;

public class VibraimageStarter extends AppCompatActivity {

    private ActivityResultContracts.RequestMultiplePermissions multiplePermissionsContract;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

    private static final String[] APP_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        multiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();
        multiplePermissionLauncher = registerForActivityResult(multiplePermissionsContract, isGranted -> {
            Log.d("PERMISSIONS", "Launcher result: " + isGranted.toString());
            askPermissions(multiplePermissionLauncher);
        });
        askPermissions(multiplePermissionLauncher);
    }
    private void startVI() {
        final VibraimageStarter pThis = this;
        runOnUiThread(new Runnable() {
            public void run() {
                Intent intent = new Intent(pThis, VibraimageActivity.class);
                startActivity(intent);
            }
        });

    //    finish();
    }

    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        if (!hasPermissions(APP_PERMISSIONS)) {
            Log.d("PERMISSIONS", "Launching multiple contract permission launcher for ALL required permissions");
                    multiplePermissionLauncher.launch(APP_PERMISSIONS);

        } else {
            startVI();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Permission is not granted: " + permission);
                    return false;
                }
                Log.d("PERMISSIONS", "Permission already granted: " + permission);
            }
            return true;
        }
        return false;
    }
}
