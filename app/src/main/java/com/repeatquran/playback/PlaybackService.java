package com.repeatquran.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;
//import androidx.media.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.media.session.MediaButtonReceiver;
import androidx.annotation.NonNull;

import android.app.Service;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.repeatquran.MainActivity;
import com.repeatquran.R;

import android.support.v4.media.session.MediaSessionCompat;

public class PlaybackService extends Service {
    public static final String ACTION_START = "com.repeatquran.action.START";
    public static final String ACTION_STOP = "com.repeatquran.action.STOP";
    public static final String ACTION_PLAY = "com.repeatquran.action.PLAY";
    public static final String ACTION_PAUSE = "com.repeatquran.action.PAUSE";
    public static final String ACTION_NEXT = "com.repeatquran.action.NEXT";
    public static final String ACTION_PREV = "com.repeatquran.action.PREV";

    private static final String CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private PlaybackManager playbackManager;
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    private PlayerNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        playbackManager = new PlaybackManager(
                this,
                new SimpleVerseProvider(),
                2,
                new PlaybackManager.Callback() {
                    @Override
                    public void onBufferAppended(int index) {
                        Log.d("PlaybackService", "Buffer appended: " + index);
                    }

                    @Override
                    public void onPlaybackError(String message) {
                        Log.e("PlaybackService", "Playback error: " + message);
                    }

                    @Override
                    public void onStateChanged(int state) { /* no-op */ }
                }
        );
        player = playbackManager.getPlayer();

        mediaSession = new MediaSessionCompat(this, "RepeatQuranSession");
        mediaSession.setActive(true);

        PendingIntent sessionActivity = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(new Intent(this, MainActivity.class))
                .getPendingIntent(0, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        mediaSession.setSessionActivity(sessionActivity);

        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @NonNull
                    @Override
                    public String getCurrentContentTitle(@NonNull Player player) {
                        return "Repeat Quran";
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(@NonNull Player player) {
                        return sessionActivity;
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(@NonNull Player player) {
                        return "Playing";
                    }

                    @Nullable
                    @Override
                    public android.graphics.Bitmap getCurrentLargeIcon(@NonNull Player player, @NonNull PlayerNotificationManager.BitmapCallback callback) {
                        return null;
                    }
                })
                .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationPosted(int id, Notification notification, boolean ongoing) {
                        if (ongoing) {
                            startForeground(id, notification);
                        }
                    }

                    @Override
                    public void onNotificationCancelled(int id, boolean dismissedByUser) {
                        stopSelf();
                    }
                })
                .build();
        notificationManager.setMediaSessionToken(mediaSession.getSessionToken());
        notificationManager.setSmallIcon(R.drawable.ic_launcher_foreground);
        notificationManager.setPlayer(player);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_PLAY.equals(action) || ACTION_START.equals(action) || action == null) {
            playbackManager.prepareAndStart();
        } else if (ACTION_PAUSE.equals(action)) {
            if (player != null) player.pause();
        } else if (ACTION_NEXT.equals(action)) {
            if (player != null) player.seekToNextMediaItem();
        } else if (ACTION_PREV.equals(action)) {
            if (player != null) player.seekToPreviousMediaItem();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.setPlayer(null);
        mediaSession.setActive(false);
        mediaSession.release();
        playbackManager.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not supporting binding for MVP
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Audio playback");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}
