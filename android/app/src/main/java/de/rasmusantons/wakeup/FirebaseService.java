package de.rasmusantons.wakeup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.Executors;

public class FirebaseService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseService";

    public void testNotification(String message) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        String token = task.getResult();
                        Log.d(TAG, "token: " + token);
                        Toast.makeText(getBaseContext(), "token: " + token, Toast.LENGTH_SHORT).show();
                    }
                });

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("wakeup_channel", "notification_channel_name", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "wakeup_channel")
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setContentTitle("Wake Up!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_menu_camera, "stop", pendingIntent);

        int notificationId = (int) SystemClock.uptimeMillis();
        nm.notify(notificationId, builder.build());
    }

    private void testAlarm() {
        RingtoneManager rm = new RingtoneManager(this);
        rm.setType(RingtoneManager.TYPE_ALARM);
        Uri defaultAlarm = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String soundStr = prefs.getString(getString(R.string.wakeup_alarm_sound), defaultAlarm.toString());
        Uri sound = Uri.parse(soundStr);
        Ringtone ringtone = RingtoneManager.getRingtone(this, sound);
        ringtone.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        ringtone.play();
    }

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
        testNotification(message);
        testAlarm();
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
