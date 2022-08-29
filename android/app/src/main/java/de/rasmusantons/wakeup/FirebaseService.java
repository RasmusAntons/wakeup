package de.rasmusantons.wakeup;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.Executors;

public class FirebaseService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String message = null;
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            message = remoteMessage.getData().get("message");
        }
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
        Intent alarmIntent = new Intent(getBaseContext(), AlarmService.class);
        alarmIntent.putExtra("action", AlarmService.Action.START.ordinal());
        alarmIntent.putExtra("message", message);
        getBaseContext().startService(alarmIntent);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        getSharedPreferences("firebase", MODE_PRIVATE).edit()
                .putString("fb_token", token)
                .putBoolean("fb_token_updated", true)
                .apply();
        if (LoginActivity.mAuthState != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                (new WakeupApi(getApplicationContext())).updateFbToken(token);
                getSharedPreferences("firebase", MODE_PRIVATE).edit()
                        .putBoolean("fb_token_updated", false)
                        .apply();
            });
        }
    }
}
