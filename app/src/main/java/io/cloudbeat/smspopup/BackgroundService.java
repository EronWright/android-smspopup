package io.cloudbeat.smspopup;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.view.WindowManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackgroundService extends Service {
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private final IBinder mBinder = new LocalBinder();
    private final Set<AlertDialog> alerts = new HashSet<>();

    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(ACTION);
        this.registerReceiver(smsReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(smsReceiver);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void displayAlert(String sms) {
        // close any previous alerts
        for (Iterator<AlertDialog> i = alerts.iterator(); i.hasNext();) {
            AlertDialog dialog = i.next();
            try {
                dialog.cancel();
            } catch (Exception nop) {
            }
            i.remove();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(sms).setCancelable(false);

        final String url = extractUrl(sms);
        if (url != null) {
            builder.setPositiveButton("Link",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent i = new Intent(android.content.Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                            dialog.cancel();
                            alerts.remove(dialog);
                        }
                    });
        }
        builder.setNegativeButton("Close",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        alerts.remove(dialog);
                    }
                });
        AlertDialog alert = builder.create();
        alerts.add(alert);
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }

    public static String extractUrl(String text) {
        String urlRegex = "((https?|ftp):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        if (urlMatcher.find())
            return text.substring(urlMatcher.start(0), urlMatcher.end(0));

        return null;
    }

    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION.equals(action)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    final SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < pdus.length; i++)
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                    if (messages.length > -1)
                        displayAlert(messages[0].getMessageBody());
                }
            }
        }
    };
}
