package de.rasmusantons.wakeup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    List<Integer> notifications = new ArrayList<>();
    private Ringtone ringtone;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "AlarmService::onStartBind");
        return null;
    }

    private void showNotification(String message) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("wakeup_channel", "notification_channel_name", NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);
        int notificationId = (int) SystemClock.uptimeMillis();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId + 1, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.putExtra("action", Action.STOP.ordinal());
        stopIntent.putExtra("notification_id", notificationId);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, notificationId + 2, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "wakeup_channel")
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setContentTitle("Wake Up!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_menu_camera, "stop", stopPendingIntent);

        notifications.add(notificationId);
        nm.notify(notificationId, builder.build());
    }

    private void startAlarm() {
        if (ringtone == null) {
            RingtoneManager rm = new RingtoneManager(this);
            rm.setType(RingtoneManager.TYPE_ALARM);
            Uri defaultAlarm = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String soundStr = prefs.getString(getString(R.string.wakeup_alarm_sound), defaultAlarm.toString());
            Uri sound = Uri.parse(soundStr);
            ringtone = RingtoneManager.getRingtone(this, sound);
            ringtone.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            ringtone.play();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int action = intent.getIntExtra("action", -1);
        Log.i(TAG, String.format("received action %d", action));
        if (action == Action.START.ordinal()) {
            String message = intent.getStringExtra("message");
            showNotification(message);
            startAlarm();
        } else if (action == Action.STOP.ordinal()) {
            int notificationId = intent.getIntExtra("notification_id", 0);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(notificationId);
            notifications.remove(Integer.valueOf(notificationId));
            if (notifications.size() == 0 && ringtone != null)
                ringtone.stop();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        ringtone.stop();
    }

    public enum Action {
        START, STOP
    }
}
