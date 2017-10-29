package io.cloudbeat.smspopup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class SMSPopup extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, BackgroundService.class));
        finish();
    }
}