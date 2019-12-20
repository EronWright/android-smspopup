package io.cloudbeat.smspopup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;


public class SMSPopup extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_RECEIVE_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkForSmsPermission();
    }

    /**
     * Checks whether the app has SMS permission.
     */
    private void checkForSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Permission not yet granted. Use requestPermissions().
            // MY_PERMISSIONS_REQUEST_SEND_SMS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_SMS},
                    MY_PERMISSIONS_REQUEST_RECEIVE_SMS);
        } else {
            // Permission already granted.
            startService();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // For the requestCode, check if permission was granted or not.
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECEIVE_SMS: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.RECEIVE_SMS)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    startService();
                } else {
                    // Permission denied.
                }
                finish();
            }
        }
    }

    private void startService() {
        startService(new Intent(this, BackgroundService.class));
    }
}