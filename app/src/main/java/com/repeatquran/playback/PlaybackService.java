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
import com.repeatquran.data.SessionRepository;
import com.repeatquran.data.db.SessionEntity;

import android.support.v4.media.session.MediaSessionCompat;

public class PlaybackService extends Service {
    public static final String ACTION_START = "com.repeatquran.action.START";
    public static final String ACTION_STOP = "com.repeatquran.action.STOP";
    public static final String ACTION_PLAY = "com.repeatquran.action.PLAY";
    public static final String ACTION_PAUSE = "com.repeatquran.action.PAUSE";
    public static final String ACTION_NEXT = "com.repeatquran.action.NEXT";
    public static final String ACTION_PREV = "com.repeatquran.action.PREV";
    public static final String ACTION_LOAD_SINGLE = "com.repeatquran.action.LOAD_SINGLE";
    public static final String ACTION_LOAD_RANGE = "com.repeatquran.action.LOAD_RANGE";

    private static final String CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private PlaybackManager playbackManager;
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    private PlayerNotificationManager notificationManager;
    private SessionRepository sessionRepo;
    private Long currentSessionId = null;
    private Integer currentCyclesRequested = null;

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
        sessionRepo = new SessionRepository(this);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED && currentSessionId != null) {
                    // Mark session ended
                    long now = System.currentTimeMillis();
                    sessionRepo.markEnded(currentSessionId, now, currentCyclesRequested);
                    currentSessionId = null;
                    currentCyclesRequested = null;
                }
            }
        });

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
            // Log selected repeat count for proof in UHW-7
            int repeatCount = getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1);
            String repeatStr = (repeatCount == -1) ? "∞" : String.valueOf(repeatCount);
            Log.d("PlaybackService", "Starting playback with repeat count=" + repeatStr);
            if (player.getMediaItemCount() > 0) {
                player.play();
            } else {
                playbackManager.setFeedingEnabled(true);
                playbackManager.setPassageRepeatCount(repeatCount);
                playbackManager.prepareAndStart();
                // Log provider-based session start
                SessionEntity e = new SessionEntity();
                e.startedAt = System.currentTimeMillis();
                e.sourceType = "provider";
                e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                e.repeatCount = repeatCount;
                e.cyclesRequested = repeatCount;
                currentCyclesRequested = repeatCount;
                currentSessionId = sessionRepo.insert(e);
            }
        } else if (ACTION_PAUSE.equals(action)) {
            if (player != null) player.pause();
        } else if (ACTION_NEXT.equals(action)) {
            if (player != null) player.seekToNextMediaItem();
        } else if (ACTION_PREV.equals(action)) {
            if (player != null) player.seekToPreviousMediaItem();
        } else if (ACTION_LOAD_SINGLE.equals(action)) {
            int sura = intent.getIntExtra("sura", 1);
            int ayah = intent.getIntExtra("ayah", 1);
            // Build exact playlist based on repeat setting
            String sss = String.format("%03d", sura);
            String aaa = String.format("%03d", ayah);
            int repeat = intent.getIntExtra("repeat",
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false); // prevent provider from appending more items
            java.util.List<MediaItem> cycle = buildSingleAyahCycle(sss, aaa);
            enqueueCycles(cycle, repeat);
            player.prepare();
            player.play();
            SessionEntity e = new SessionEntity();
            e.startedAt = System.currentTimeMillis();
            e.sourceType = "single";
            e.startSurah = sura;
            e.startAyah = ayah;
            e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
            e.repeatCount = repeat;
            e.cyclesRequested = repeat;
            currentCyclesRequested = repeat;
            currentSessionId = sessionRepo.insert(e);
        } else if (ACTION_LOAD_RANGE.equals(action)) {
            int ss = intent.getIntExtra("ss", 1);
            int sa = intent.getIntExtra("sa", 1);
            int es = intent.getIntExtra("es", 1);
            int ea = intent.getIntExtra("ea", 1);
            int repeat = intent.getIntExtra("repeat",
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));

            // Build URLs for the inclusive range (ss:sa) -> (es:ea)
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            java.util.List<MediaItem> cycle = buildRangeCycle(ss, sa, es, ea);
            enqueueCycles(cycle, repeat);
            player.prepare();
            player.play();
            SessionEntity e = new SessionEntity();
            e.startedAt = System.currentTimeMillis();
            e.sourceType = "range";
            e.startSurah = ss;
            e.startAyah = sa;
            e.endSurah = es;
            e.endAyah = ea;
            e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
            e.repeatCount = repeat;
            e.cyclesRequested = repeat;
            currentCyclesRequested = repeat;
            currentSessionId = sessionRepo.insert(e);
        }
        return START_STICKY;
    }

    private int getAyahCount(int surah) {
        final int[] AYAH_COUNTS = new int[] {
                7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
                123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
                112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
                34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
                54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
                60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
                14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
                28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
                29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
                15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
                11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
                5, 4, 5, 6
        };
        return AYAH_COUNTS[surah - 1];
    }

    private java.util.List<String> getSelectedReciterIds() {
        String saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (saved == null || saved.isEmpty()) return ids;
        for (String s : saved.split(",")) if (!s.isEmpty()) ids.add(s);
        return ids;
    }

    private java.util.List<MediaItem> buildSingleAyahCycle(String sss, String aaa) {
        java.util.List<String> reciters = getSelectedReciterIds();
        if (reciters.isEmpty()) reciters = java.util.Arrays.asList("Abdurrahmaan_As-Sudais_64kbps");
        java.util.List<MediaItem> cycle = new java.util.ArrayList<>();
        java.util.List<String> reciterNames = getReciterNames(reciters);
        StringBuilder orderLog = new StringBuilder("Cycle order (single ayah): ");
        for (int i = 0; i < reciters.size(); i++) {
            String rid = reciters.get(i);
            String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
            cycle.add(MediaItem.fromUri(url));
            orderLog.append(reciterNames.get(i));
            if (i < reciters.size() - 1) orderLog.append(" -> ");
        }
        Log.d("PlaybackService", orderLog.toString());
        return cycle;
    }

    private java.util.List<MediaItem> buildRangeCycle(int ss, int sa, int es, int ea) {
        java.util.List<String> reciters = getSelectedReciterIds();
        if (reciters.isEmpty()) reciters = java.util.Arrays.asList("Abdurrahmaan_As-Sudais_64kbps");
        java.util.List<MediaItem> cycle = new java.util.ArrayList<>();
        java.util.List<String> reciterNames = getReciterNames(reciters);
        StringBuilder orderLog = new StringBuilder("Cycle order (range): ");
        for (int i = 0; i < reciters.size(); i++) {
            String rid = reciters.get(i);
            orderLog.append(reciterNames.get(i));
            if (i < reciters.size() - 1) orderLog.append(" -> ");
            for (int s = ss; s <= es; s++) {
                int startAyah = (s == ss) ? sa : 1;
                int endAyah = (s == es) ? ea : getAyahCount(s);
                for (int a = startAyah; a <= endAyah; a++) {
                    String sss = String.format("%03d", s);
                    String aaa = String.format("%03d", a);
                    String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                    cycle.add(MediaItem.fromUri(url));
                }
            }
        }
        Log.d("PlaybackService", orderLog.toString());
        return cycle;
    }

    private void enqueueCycles(java.util.List<MediaItem> cycle, int repeat) {
        if (repeat == -1) {
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            for (MediaItem mi : cycle) player.addMediaItem(mi);
            Log.d("PlaybackService", "Enqueued 1 cycle (∞ loop), items=" + cycle.size());
        } else {
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            int n = Math.max(1, repeat);
            for (int i = 0; i < n; i++) for (MediaItem mi : cycle) player.addMediaItem(mi);
            Log.d("PlaybackService", "Enqueued " + n + " cycles, itemsPerCycle=" + cycle.size() + ", total=" + (n * cycle.size()));
        }
    }

    private java.util.List<String> getReciterNames(java.util.List<String> ids) {
        String[] names = getResources().getStringArray(R.array.reciter_names);
        String[] allIds = getResources().getStringArray(R.array.reciter_ids);
        java.util.Map<String, String> idToName = new java.util.HashMap<>();
        for (int i = 0; i < allIds.length; i++) idToName.put(allIds[i], names[i]);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String id : ids) out.add(idToName.getOrDefault(id, id));
        return out;
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
