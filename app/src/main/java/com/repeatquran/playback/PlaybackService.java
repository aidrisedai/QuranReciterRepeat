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
import android.os.Handler;
import android.os.Looper;

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
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.repeatquran.MainActivity;
import com.repeatquran.R;
import com.repeatquran.data.SessionRepository;
import com.repeatquran.data.db.SessionEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;
import com.repeatquran.data.db.PageSegmentDao;
import com.repeatquran.data.db.PageSegmentEntity;
import com.repeatquran.data.CacheManager;

import android.support.v4.media.session.MediaSessionCompat;

public class PlaybackService extends Service {
    public static final String ACTION_START = "com.repeatquran.action.START";
    public static final String ACTION_STOP = "com.repeatquran.action.STOP";
    public static final String ACTION_PLAY = "com.repeatquran.action.PLAY";
    public static final String ACTION_PAUSE = "com.repeatquran.action.PAUSE";
    public static final String ACTION_TOGGLE = "com.repeatquran.action.TOGGLE";
    public static final String ACTION_NEXT = "com.repeatquran.action.NEXT";
    public static final String ACTION_PREV = "com.repeatquran.action.PREV";
    public static final String ACTION_LOAD_SINGLE = "com.repeatquran.action.LOAD_SINGLE";
    public static final String ACTION_LOAD_RANGE = "com.repeatquran.action.LOAD_RANGE";
    public static final String ACTION_LOAD_PAGE = "com.repeatquran.action.LOAD_PAGE";
    public static final String ACTION_LOAD_SURAH = "com.repeatquran.action.LOAD_SURAH";
    public static final String ACTION_RESUME = "com.repeatquran.action.RESUME";
    public static final String ACTION_SET_SPEED = "com.repeatquran.action.SET_SPEED";
    public static final String ACTION_PLAYBACK_STATE = "com.repeatquran.action.PLAYBACK_STATE";
    public static final String ACTION_RETRY_ITEM = "com.repeatquran.action.RETRY_ITEM";
    public static final String ACTION_SKIP_ITEM = "com.repeatquran.action.SKIP_ITEM";
    public static final String ACTION_SIMULATE_FOCUS_LOSS = "com.repeatquran.action.SIM_FOCUS_LOSS";
    public static final String ACTION_SIMULATE_FOCUS_GAIN = "com.repeatquran.action.SIM_FOCUS_GAIN";

    private static final String CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private PlaybackManager playbackManager;
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    private PlayerNotificationManager notificationManager;
    private SessionRepository sessionRepo;
    private Long currentSessionId = null;
    private Integer currentCyclesRequested = null;
    private java.util.concurrent.ExecutorService ioExecutor;
    private Handler mainHandler;
    private CacheManager cacheManager;
    private java.util.Map<String, Integer> retryCounts = new java.util.HashMap<>();
    private static final int ERROR_NOTIFICATION_ID = 2002;
    
    // Thread-safe state management
    private ThreadSafePlaybackState safeState;
    
    // New State Machine for Action Processing
    public enum PlaybackServiceState {
        IDLE,                    // Ready for new actions
        PREPARING_DATA,         // Loading/building playlist data
        EXECUTING_PLAYBACK,     // Actually setting up player
        ERROR                   // Error state, needs reset
    }
    
    private volatile PlaybackServiceState currentState = PlaybackServiceState.IDLE;

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
        ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        cacheManager = CacheManager.get(this);
        safeState = new ThreadSafePlaybackState(this);

        // Apply persisted playback speed (global)
        try {
            float speed = getSharedPreferences("rq_prefs", MODE_PRIVATE).getFloat("playback.speed", 1.0f);
            applyPlaybackSpeed(speed);
        } catch (Exception ignored) {}

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED && currentSessionId != null) {
                    final long id = currentSessionId;
                    final Integer cycles = currentCyclesRequested;
                    currentSessionId = null;
                    currentCyclesRequested = null;
                    ioExecutor.execute(() -> sessionRepo.markEnded(id, System.currentTimeMillis(), cycles));
                }
                broadcastState();
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                int idx = player.getCurrentMediaItemIndex();
                
                // Use thread-safe loop detection
                if (safeState.checkAndIncrementLoop(idx)) {
                    // Stop looping and end playback cleanly
                    mainHandler.post(() -> {
                        try {
                            player.setRepeatMode(Player.REPEAT_MODE_OFF);
                            player.stop();
                        } catch (Exception ignored) {}
                    });
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                // Push an update specifically when play/pause toggles
                broadcastState();
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                boolean online = com.repeatquran.util.NetworkUtil.isOnline(PlaybackService.this);
                MediaItem mi = player.getCurrentMediaItem();
                String uriStr = mi != null && mi.playbackProperties != null && mi.playbackProperties.uri != null ? mi.playbackProperties.uri.toString() : "";
                {
                    java.util.Map<String, Object> evErr = new java.util.HashMap<>();
                    evErr.put("online", online);
                    evErr.put("uri", uriStr);
                    evErr.put("message", error.getMessage());
                    com.repeatquran.analytics.AnalyticsLogger.get(PlaybackService.this).log("error_playback", evErr);
                }
                if (!online) {
                    Log.w("PlaybackService", "Offline playback error; skipping item: " + uriStr);
                    mainHandler.post(() -> android.widget.Toast.makeText(PlaybackService.this, "Offline: skipping uncached item", android.widget.Toast.LENGTH_SHORT).show());
                    // Skip to next item when offline and current failed
                    mainHandler.post(() -> {
                        if (player.hasNextMediaItem()) player.seekToNextMediaItem(); else player.stop();
                        cancelErrorNotification();
                    });
                } else {
                    Log.e("PlaybackService", "Playback error online: " + error.getMessage());
                    int count = retryCounts.getOrDefault(uriStr, 0);
                    if (count < 1) {
                        retryCounts.put(uriStr, count + 1);
                        com.repeatquran.analytics.AnalyticsLogger.get(PlaybackService.this).log("error_retry", java.util.Collections.singletonMap("uri", uriStr));
                        int idx = player.getCurrentMediaItemIndex();
                        Log.w("PlaybackService", "Retrying item once: index=" + idx + ", uri=" + uriStr);
                        mainHandler.post(() -> {
                            try {
                                player.seekTo(Math.max(idx, 0), 0);
                                player.prepare();
                                player.play();
                            } catch (Exception e) {
                                Log.e("PlaybackService", "Retry failed to start", e);
                                if (player.hasNextMediaItem()) player.seekToNextMediaItem(); else player.stop();
                            }
                        });
                    } else {
                        Log.e("PlaybackService", "Item failed after retry; showing actions");
                        com.repeatquran.analytics.AnalyticsLogger.get(PlaybackService.this).log("error_actionable", java.util.Collections.singletonMap("uri", uriStr));
                        showErrorNotification("Playback failed", "Retry this ayah or skip to next");
                    }
                }
                broadcastState();
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

        broadcastState();
    }

    /**
     * Central action processor that uses state machine to prevent concurrent actions
     * This is the single entry point for all content-loading actions
     */
    private int processAction(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        
        // Quick actions that don't need state management
        if (ACTION_STOP.equals(action) || ACTION_PLAY.equals(action) || ACTION_PAUSE.equals(action) || 
            ACTION_NEXT.equals(action) || ACTION_PREV.equals(action) || ACTION_TOGGLE.equals(action) ||
            ACTION_SET_SPEED.equals(action) || ACTION_RESUME.equals(action) || ACTION_START.equals(action) ||
            ACTION_RETRY_ITEM.equals(action) || ACTION_SKIP_ITEM.equals(action) || 
            ACTION_SIMULATE_FOCUS_LOSS.equals(action) || ACTION_SIMULATE_FOCUS_GAIN.equals(action) ||
            action == null) {
            // Pass through to legacy handler for simple actions
            return onStartCommandLegacy(intent, flags, startId);
        }
        
        // Content-loading actions that need state management
        if (ACTION_LOAD_SINGLE.equals(action) || ACTION_LOAD_RANGE.equals(action) || 
            ACTION_LOAD_PAGE.equals(action) || ACTION_LOAD_SURAH.equals(action)) {
            
            if (!canAcceptAction(action)) {
                // State machine prevents concurrent execution
                return START_STICKY;
            }
            
            // Use new clean implementations where available
            if (ACTION_LOAD_SINGLE.equals(action)) {
                return handleLoadSingle(intent);
            }
            if (ACTION_LOAD_RANGE.equals(action)) {
                return handleLoadRange(intent);
            }
            if (ACTION_LOAD_PAGE.equals(action)) {
                return handleLoadPage(intent);
            }
            if (ACTION_LOAD_SURAH.equals(action)) {
                return handleLoadSurah(intent);
            }
            
            // Legacy actions (will be refactored in subsequent phases)
            transitionTo(PlaybackServiceState.PREPARING_DATA);
            try {
                int result = onStartCommandLegacy(intent, flags, startId);
                resetToIdle();
                return result;
            } catch (Exception e) {
                Log.e("PlaybackService", "Action " + action + " failed", e);
                resetToIdle();
                return START_STICKY;
            }
        }
        
        Log.w("PlaybackService", "Unknown action: " + action);
        return START_STICKY;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return processAction(intent, flags, startId);
    }
    
    /**
     * Legacy action handler - will be gradually refactored
     */
    private int onStartCommandLegacy(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            try {
                if (player != null) {
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                    player.stop();
                    player.clearMediaItems();
                }
                cancelErrorNotification();
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
                android.widget.Toast.makeText(this, "Stopped playback", android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
            stopForeground(true);
            stopSelf();
            broadcastState();
            return START_NOT_STICKY;
        }
        if (ACTION_SIMULATE_FOCUS_LOSS.equals(action)) {
            Log.w("PlaybackService", "Simulated focus loss (call) — pausing");
            if (player != null) player.pause();
            broadcastState();
            return START_STICKY;
        }
        if (ACTION_SIMULATE_FOCUS_GAIN.equals(action)) {
            Log.w("PlaybackService", "Simulated focus gain — resuming if queue exists");
            if (player != null && player.getMediaItemCount() > 0) player.play();
            broadcastState();
            return START_STICKY;
        }
        if (ACTION_RETRY_ITEM.equals(action)) {
            cancelErrorNotification();
            MediaItem mi = player.getCurrentMediaItem();
            String uriStr = mi != null && mi.playbackProperties != null && mi.playbackProperties.uri != null ? mi.playbackProperties.uri.toString() : "";
            retryCounts.remove(uriStr); // allow another auto-retry sequence
            int idx = player.getCurrentMediaItemIndex();
            mainHandler.post(() -> {
                player.seekTo(Math.max(idx, 0), 0);
                player.prepare();
                player.play();
            });
            return START_STICKY;
        }
        if (ACTION_SKIP_ITEM.equals(action)) {
            cancelErrorNotification();
            mainHandler.post(() -> {
                if (player.hasNextMediaItem()) player.seekToNextMediaItem(); else player.stop();
            });
            return START_STICKY;
        }
        if (ACTION_RESUME.equals(action)) {
            // Delegate to resume handler which will either resume current queue or rebuild from snapshot
            onResumeRequested();
            return START_STICKY;
        }
        if (ACTION_TOGGLE.equals(action)) {
            if (player.isPlaying()) {
                player.pause();
                mainHandler.post(() -> android.widget.Toast.makeText(this, "Paused", android.widget.Toast.LENGTH_SHORT).show());
                broadcastState();
            } else {
                mainHandler.post(() -> android.widget.Toast.makeText(this, "Resuming", android.widget.Toast.LENGTH_SHORT).show());
                onResumeRequested();
            }
            return START_STICKY;
        }
        if (ACTION_PLAY.equals(action)) {
            if (player.getMediaItemCount() > 0) {
                player.play();
            } else {
                Log.d("PlaybackService", "Play pressed with empty queue; not seeding provider.");
            }
            broadcastState();
        } else if (ACTION_START.equals(action) || action == null) {
            // Warm service only; never auto-play on start
            broadcastState();
        } else if (ACTION_PAUSE.equals(action)) {
            if (player != null) player.pause();
            broadcastState();
        } else if (ACTION_NEXT.equals(action)) {
            if (player != null) player.seekToNextMediaItem();
            broadcastState();
        } else if (ACTION_PREV.equals(action)) {
            if (player != null) player.seekToPreviousMediaItem();
            broadcastState();
        } else if (ACTION_SET_SPEED.equals(action)) {
            float speed = intent.getFloatExtra("speed", 1.0f);
            // Clamp to sensible range
            if (speed < 0.5f) speed = 0.5f;
            if (speed > 2.0f) speed = 2.0f;
            final float fSpeed = speed;
            getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putFloat("playback.speed", fSpeed).apply();
            {
                java.util.Map<String, Object> ev = new java.util.HashMap<>();
                ev.put("speed", fSpeed);
                com.repeatquran.analytics.AnalyticsLogger.get(this).log("speed_changed", ev);
            }
            mainHandler.post(() -> applyPlaybackSpeed(fSpeed));
            android.widget.Toast.makeText(this, "Speed set to " + fSpeed + "×", android.widget.Toast.LENGTH_SHORT).show();
        }
        
        // All content-loading actions (LOAD_SINGLE, LOAD_RANGE, LOAD_PAGE, LOAD_SURAH) now use
        // the new clean implementations and will never reach this legacy handler.
        // Legacy implementations have been removed as they are no longer used.
        
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
        // Check for override first (thread-safe)
        java.util.List<String> override = safeState.getRecitersOverride();
        if (override != null && !override.isEmpty()) {
            return new java.util.ArrayList<>(override);
        }
        
        // Fall back to preferences
        String saved = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (saved == null || saved.isEmpty()) return ids;
        for (String s : saved.split(",")) if (!s.isEmpty()) ids.add(s);
        return ids;
    }

    private static class CycleResult {
        java.util.List<MediaItem> items = new java.util.ArrayList<>();
        int cachedCount;
        int totalCount;
    }

    private CycleResult buildSingleAyahCycle(String sss, String aaa) {
        java.util.List<String> reciters = getSelectedReciterIds();
        CycleResult out = new CycleResult();
        java.util.List<String> reciterNames = getReciterNames(reciters);
        StringBuilder orderLog = new StringBuilder("Cycle order (single ayah): ");
        for (int i = 0; i < reciters.size(); i++) {
            String rid = reciters.get(i);
            String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
            java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
            if (cached.exists()) {
                out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached)));
                out.cachedCount++;
            } else {
                out.items.add(MediaItem.fromUri(url));
                cacheManager.cacheAsync(url, rid, sss, aaa);
            }
            out.totalCount++;
            orderLog.append(reciterNames.get(i));
            if (i < reciters.size() - 1) orderLog.append(" -> ");
        }
        Log.d("PlaybackService", orderLog.toString());
        return out;
    }

    private CycleResult buildRangeCycle(int ss, int sa, int es, int ea, boolean halfSplit) {
        java.util.List<String> reciters = getSelectedReciterIds();
        CycleResult out = new CycleResult();
        if (!halfSplit || reciters.size() == 1) {
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
                        java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                        if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                        else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                        out.totalCount++;
                    }
                }
            }
            Log.d("PlaybackService", orderLog.toString());
        } else {
            // Build verses and split strictly into two contiguous halves using the first two reciters
            java.util.List<String[]> verses = new java.util.ArrayList<>(); // [sss, aaa]
            for (int s = ss; s <= es; s++) {
                int startAyah = (s == ss) ? sa : 1;
                int endAyah = (s == es) ? ea : getAyahCount(s);
                for (int a = startAyah; a <= endAyah; a++)
                    verses.add(new String[]{String.format("%03d", s), String.format("%03d", a)});
            }
            String rA = reciters.get(0);
            String rB = reciters.size() > 1 ? reciters.get(1) : reciters.get(0);
            int N = verses.size();
            int split = N / 2; // first half size
            for (int i = 0; i < N; i++) {
                String rid = (i < split) ? rA : rB;
                String sss = verses.get(i)[0];
                String aaa = verses.get(i)[1];
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                out.totalCount++;
            }
            Log.d("PlaybackService", "Range half-split enabled (pair=" + rA + "," + rB + "); itemsPerCycle=" + out.items.size());
        }
        return out;
    }

    // Legacy guard variables removed - now using state machine for concurrency control

    private void enqueueCycles(java.util.List<MediaItem> cycle, int repeat) {
        // Clean implementation - state machine prevents infinite loops at the architecture level
        
        // For very large playlists, avoid duplicating items in memory; loop finite times via listener
        final int LARGE_THRESHOLD = 2000; // items total threshold
        int totalRequested = (repeat <= 0) ? cycle.size() : cycle.size() * repeat;
        
        if (repeat > 0 && totalRequested > LARGE_THRESHOLD) {
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            for (MediaItem mi : cycle) player.addMediaItem(mi);
            
            // Use thread-safe loop control
            safeState.setLoopControl(repeat, cycle.size());
            
            Log.w("PlaybackService", "Enqueued 1 large cycle with finite loop target=" + repeat + ", itemsPerCycle=" + cycle.size());
        } else if (repeat == -1) {
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
        for (String id : ids) {
            String name = idToName.get(id);
            out.add(name != null ? name : id);
        }
        return out;
    }

    private static class SplitBuildResult {
        java.util.List<MediaItem> items = new java.util.ArrayList<>();
        int cachedCount;
        int recommendedRepeat; // -1 for infinite, >=1 otherwise
    }

    private java.util.List<String[]> buildVersesForRange(int ss, int sa, int es, int ea) {
        java.util.List<String[]> verses = new java.util.ArrayList<>();
        for (int s = ss; s <= es; s++) {
            int startAyah = (s == ss) ? sa : 1;
            int endAyah = (s == es) ? ea : getAyahCount(s);
            for (int a = startAyah; a <= endAyah; a++)
                verses.add(new String[]{String.format("%03d", s), String.format("%03d", a)});
        }
        return verses;
    }

    private java.util.List<String[]> buildVersesForSurah(int surah) {
        java.util.List<String[]> verses = new java.util.ArrayList<>();
        String sss = String.format("%03d", surah);
        int N = getAyahCount(surah);
        for (int i = 1; i <= N; i++) verses.add(new String[]{sss, String.format("%03d", i)});
        return verses;
    }

    // Build one split-cycle as list of pairs (a,b) where a reads first half, b reads second half
    private java.util.List<int[]> buildPairsForCycle(int s0, int k) {
        java.util.List<int[]> pairs = new java.util.ArrayList<>();
        int m = (k % 2 == 0) ? (k / 2) : k; // even: K/2 pairs, odd: K pairs with wrap
        for (int j = 0; j < m; j++) {
            int a = (s0 + 2 * j) % k;
            int b = (a + 1) % k;
            pairs.add(new int[]{a, b});
        }
        return pairs;
    }

    private SplitBuildResult buildHalfSplit(java.util.List<String> reciters, java.util.List<String[]> verses, int repeat) {
        SplitBuildResult out = new SplitBuildResult();
        int K = reciters.size();
        if (K < 2) {
            // Fallback: no split possible
            // Build as a single-reciter normal cycle (use first reciter only)
            String rid = reciters.isEmpty() ? "Abdurrahmaan_As-Sudais_64kbps" : reciters.get(0);
            for (String[] va : verses) {
                String sss = va[0]; String aaa = va[1];
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
            }
            out.recommendedRepeat = repeat;
            return out;
        }

        java.util.List<String> reciterNames = getReciterNames(reciters);
        int N = verses.size();
        int split = N / 2;

        // Helper to append one pair block (rA first half, rB second half)
        final java.util.List<String[]> fVerses = verses;
        final int fSplit = split;
        java.util.List<MediaItem> targetItems = out.items;

        // Pair list built via helper method buildPairsForCycle(s0, K)

        // Build according to repeat/infinite and parity rules
        if (repeat == -1) {
            if (K % 2 == 0) {
                // Even K: one split-cycle, repeat ∞
                java.util.List<int[]> pairs = buildPairsForCycle(0, K);
                logPairs("split-cycle (even, ∞)", pairs, reciterNames);
                for (int[] p : pairs) {
                    String rA = reciters.get(p[0]);
                    String rB = reciters.get(p[1]);
                    for (int i = 0; i < N; i++) {
                        String rid = (i < fSplit) ? rA : rB;
                        String sss = fVerses.get(i)[0]; String aaa = fVerses.get(i)[1];
                        String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                        java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                        if (cached.exists()) { targetItems.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                        else { targetItems.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                    }
                }
                out.recommendedRepeat = -1;
            } else {
                // Odd K: build super-cycle of K split-cycles; repeat ∞
                for (int s0 = 0; s0 < K; s0++) {
                    java.util.List<int[]> pairs = buildPairsForCycle(s0, K);
                    logPairs("split-cycle " + (s0 + 1) + "/" + K + " (odd, ∞)", pairs, reciterNames);
                    for (int[] p : pairs) {
                        String rA = reciters.get(p[0]);
                        String rB = reciters.get(p[1]);
                        for (int i = 0; i < N; i++) {
                            String rid = (i < fSplit) ? rA : rB;
                            String sss = fVerses.get(i)[0]; String aaa = fVerses.get(i)[1];
                            String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                            java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                            if (cached.exists()) { targetItems.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                            else { targetItems.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                        }
                    }
                }
                out.recommendedRepeat = -1;
            }
        } else {
            if (K % 2 == 0) {
                // Even K: one split-cycle; repeat N times
                java.util.List<int[]> pairs = buildPairsForCycle(0, K);
                logPairs("split-cycle (even, x" + repeat + ")", pairs, reciterNames);
                for (int[] p : pairs) {
                    String rA = reciters.get(p[0]);
                    String rB = reciters.get(p[1]);
                    for (int i = 0; i < N; i++) {
                        String rid = (i < fSplit) ? rA : rB;
                        String sss = fVerses.get(i)[0]; String aaa = fVerses.get(i)[1];
                        String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                        java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                        if (cached.exists()) { targetItems.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                        else { targetItems.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                    }
                }
                out.recommendedRepeat = Math.max(1, repeat);
            } else {
                // Odd K: flatten repeat cycles with s0 advancing by +1 each cycle
                int reps = Math.max(1, repeat);
                for (int c = 0; c < reps; c++) {
                    int s0 = c % K;
                    java.util.List<int[]> pairs = buildPairsForCycle(s0, K);
                    logPairs("split-cycle " + (c + 1) + "/" + reps + " (odd)", pairs, reciterNames);
                    for (int[] p : pairs) {
                        String rA = reciters.get(p[0]);
                        String rB = reciters.get(p[1]);
                        for (int i = 0; i < N; i++) {
                            String rid = (i < fSplit) ? rA : rB;
                            String sss = fVerses.get(i)[0]; String aaa = fVerses.get(i)[1];
                            String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                            java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                            if (cached.exists()) { targetItems.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                            else { targetItems.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                        }
                    }
                }
                out.recommendedRepeat = 1; // already expanded
            }
        }
        return out;
    }

    private void logPairs(String label, java.util.List<int[]> pairs, java.util.List<String> names) {
        StringBuilder sb = new StringBuilder();
        sb.append("Split pairs ").append(label).append(": ");
        for (int i = 0; i < pairs.size(); i++) {
            int[] p = pairs.get(i);
            sb.append('(').append(names.get(p[0])).append('|').append(names.get(p[1])).append(')');
            if (i < pairs.size() - 1) sb.append(',');
        }
        Log.d("PlaybackService", sb.toString());
    }

    private static final String[] SURAH_NAMES_EN = new String[] {
            "Al-Fatihah", "Al-Baqarah", "Aal Imran", "An-Nisa", "Al-Maidah", "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
            "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Ta-Ha",
            "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan", "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
            "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
            "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
            "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadila", "Al-Hashr", "Al-Mumtahanah",
            "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
            "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "Abasa",
            "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
            "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-Adiyat",
            "Al-Qari'ah", "At-Takathur", "Al-Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
            "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"
    };

    private String surahName(int surah) {
        if (surah >= 1 && surah <= SURAH_NAMES_EN.length) return SURAH_NAMES_EN[surah - 1];
        return "";
    }

    private CycleResult buildPageCycle(int page, boolean halfSplit) {
        PageSegmentDao dao = RepeatQuranDatabase.get(this).pageSegmentDao();
        java.util.List<PageSegmentEntity> segs = dao.segmentsForPage(page);
        java.util.List<String> reciters = getSelectedReciterIds();
        CycleResult out = new CycleResult();
        if (!halfSplit || reciters.size() == 1) {
            java.util.List<String> reciterNames = getReciterNames(reciters);
            StringBuilder orderLog = new StringBuilder("Cycle order (page ").append(page).append("): ");
            for (int i = 0; i < reciters.size(); i++) {
                String rid = reciters.get(i);
                orderLog.append(reciterNames.get(i));
                if (i < reciters.size() - 1) orderLog.append(" -> ");
                for (PageSegmentEntity s : segs) {
                    for (int a = s.startAyah; a <= s.endAyah; a++) {
                        String sss = String.format("%03d", s.surah);
                        String aaa = String.format("%03d", a);
                        String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                        java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                        if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                        else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                        out.totalCount++;
                    }
                }
            }
            Log.d("PlaybackService", orderLog.toString());
            Log.d("PlaybackService", "Page " + page + " itemsPerCycle=" + out.items.size());
        } else {
            java.util.List<String[]> verses = new java.util.ArrayList<>();
            for (PageSegmentEntity s : segs)
                for (int a = s.startAyah; a <= s.endAyah; a++)
                    verses.add(new String[]{String.format("%03d", s.surah), String.format("%03d", a)});
            String rA = reciters.get(0);
            String rB = reciters.size() > 1 ? reciters.get(1) : reciters.get(0);
            int N = verses.size();
            int split = N / 2;
            for (int i = 0; i < N; i++) {
                String rid = (i < split) ? rA : rB;
                String sss = verses.get(i)[0];
                String aaa = verses.get(i)[1];
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                out.totalCount++;
            }
            Log.d("PlaybackService", "Page half-split enabled (pair=" + rA + "," + rB + "); itemsPerCycle=" + out.items.size());
        }
        return out;
    }

    private CycleResult buildSurahCycle(int surah, boolean halfSplit) {
        java.util.List<String> reciters = getSelectedReciterIds();
        CycleResult out = new CycleResult();
        if (!halfSplit || reciters.size() == 1) {
            java.util.List<String> reciterNames = getReciterNames(reciters);
            int maxAyah = getAyahCount(surah);
            StringBuilder orderLog = new StringBuilder("Cycle order (surah ").append(String.format("%03d", surah)).append("): ");
            for (int i = 0; i < reciters.size(); i++) {
                String rid = reciters.get(i);
                orderLog.append(reciterNames.get(i));
                if (i < reciters.size() - 1) orderLog.append(" -> ");
                String sss = String.format("%03d", surah);
                for (int a = 1; a <= maxAyah; a++) {
                    String aaa = String.format("%03d", a);
                    String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                    java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                    if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                    else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                    out.totalCount++;
                }
            }
            Log.d("PlaybackService", orderLog.toString());
            Log.d("PlaybackService", "Surah " + surah + " itemsPerCycle=" + out.items.size());
        } else {
            String sss = String.format("%03d", surah);
            int N = getAyahCount(surah);
            String rA = reciters.get(0);
            String rB = reciters.size() > 1 ? reciters.get(1) : reciters.get(0);
            int split = N / 2;
            for (int i = 0; i < N; i++) {
                String rid = (i < split) ? rA : rB;
                String aaa = String.format("%03d", i + 1);
                String url = "https://everyayah.com/data/" + rid + "/" + sss + aaa + ".mp3";
                java.io.File cached = cacheManager.getTargetFile(rid, sss, aaa);
                if (cached.exists()) { out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached))); out.cachedCount++; }
                else { out.items.add(MediaItem.fromUri(url)); cacheManager.cacheAsync(url, rid, sss, aaa); }
                out.totalCount++;
            }
            Log.d("PlaybackService", "Surah half-split enabled (pair=" + rA + "," + rB + "); itemsPerCycle=" + out.items.size());
        }
        return out;
    }

    

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.setPlayer(null);
        mediaSession.setActive(false);
        mediaSession.release();
        playbackManager.release();
        if (ioExecutor != null) ioExecutor.shutdownNow();
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
            // Create a separate channel for errors with default importance
            NotificationChannel err = new NotificationChannel(
                    "errors",
                    "Playback Errors",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            err.setDescription("Playback error actions");
            manager.createNotificationChannel(err);
        }
    }

    private boolean ensureRecitersSelectedGuard() {
        java.util.List<String> recs = getSelectedReciterIds();
        if (recs == null || recs.isEmpty()) {
            Log.w("PlaybackService", "Play request ignored: no reciters selected");
            if (mainHandler != null) mainHandler.post(() -> android.widget.Toast.makeText(PlaybackService.this, "Select at least one reciter", android.widget.Toast.LENGTH_SHORT).show());
            return false;
        }
        return true;
    }

    private void applyPlaybackSpeed(float speed) {
        try {
            if (player != null) {
                // Keep pitch natural (1.0f)
                PlaybackParameters params = new PlaybackParameters(speed, 1.0f);
                player.setPlaybackParameters(params);
                Log.d("PlaybackService", "Applied playback speed=" + speed);
            }
        } catch (Exception e) {
            Log.e("PlaybackService", "Failed to apply playback speed", e);
        }
    }

    private void broadcastState() {
        try {
            boolean hasQueue = player != null && player.getMediaItemCount() > 0;
            int state = player != null ? player.getPlaybackState() : Player.STATE_IDLE;
            boolean playing = player != null && player.isPlaying();
            boolean active = playing;
            android.content.Intent i = new android.content.Intent(ACTION_PLAYBACK_STATE);
            i.putExtra("hasQueue", hasQueue);
            i.putExtra("state", state);
            i.putExtra("active", active);
            i.putExtra("playing", playing);
            sendBroadcast(i);
        } catch (Exception ignored) {}
    }

    private void showErrorNotification(String title, String text) {
        Intent retry = new Intent(this, PlaybackService.class);
        retry.setAction(ACTION_RETRY_ITEM);
        PendingIntent piRetry = PendingIntent.getService(this, 3001, retry, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Intent skip = new Intent(this, PlaybackService.class);
        skip.setAction(ACTION_SKIP_ITEM);
        PendingIntent piSkip = PendingIntent.getService(this, 3002, skip, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, Build.VERSION.SDK_INT >= 26 ? "errors" : CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .addAction(new NotificationCompat.Action(0, "Retry", piRetry))
                .addAction(new NotificationCompat.Action(0, "Skip", piSkip));
        NotificationManagerCompat.from(this).notify(ERROR_NOTIFICATION_ID, b.build());
    }

    private void cancelErrorNotification() {
        NotificationManagerCompat.from(this).cancel(ERROR_NOTIFICATION_ID);
    }



    private void onResumeRequested() {
        boolean hasQueue = player != null && player.getMediaItemCount() > 0 && player.getPlaybackState() != Player.STATE_ENDED;
        boolean isPlaying = player != null && player.isPlaying();
        
        if (hasQueue && !isPlaying) {
            // Simple resume: keep current queue and continue
            player.play();
            broadcastState();
            return;
        }
        
        // Get resume state using thread-safe manager
        ThreadSafePlaybackState.ResumeState resumeState = safeState.getResumeState();
        
        if (resumeState.sourceType == null) {
            android.widget.Toast.makeText(this, "No recent session to resume", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        java.util.List<String> reciters = new java.util.ArrayList<>();
        if (resumeState.recitersCsv != null && !resumeState.recitersCsv.isEmpty()) {
            for (String s : resumeState.recitersCsv.split(",")) {
                if (!s.isEmpty()) reciters.add(s);
            }
        }
        
        if (reciters.isEmpty()) {
            android.widget.Toast.makeText(this, "Select at least one reciter", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        player.stop();
        player.clearMediaItems();
        playbackManager.setFeedingEnabled(false);
        
        // Use saved reciters for fidelity (thread-safe)
        safeState.setRecitersOverride(reciters);
        
        try {
            if ("single".equals(resumeState.sourceType)) {
                resumeSingle(resumeState);
            } else if ("range".equals(resumeState.sourceType)) {
                resumeRange(resumeState);
            } else if ("page".equals(resumeState.sourceType)) {
                resumePage(resumeState);
                return; // Async operation, return early
            } else if ("surah".equals(resumeState.sourceType)) {
                resumeSurah(resumeState);
            } else {
                android.widget.Toast.makeText(this, "Nothing to resume", android.widget.Toast.LENGTH_SHORT).show();
                safeState.clearRecitersOverride();
                return;
            }
            
            // Clear override and start playback
            safeState.clearRecitersOverride();
            player.prepare();
            if (resumeState.mediaIndex >= 0) {
                player.seekTo(resumeState.mediaIndex, Math.max(0, resumeState.positionMs));
            }
            player.play();
            broadcastState();
            
        } catch (Exception e) {
            Log.e("PlaybackService", "Resume failed", e);
            safeState.clearRecitersOverride();
            android.widget.Toast.makeText(this, "Resume failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void resumeSingle(ThreadSafePlaybackState.ResumeState state) {
        CycleResult cycle = buildSingleAyahCycle(
            String.format("%03d", state.startSurah), 
            String.format("%03d", state.startAyah)
        );
        
        if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
            android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
            throw new RuntimeException("No cached audio available offline");
        }
        
        enqueueCycles(cycle.items, state.repeat);
    }
    
    private void resumeRange(ThreadSafePlaybackState.ResumeState state) {
        if (state.halfSplit && getSelectedReciterIds().size() >= 2) {
            java.util.List<String[]> verses = buildVersesForRange(state.startSurah, state.startAyah, state.endSurah, state.endAyah);
            SplitBuildResult sbr = buildHalfSplit(getSelectedReciterIds(), verses, state.repeat);
            
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && sbr.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                throw new RuntimeException("No cached audio available offline");
            }
            
            enqueueCycles(sbr.items, sbr.recommendedRepeat);
        } else {
            CycleResult cycle = buildRangeCycle(state.startSurah, state.startAyah, state.endSurah, state.endAyah, state.halfSplit);
            
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                throw new RuntimeException("No cached audio available offline");
            }
            
            enqueueCycles(cycle.items, state.repeat);
        }
    }
    
    private void resumeSurah(ThreadSafePlaybackState.ResumeState state) {
        if (state.halfSplit && getSelectedReciterIds().size() >= 2) {
            java.util.List<String[]> verses = buildVersesForSurah(state.startSurah);
            SplitBuildResult sbr = buildHalfSplit(getSelectedReciterIds(), verses, state.repeat);
            
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && sbr.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                throw new RuntimeException("No cached audio available offline");
            }
            
            enqueueCycles(sbr.items, sbr.recommendedRepeat);
        } else {
            CycleResult cycle = buildSurahCycle(state.startSurah, state.halfSplit);
            
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                throw new RuntimeException("No cached audio available offline");
            }
            
            enqueueCycles(cycle.items, state.repeat);
        }
    }
    
    private void resumePage(ThreadSafePlaybackState.ResumeState state) {
        // Page resume requires database access, so run on background thread
        ioExecutor.execute(() -> {
            try {
                if (state.halfSplit && getSelectedReciterIds().size() >= 2) {
                    PageSegmentDao dao = RepeatQuranDatabase.get(this).pageSegmentDao();
                    java.util.List<PageSegmentEntity> segs = dao.segmentsForPage(state.page);
                    java.util.List<String[]> verses = new java.util.ArrayList<>();
                    
                    for (PageSegmentEntity s : segs) {
                        for (int a = s.startAyah; a <= s.endAyah; a++) {
                            verses.add(new String[]{String.format("%03d", s.surah), String.format("%03d", a)});
                        }
                    }
                    
                    SplitBuildResult sbr = buildHalfSplit(getSelectedReciterIds(), verses, state.repeat);
                    
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && sbr.cachedCount == 0) {
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                            safeState.clearRecitersOverride();
                        });
                        return;
                    }
                    
                    mainHandler.post(() -> {
                        enqueueCycles(sbr.items, sbr.recommendedRepeat);
                        safeState.clearRecitersOverride();
                        player.prepare();
                        if (state.mediaIndex >= 0) {
                            player.seekTo(state.mediaIndex, Math.max(0, state.positionMs));
                        }
                        player.play();
                        broadcastState();
                    });
                } else {
                    CycleResult cycle = buildPageCycle(state.page, state.halfSplit);
                    
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                            safeState.clearRecitersOverride();
                        });
                        return;
                    }
                    
                    mainHandler.post(() -> {
                        enqueueCycles(cycle.items, state.repeat);
                        safeState.clearRecitersOverride();
                        player.prepare();
                        if (state.mediaIndex >= 0) {
                            player.seekTo(state.mediaIndex, Math.max(0, state.positionMs));
                        }
                        player.play();
                        broadcastState();
                    });
                }
            } catch (Exception e) {
                Log.e("PlaybackService", "Page resume failed", e);
                mainHandler.post(() -> {
                    safeState.clearRecitersOverride();
                    android.widget.Toast.makeText(this, "Page resume failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    // ===== NEW CLEAN ACTION IMPLEMENTATIONS =====
    
    /**
     * NEW: Clean implementation of LOAD_SINGLE action using state machine pattern
     */
    private int handleLoadSingle(Intent intent) {
        int sura = intent.getIntExtra("sura", 1);
        int ayah = intent.getIntExtra("ayah", 1);
        int repeat = intent.getIntExtra("repeat",
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
        
        Log.d("PlaybackService", "NEW: Load Single: Surah " + String.format("%03d", sura) + " — " + surahName(sura) + ", Ayah " + ayah + ", Repeat=" + (repeat==-1?"∞":repeat));
        
        // Step 1: Quick validation (on main thread)
        if (!ensureRecitersSelectedGuard()) {
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 2: Check if we can resume existing playback (optimization)
        if (canResumeExistingPlayback("single", sura, ayah, null, null, repeat)) {
            Log.d("PlaybackService", "NEW: Resuming existing single ayah playback");
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0, 0);
                player.prepare();
            }
            player.play();
            broadcastState();
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 3: State transition to data preparation
        transitionTo(PlaybackServiceState.PREPARING_DATA);
        
        // Step 4: Prepare data on background thread
        ioExecutor.execute(() -> {
            try {
                // Build the cycle
                String sss = String.format("%03d", sura);
                String aaa = String.format("%03d", ayah);
                CycleResult cycle = buildSingleAyahCycle(sss, aaa);
                
                // Check connectivity
                if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                    Log.w("PlaybackService", "NEW: Offline with no cached audio for single ayah");
                    mainHandler.post(() -> {
                        android.widget.Toast.makeText(this, "Offline: no cached audio for this ayah", android.widget.Toast.LENGTH_LONG).show();
                        resetToIdle();
                    });
                    return;
                }
                
                // Step 5: Execute playback on main thread
                mainHandler.post(() -> {
                    try {
                        transitionTo(PlaybackServiceState.EXECUTING_PLAYBACK);
                        
                        // Capture state for resume
                        safeState.captureSelectionForResume("single", null, sura, ayah, null, null, repeat);
                        
                        // Analytics
                        {
                            java.util.Map<String, Object> ev = new java.util.HashMap<>();
                            ev.put("type", "single"); 
                            ev.put("surah", sura); 
                            ev.put("ayah", ayah); 
                            ev.put("repeat", repeat);
                            com.repeatquran.analytics.AnalyticsLogger.get(this).log("play_request", ev);
                        }
                        
                        // Setup player
                        player.stop();
                        player.clearMediaItems();
                        playbackManager.setFeedingEnabled(false);
                        
                        // Enqueue and start
                        enqueueCycles(cycle.items, repeat);
                        player.prepare();
                        player.play();
                        broadcastState();
                        
                        currentCyclesRequested = repeat;
                        
                        // Success - back to idle
                        resetToIdle();
                        
                    } catch (Exception e) {
                        Log.e("PlaybackService", "NEW: Failed to execute single ayah playback", e);
                        resetToIdle();
                    }
                });
                
                // Step 6: Save session on background thread
                SessionEntity e = new SessionEntity();
                e.startedAt = System.currentTimeMillis();
                e.sourceType = "single";
                e.startSurah = sura;
                e.startAyah = ayah;
                e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                e.repeatCount = repeat;
                e.cyclesRequested = repeat;
                currentSessionId = sessionRepo.insert(e);
                
            } catch (Exception e) {
                Log.e("PlaybackService", "NEW: Failed to prepare single ayah data", e);
                mainHandler.post(() -> {
                    android.widget.Toast.makeText(this, "Failed to load ayah: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    resetToIdle();
                });
            }
        });
        
        return START_STICKY;
    }
    
    /**
     * Helper method to check if we can resume existing playback instead of rebuilding
     */
    private boolean canResumeExistingPlayback(String sourceType, Integer sura, Integer ayah, Integer endSura, Integer endAyah, int repeat) {
        if (player.getMediaItemCount() == 0) {
            return false;
        }
        
        ThreadSafePlaybackState.ResumeState resumeState = safeState.getResumeState();
        String currentReciters = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        
        boolean sameSelection = sourceType.equals(resumeState.sourceType);
        if ("single".equals(sourceType)) {
            sameSelection = sameSelection && resumeState.startSurah == sura && resumeState.startAyah == ayah;
        } else if ("range".equals(sourceType)) {
            sameSelection = sameSelection && resumeState.startSurah == sura && resumeState.startAyah == ayah 
                         && resumeState.endSurah == endSura && resumeState.endAyah == endAyah;
        }
        
        boolean sameReciters = (resumeState.recitersCsv == null
                ? (currentReciters == null || currentReciters.isEmpty())
                : resumeState.recitersCsv.equals(currentReciters));
        boolean sameRepeat = (resumeState.repeat == repeat);
        
        return sameSelection && sameReciters && sameRepeat;
    }
    
    /**
     * NEW: Clean implementation of LOAD_PAGE action using state machine pattern
     */
    private int handleLoadPage(Intent intent) {
        int page = intent.getIntExtra("page", -1);
        int repeat = intent.getIntExtra("repeat",
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
        boolean halfSplit = intent.getBooleanExtra("halfSplit", 
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("ui.half.split", false));
        
        Log.d("PlaybackService", "NEW: Load Page: " + page + ", Repeat=" + (repeat==-1?"∞":repeat) + ", HalfSplit=" + halfSplit);
        
        // Step 1: Quick validation (on main thread)
        if (page < 1 || page > 604) {
            Log.e("PlaybackService", "NEW: Invalid page: " + page);
            resetToIdle();
            return START_STICKY;
        }
        
        if (!ensureRecitersSelectedGuard()) {
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 2: Check if we can resume existing playback (optimization)
        if (canResumeExistingPagePlayback(page, halfSplit, repeat)) {
            Log.d("PlaybackService", "NEW: Resuming existing page playback");
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0, 0);
                player.prepare();
            }
            player.play();
            broadcastState();
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 3: State transition to data preparation
        transitionTo(PlaybackServiceState.PREPARING_DATA);
        
        // Step 4: Prepare data on background thread (including database access)
        ioExecutor.execute(() -> {
            try {
                java.util.List<String> reciters = getSelectedReciterIds();
                
                if (halfSplit && reciters.size() >= 2) {
                    // Build split version using database
                    PageSegmentDao dao = RepeatQuranDatabase.get(this).pageSegmentDao();
                    java.util.List<PageSegmentEntity> segs = dao.segmentsForPage(page);
                    java.util.List<String[]> verses = new java.util.ArrayList<>();
                    
                    for (PageSegmentEntity s : segs) {
                        for (int a = s.startAyah; a <= s.endAyah; a++) {
                            verses.add(new String[]{String.format("%03d", s.surah), String.format("%03d", a)});
                        }
                    }
                    
                    SplitBuildResult sbr = buildHalfSplit(reciters, verses, repeat);
                    
                    // Check connectivity
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && sbr.cachedCount == 0) {
                        Log.w("PlaybackService", "NEW: Offline with no cached audio for page (split)");
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio for this page", android.widget.Toast.LENGTH_LONG).show();
                            resetToIdle();
                        });
                        return;
                    }
                    
                    // Execute playback on main thread
                    mainHandler.post(() -> {
                        executePagePlayback(page, repeat, sbr.items, sbr.recommendedRepeat);
                    });
                    
                } else {
                    // Build regular version
                    CycleResult cycle = buildPageCycle(page, halfSplit);
                    
                    // Check connectivity
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                        Log.w("PlaybackService", "NEW: Offline with no cached audio for page");
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio for this page", android.widget.Toast.LENGTH_LONG).show();
                            resetToIdle();
                        });
                        return;
                    }
                    
                    // Execute playback on main thread
                    mainHandler.post(() -> {
                        executePagePlayback(page, repeat, cycle.items, repeat);
                    });
                }
                
                // Step 6: Save session on background thread
                SessionEntity e = new SessionEntity();
                e.startedAt = System.currentTimeMillis();
                e.sourceType = "page";
                e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                e.repeatCount = repeat;
                e.cyclesRequested = repeat;
                currentSessionId = sessionRepo.insert(e);
                
            } catch (Exception e) {
                Log.e("PlaybackService", "NEW: Failed to prepare page data", e);
                mainHandler.post(() -> {
                    android.widget.Toast.makeText(this, "Failed to load page: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    resetToIdle();
                });
            }
        });
        
        return START_STICKY;
    }
    
    /**
     * Helper method to execute page playback on main thread - shared between split/regular
     */
    private void executePagePlayback(int page, int originalRepeat, java.util.List<MediaItem> items, int actualRepeat) {
        try {
            transitionTo(PlaybackServiceState.EXECUTING_PLAYBACK);
            
            // Capture state for resume
            boolean halfSplit = getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("ui.half.split", false);
            safeState.captureSelectionForResume("page", page, null, null, null, null, originalRepeat);
            
            // Analytics
            {
                java.util.Map<String, Object> ev = new java.util.HashMap<>();
                ev.put("type", "page"); 
                ev.put("page", page); 
                ev.put("repeat", originalRepeat);
                com.repeatquran.analytics.AnalyticsLogger.get(this).log("play_request", ev);
            }
            
            // Setup player
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            
            // Enqueue and start
            enqueueCycles(items, actualRepeat);
            player.prepare();
            player.play();
            broadcastState();
            
            currentCyclesRequested = originalRepeat;
            
            // Success - back to idle
            resetToIdle();
            
        } catch (Exception e) {
            Log.e("PlaybackService", "NEW: Failed to execute page playback", e);
            resetToIdle();
        }
    }
    
    /**
     * Helper method to check if we can resume existing page playback
     */
    private boolean canResumeExistingPagePlayback(int page, boolean halfSplit, int repeat) {
        if (player.getMediaItemCount() == 0) {
            return false;
        }
        
        ThreadSafePlaybackState.ResumeState resumeState = safeState.getResumeState();
        String currentReciters = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        
        boolean sameSelection = "page".equals(resumeState.sourceType) && resumeState.page >= 1 && resumeState.page == page;
        boolean sameReciters = (resumeState.recitersCsv == null
                ? (currentReciters == null || currentReciters.isEmpty())
                : resumeState.recitersCsv.equals(currentReciters));
        boolean sameRepeat = (resumeState.repeat == repeat);
        boolean sameHalfSplit = (resumeState.halfSplit == halfSplit);
        
        return sameSelection && sameReciters && sameRepeat && sameHalfSplit;
    }
    
    /**
     * NEW: Clean implementation of LOAD_SURAH action using state machine pattern
     */
    private int handleLoadSurah(Intent intent) {
        int surah = intent.getIntExtra("surah", -1);
        int repeat = intent.getIntExtra("repeat",
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
        boolean halfSplit = intent.getBooleanExtra("halfSplit", 
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("ui.half.split", false));
        
        Log.d("PlaybackService", "NEW: Load Surah: " + String.format("%03d", surah) + " — " + surahName(surah) + ", Repeat=" + (repeat==-1?"∞":repeat) + ", HalfSplit=" + halfSplit);
        
        // Step 1: Quick validation (on main thread)
        if (surah < 1 || surah > 114) {
            Log.e("PlaybackService", "NEW: Invalid surah: " + surah);
            resetToIdle();
            return START_STICKY;
        }
        
        if (!ensureRecitersSelectedGuard()) {
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 2: Check if we can resume existing playback (optimization)
        if (canResumeExistingSurahPlayback(surah, halfSplit, repeat)) {
            Log.d("PlaybackService", "NEW: Resuming existing surah playback");
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0, 0);
                player.prepare();
            }
            player.play();
            broadcastState();
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 3: State transition to data preparation
        transitionTo(PlaybackServiceState.PREPARING_DATA);
        
        // Step 4: Prepare data on background thread
        ioExecutor.execute(() -> {
            try {
                java.util.List<String> reciters = getSelectedReciterIds();
                
                if (halfSplit && reciters.size() >= 2) {
                    // Build split version
                    java.util.List<String[]> verses = buildVersesForSurah(surah);
                    SplitBuildResult sbr = buildHalfSplit(reciters, verses, repeat);
                    
                    // Check connectivity
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && sbr.cachedCount == 0) {
                        Log.w("PlaybackService", "NEW: Offline with no cached audio for surah (split)");
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio for this surah", android.widget.Toast.LENGTH_LONG).show();
                            resetToIdle();
                        });
                        return;
                    }
                    
                    // Execute playback on main thread
                    mainHandler.post(() -> {
                        executeSurahPlayback(surah, repeat, sbr.items, sbr.recommendedRepeat);
                    });
                    
                } else {
                    // Build regular version
                    CycleResult cycle = buildSurahCycle(surah, halfSplit);
                    
                    // Check connectivity
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                        Log.w("PlaybackService", "NEW: Offline with no cached audio for surah");
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio for this surah", android.widget.Toast.LENGTH_LONG).show();
                            resetToIdle();
                        });
                        return;
                    }
                    
                    // Execute playback on main thread
                    mainHandler.post(() -> {
                        executeSurahPlayback(surah, repeat, cycle.items, repeat);
                    });
                }
                
                // Step 6: Save session on background thread
                SessionEntity e = new SessionEntity();
                e.startedAt = System.currentTimeMillis();
                e.sourceType = "surah";
                e.startSurah = surah;
                e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                e.repeatCount = repeat;
                e.cyclesRequested = repeat;
                currentSessionId = sessionRepo.insert(e);
                
            } catch (Exception e) {
                Log.e("PlaybackService", "NEW: Failed to prepare surah data", e);
                mainHandler.post(() -> {
                    android.widget.Toast.makeText(this, "Failed to load surah: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    resetToIdle();
                });
            }
        });
        
        return START_STICKY;
    }
    
    /**
     * Helper method to execute surah playback on main thread - shared between split/regular
     */
    private void executeSurahPlayback(int surah, int originalRepeat, java.util.List<MediaItem> items, int actualRepeat) {
        try {
            transitionTo(PlaybackServiceState.EXECUTING_PLAYBACK);
            
            // Capture state for resume
            safeState.captureSelectionForResume("surah", null, surah, null, null, null, originalRepeat);
            
            // Analytics
            {
                java.util.Map<String, Object> ev = new java.util.HashMap<>();
                ev.put("type", "surah"); 
                ev.put("surah", surah); 
                ev.put("repeat", originalRepeat);
                com.repeatquran.analytics.AnalyticsLogger.get(this).log("play_request", ev);
            }
            
            // Setup player
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            
            // Enqueue and start
            enqueueCycles(items, actualRepeat);
            player.prepare();
            player.play();
            broadcastState();
            
            currentCyclesRequested = originalRepeat;
            
            // Success - back to idle
            resetToIdle();
            
        } catch (Exception e) {
            Log.e("PlaybackService", "NEW: Failed to execute surah playback", e);
            resetToIdle();
        }
    }
    
    /**
     * Helper method to check if we can resume existing surah playback
     */
    private boolean canResumeExistingSurahPlayback(int surah, boolean halfSplit, int repeat) {
        if (player.getMediaItemCount() == 0) {
            return false;
        }
        
        ThreadSafePlaybackState.ResumeState resumeState = safeState.getResumeState();
        String currentReciters = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        
        boolean sameSelection = "surah".equals(resumeState.sourceType) && resumeState.startSurah >= 1 && resumeState.startSurah == surah;
        boolean sameReciters = (resumeState.recitersCsv == null
                ? (currentReciters == null || currentReciters.isEmpty())
                : resumeState.recitersCsv.equals(currentReciters));
        boolean sameRepeat = (resumeState.repeat == repeat);
        boolean sameHalfSplit = (resumeState.halfSplit == halfSplit);
        
        return sameSelection && sameReciters && sameRepeat && sameHalfSplit;
    }
    
    /**
     * NEW: Clean implementation of LOAD_RANGE action using state machine pattern
     */
    private int handleLoadRange(Intent intent) {
        int ss = intent.getIntExtra("ss", 1);
        int sa = intent.getIntExtra("sa", 1);
        int es = intent.getIntExtra("es", 1);
        int ea = intent.getIntExtra("ea", 1);
        int repeat = intent.getIntExtra("repeat",
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
        boolean halfSplit = intent.getBooleanExtra("halfSplit", 
                getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("ui.half.split", false));
        
        Log.d("PlaybackService", "NEW: Load Range: " + String.format("%03d", ss) + " — " + surahName(ss) + ":" + sa +
                " → " + String.format("%03d", es) + " — " + surahName(es) + ":" + ea + ", Repeat=" + (repeat==-1?"∞":repeat) + ", HalfSplit=" + halfSplit);
        
        // Step 1: Quick validation (on main thread)
        if (ss < 1 || ss > 114 || es < 1 || es > 114 || ss > es) {
            Log.e("PlaybackService", "NEW: Invalid range: " + ss + ":" + sa + " -> " + es + ":" + ea);
            resetToIdle();
            return START_STICKY;
        }
        
        if (!ensureRecitersSelectedGuard()) {
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 2: Check if we can resume existing playback (optimization)
        if (canResumeExistingRangePlayback(ss, sa, es, ea, halfSplit, repeat)) {
            Log.d("PlaybackService", "NEW: Resuming existing range playback");
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0, 0);
                player.prepare();
            }
            player.play();
            broadcastState();
            resetToIdle();
            return START_STICKY;
        }
        
        // Step 3: State transition to data preparation
        transitionTo(PlaybackServiceState.PREPARING_DATA);
        
        // Step 4: Prepare data on background thread
        ioExecutor.execute(() -> {
            try {
                java.util.List<String> reciters = getSelectedReciterIds();
                
                if (halfSplit && reciters.size() >= 2) {
                    // Build split version
                    java.util.List<String[]> verses = new java.util.ArrayList<>();
                    for (int s = ss; s <= es; s++) {
                        int startAyah = (s == ss) ? sa : 1;
                        int endAyah = (s == es) ? ea : getAyahCount(s);
                        for (int a = startAyah; a <= endAyah; a++) {
                            verses.add(new String[]{String.format("%03d", s), String.format("%03d", a)});
                        }
                    }
                    SplitBuildResult sbr = buildHalfSplit(reciters, verses, repeat);
                    
                    // Check connectivity
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && sbr.cachedCount == 0) {
                        Log.w("PlaybackService", "NEW: Offline with no cached audio for range (split)");
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio for this range", android.widget.Toast.LENGTH_LONG).show();
                            resetToIdle();
                        });
                        return;
                    }
                    
                    // Execute playback on main thread
                    mainHandler.post(() -> {
                        executeRangePlayback(ss, sa, es, ea, repeat, sbr.items, sbr.recommendedRepeat);
                    });
                    
                } else {
                    // Build regular version
                    CycleResult cycle = buildRangeCycle(ss, sa, es, ea, halfSplit);
                    
                    // Check connectivity
                    if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                        Log.w("PlaybackService", "NEW: Offline with no cached audio for range");
                        mainHandler.post(() -> {
                            android.widget.Toast.makeText(this, "Offline: no cached audio for this range", android.widget.Toast.LENGTH_LONG).show();
                            resetToIdle();
                        });
                        return;
                    }
                    
                    // Execute playback on main thread
                    mainHandler.post(() -> {
                        executeRangePlayback(ss, sa, es, ea, repeat, cycle.items, repeat);
                    });
                }
                
                // Step 6: Save session on background thread
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
                currentSessionId = sessionRepo.insert(e);
                
            } catch (Exception e) {
                Log.e("PlaybackService", "NEW: Failed to prepare range data", e);
                mainHandler.post(() -> {
                    android.widget.Toast.makeText(this, "Failed to load range: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    resetToIdle();
                });
            }
        });
        
        return START_STICKY;
    }
    
    /**
     * Helper method to execute range playback on main thread - shared between split/regular
     */
    private void executeRangePlayback(int ss, int sa, int es, int ea, int originalRepeat, java.util.List<MediaItem> items, int actualRepeat) {
        try {
            transitionTo(PlaybackServiceState.EXECUTING_PLAYBACK);
            
            // Capture state for resume
            safeState.captureSelectionForResume("range", null, ss, sa, es, ea, originalRepeat);
            
            // Analytics
            {
                java.util.Map<String, Object> ev = new java.util.HashMap<>();
                ev.put("type", "range"); 
                ev.put("ss", ss); 
                ev.put("sa", sa); 
                ev.put("es", es); 
                ev.put("ea", ea); 
                ev.put("repeat", originalRepeat);
                com.repeatquran.analytics.AnalyticsLogger.get(this).log("play_request", ev);
            }
            
            // Setup player
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            
            // Enqueue and start
            enqueueCycles(items, actualRepeat);
            player.prepare();
            player.play();
            broadcastState();
            
            currentCyclesRequested = originalRepeat;
            
            // Success - back to idle
            resetToIdle();
            
        } catch (Exception e) {
            Log.e("PlaybackService", "NEW: Failed to execute range playback", e);
            resetToIdle();
        }
    }
    
    /**
     * Helper method to check if we can resume existing range playback
     */
    private boolean canResumeExistingRangePlayback(int ss, int sa, int es, int ea, boolean halfSplit, int repeat) {
        if (player.getMediaItemCount() == 0) {
            return false;
        }
        
        ThreadSafePlaybackState.ResumeState resumeState = safeState.getResumeState();
        String currentReciters = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        
        boolean sameSelection = "range".equals(resumeState.sourceType) 
                && resumeState.startSurah >= 1 && resumeState.startSurah == ss
                && resumeState.startAyah >= 1 && resumeState.startAyah == sa
                && resumeState.endSurah >= 1 && resumeState.endSurah == es
                && resumeState.endAyah >= 1 && resumeState.endAyah == ea;
        boolean sameReciters = (resumeState.recitersCsv == null
                ? (currentReciters == null || currentReciters.isEmpty())
                : resumeState.recitersCsv.equals(currentReciters));
        boolean sameRepeat = (resumeState.repeat == repeat);
        boolean sameHalfSplit = (resumeState.halfSplit == halfSplit);
        
        return sameSelection && sameReciters && sameRepeat && sameHalfSplit;
    }
    
    // ===== STATE MACHINE HELPERS =====
    
    /**
     * Check if we can accept a new action (only in IDLE state)
     */
    private boolean canAcceptAction(String actionName) {
        if (currentState != PlaybackServiceState.IDLE) {
            Log.w("PlaybackService", "Ignoring action " + actionName + " - not in IDLE state: " + currentState);
            return false;
        }
        return true;
    }
    
    /**
     * Transition to a new state with logging
     */
    private void transitionTo(PlaybackServiceState newState) {
        PlaybackServiceState oldState = currentState;
        currentState = newState;
        Log.d("PlaybackService", "State transition: " + oldState + " -> " + newState);
    }
    
    /**
     * Reset to IDLE state, typically after an action completes or fails
     */
    private void resetToIdle() {
        transitionTo(PlaybackServiceState.IDLE);
    }
}
