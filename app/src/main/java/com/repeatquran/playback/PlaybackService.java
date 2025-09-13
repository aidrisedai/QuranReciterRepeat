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
    public static final String ACTION_NEXT = "com.repeatquran.action.NEXT";
    public static final String ACTION_PREV = "com.repeatquran.action.PREV";
    public static final String ACTION_LOAD_SINGLE = "com.repeatquran.action.LOAD_SINGLE";
    public static final String ACTION_LOAD_RANGE = "com.repeatquran.action.LOAD_RANGE";
    public static final String ACTION_LOAD_PAGE = "com.repeatquran.action.LOAD_PAGE";
    public static final String ACTION_LOAD_SURAH = "com.repeatquran.action.LOAD_SURAH";
    public static final String ACTION_RESUME = "com.repeatquran.action.RESUME";
    public static final String ACTION_PLAYBACK_STATE = "com.repeatquran.action.PLAYBACK_STATE";

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

    // Resume state tracking
    private String lastSourceType = null; // single | range | page | surah
    private Integer lastPage = null;
    private Integer lastStartSurah = null;
    private Integer lastStartAyah = null;
    private Integer lastEndSurah = null;
    private Integer lastEndAyah = null;
    private Integer lastRepeat = null; // -1 for infinite
    private String lastRecitersCsv = null; // saved selection for fidelity
    private java.util.List<String> recitersOverride = null; // used during resume build
    private final Runnable resumeSaver = new Runnable() {
        @Override public void run() {
            try { saveResumeStatePeriodic(); } catch (Exception ignored) {}
            // Re-post every 2s
            if (mainHandler != null) mainHandler.postDelayed(this, 2000);
        }
    };

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
            public void onPlayerError(@NonNull PlaybackException error) {
                boolean online = com.repeatquran.util.NetworkUtil.isOnline(PlaybackService.this);
                MediaItem mi = player.getCurrentMediaItem();
                String uriStr = mi != null && mi.playbackProperties != null && mi.playbackProperties.uri != null ? mi.playbackProperties.uri.toString() : "";
                if (!online) {
                    Log.w("PlaybackService", "Offline playback error; skipping item: " + uriStr);
                    mainHandler.post(() -> android.widget.Toast.makeText(PlaybackService.this, "Offline: skipping uncached item", android.widget.Toast.LENGTH_SHORT).show());
                    // Skip to next item when offline and current failed
                    mainHandler.post(() -> {
                        if (player.hasNextMediaItem()) player.seekToNextMediaItem(); else player.stop();
                    });
                } else {
                    Log.e("PlaybackService", "Playback error online: " + error.getMessage());
                    // Move forward to avoid stalls
                    mainHandler.post(() -> {
                        if (player.hasNextMediaItem()) player.seekToNextMediaItem(); else player.stop();
                    });
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

        // Start periodic saver
        mainHandler.postDelayed(resumeSaver, 2000);
        broadcastState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            broadcastState();
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME.equals(action)) {
            int state = player.getPlaybackState();
            if (player.getMediaItemCount() > 0 && state != Player.STATE_ENDED) {
                // Already have an active queue; avoid disruptive jump backwards
                mainHandler.post(() -> android.widget.Toast.makeText(this, "Already playing — Resume not needed", android.widget.Toast.LENGTH_SHORT).show());
                // Refresh saved position to current for future resumes
                saveResumeStateNow();
                return START_STICKY;
            }
            onResumeRequested();
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
            // No auto-seeding on start; only resume if something is already queued
            if (player.getMediaItemCount() > 0) {
                player.play();
            }
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
        } else if (ACTION_LOAD_SINGLE.equals(action)) {
            int sura = intent.getIntExtra("sura", 1);
            int ayah = intent.getIntExtra("ayah", 1);
            // Build exact playlist based on repeat setting
            String sss = String.format("%03d", sura);
            String aaa = String.format("%03d", ayah);
            int repeat = intent.getIntExtra("repeat",
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
            Log.d("PlaybackService", "Load Single: Surah " + sss + " — " + surahName(sura) + ", Ayah " + ayah + ", Repeat=" + (repeat==-1?"∞":repeat));
            captureSelectionForResume("single", null, sura, ayah, null, null, repeat);
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false); // prevent provider from appending more items
            CycleResult cycle = buildSingleAyahCycle(sss, aaa);
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                Log.w("PlaybackService", "Offline with no cached audio for selection (single)");
                mainHandler.post(() -> android.widget.Toast.makeText(this, "Offline: no cached audio for this ayah", android.widget.Toast.LENGTH_LONG).show());
                return START_STICKY;
            }
            mainHandler.post(() -> {
                enqueueCycles(cycle.items, repeat);
                player.prepare();
                player.play();
                broadcastState();
            });
            currentCyclesRequested = repeat;
            ioExecutor.execute(() -> {
                SessionEntity e = new SessionEntity();
                e.startedAt = System.currentTimeMillis();
                e.sourceType = "single";
                e.startSurah = sura;
                e.startAyah = ayah;
                e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                e.repeatCount = repeat;
                e.cyclesRequested = repeat;
                currentSessionId = sessionRepo.insert(e);
            });
        } else if (ACTION_LOAD_RANGE.equals(action)) {
            int ss = intent.getIntExtra("ss", 1);
            int sa = intent.getIntExtra("sa", 1);
            int es = intent.getIntExtra("es", 1);
            int ea = intent.getIntExtra("ea", 1);
            int repeat = intent.getIntExtra("repeat",
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
            Log.d("PlaybackService", "Load Range: " + String.format("%03d", ss) + " — " + surahName(ss) + ":" + sa +
                    " → " + String.format("%03d", es) + " — " + surahName(es) + ":" + ea + ", Repeat=" + (repeat==-1?"∞":repeat));
            captureSelectionForResume("range", null, ss, sa, es, ea, repeat);

            // Build URLs for the inclusive range (ss:sa) -> (es:ea)
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            CycleResult cycle = buildRangeCycle(ss, sa, es, ea);
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                Log.w("PlaybackService", "Offline with no cached audio for selection (range)");
                mainHandler.post(() -> android.widget.Toast.makeText(this, "Offline: no cached audio for this range", android.widget.Toast.LENGTH_LONG).show());
                return START_STICKY;
            }
            mainHandler.post(() -> {
                enqueueCycles(cycle.items, repeat);
                player.prepare();
                player.play();
                broadcastState();
            });
            currentCyclesRequested = repeat;
            ioExecutor.execute(() -> {
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
            });
        } else if (ACTION_LOAD_PAGE.equals(action)) {
            int page = intent.getIntExtra("page", -1);
            int repeat = intent.getIntExtra("repeat",
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
            Log.d("PlaybackService", "Load Page: " + page + ", Repeat=" + (repeat==-1?"∞":repeat));
            if (page < 1 || page > 604) {
                Log.e("PlaybackService", "Invalid page: " + page);
                return START_STICKY;
            }
            captureSelectionForResume("page", page, null, null, null, null, repeat);
            // Query and build cycle off main, then enqueue on main; insert session off main
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            ioExecutor.execute(() -> {
                CycleResult cycle = buildPageCycle(page);
                if (!com.repeatquran.util.NetworkUtil.isOnline(PlaybackService.this) && cycle.cachedCount == 0) {
                    Log.w("PlaybackService", "Offline with no cached audio for selection (page)");
                    mainHandler.post(() -> android.widget.Toast.makeText(PlaybackService.this, "Offline: no cached audio for this page", android.widget.Toast.LENGTH_LONG).show());
                    return;
                }
                mainHandler.post(() -> {
                    enqueueCycles(cycle.items, repeat);
                    player.prepare();
                    player.play();
                    broadcastState();
                });
            });
            currentCyclesRequested = repeat;
            ioExecutor.execute(() -> {
                SessionEntity e = new SessionEntity();
                e.startedAt = System.currentTimeMillis();
                e.sourceType = "page";
                e.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                e.repeatCount = repeat;
                e.cyclesRequested = repeat;
                currentSessionId = sessionRepo.insert(e);
            });
        } else if (ACTION_LOAD_SURAH.equals(action)) {
            int surah = intent.getIntExtra("surah", -1);
            int repeat = intent.getIntExtra("repeat",
                    getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("repeat.count", 1));
            Log.d("PlaybackService", "Load Surah: " + String.format("%03d", surah) + " — " + surahName(surah) + ", Repeat=" + (repeat==-1?"∞":repeat));
            if (surah < 1 || surah > 114) {
                Log.e("PlaybackService", "Invalid surah: " + surah);
                return START_STICKY;
            }
            captureSelectionForResume("surah", null, surah, null, null, null, repeat);
            player.stop();
            player.clearMediaItems();
            playbackManager.setFeedingEnabled(false);
            ioExecutor.execute(() -> {
                CycleResult cycle = buildSurahCycle(surah);
                if (!com.repeatquran.util.NetworkUtil.isOnline(PlaybackService.this) && cycle.cachedCount == 0) {
                    Log.w("PlaybackService", "Offline with no cached audio for selection (surah)");
                    mainHandler.post(() -> android.widget.Toast.makeText(PlaybackService.this, "Offline: no cached audio for this surah", android.widget.Toast.LENGTH_LONG).show());
                    return;
                }
                mainHandler.post(() -> {
                    enqueueCycles(cycle.items, repeat);
                    player.prepare();
                    player.play();
                    broadcastState();
                });
            });
            currentCyclesRequested = repeat;
            ioExecutor.execute(() -> {
                SessionEntity se = new SessionEntity();
                se.startedAt = System.currentTimeMillis();
                se.sourceType = "surah";
                se.startSurah = surah;
                se.recitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
                se.repeatCount = repeat;
                se.cyclesRequested = repeat;
                currentSessionId = sessionRepo.insert(se);
            });
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
        if (recitersOverride != null && !recitersOverride.isEmpty()) {
            return new java.util.ArrayList<>(recitersOverride);
        }
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
        if (reciters.isEmpty()) reciters = java.util.Arrays.asList("Abdurrahmaan_As-Sudais_64kbps");
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

    private CycleResult buildRangeCycle(int ss, int sa, int es, int ea) {
        java.util.List<String> reciters = getSelectedReciterIds();
        if (reciters.isEmpty()) reciters = java.util.Arrays.asList("Abdurrahmaan_As-Sudais_64kbps");
        CycleResult out = new CycleResult();
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
                    if (cached.exists()) {
                        out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached)));
                        out.cachedCount++;
                    } else {
                        out.items.add(MediaItem.fromUri(url));
                        cacheManager.cacheAsync(url, rid, sss, aaa);
                    }
                    out.totalCount++;
                }
            }
        }
        Log.d("PlaybackService", orderLog.toString());
        return out;
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
        for (String id : ids) {
            String name = idToName.get(id);
            out.add(name != null ? name : id);
        }
        return out;
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

    private CycleResult buildPageCycle(int page) {
        PageSegmentDao dao = RepeatQuranDatabase.get(this).pageSegmentDao();
        java.util.List<PageSegmentEntity> segs = dao.segmentsForPage(page);
        java.util.List<String> reciters = getSelectedReciterIds();
        if (reciters.isEmpty()) reciters = java.util.Arrays.asList("Abdurrahmaan_As-Sudais_64kbps");
        java.util.List<String> reciterNames = getReciterNames(reciters);
        StringBuilder orderLog = new StringBuilder("Cycle order (page ").append(page).append("): ");
        CycleResult out = new CycleResult();
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
                    if (cached.exists()) {
                        out.items.add(MediaItem.fromUri(android.net.Uri.fromFile(cached)));
                        out.cachedCount++;
                    } else {
                        out.items.add(MediaItem.fromUri(url));
                        cacheManager.cacheAsync(url, rid, sss, aaa);
                    }
                    out.totalCount++;
                }
            }
        }
        Log.d("PlaybackService", orderLog.toString());
        Log.d("PlaybackService", "Page " + page + " itemsPerCycle=" + out.items.size());
        return out;
    }

    private CycleResult buildSurahCycle(int surah) {
        java.util.List<String> reciters = getSelectedReciterIds();
        if (reciters.isEmpty()) reciters = java.util.Arrays.asList("Abdurrahmaan_As-Sudais_64kbps");
        java.util.List<String> reciterNames = getReciterNames(reciters);
        int maxAyah = getAyahCount(surah);
        StringBuilder orderLog = new StringBuilder("Cycle order (surah ").append(String.format("%03d", surah)).append("): ");
        CycleResult out = new CycleResult();
        for (int i = 0; i < reciters.size(); i++) {
            String rid = reciters.get(i);
            orderLog.append(reciterNames.get(i));
            if (i < reciters.size() - 1) orderLog.append(" -> ");
            String sss = String.format("%03d", surah);
            for (int a = 1; a <= maxAyah; a++) {
                String aaa = String.format("%03d", a);
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
            }
        }
        Log.d("PlaybackService", orderLog.toString());
        Log.d("PlaybackService", "Surah " + surah + " itemsPerCycle=" + out.items.size());
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
        }
    }

    private void broadcastState() {
        try {
            boolean hasQueue = player != null && player.getMediaItemCount() > 0;
            int state = player != null ? player.getPlaybackState() : Player.STATE_IDLE;
            boolean active = hasQueue && state != Player.STATE_ENDED;
            android.content.Intent i = new android.content.Intent(ACTION_PLAYBACK_STATE);
            i.putExtra("hasQueue", hasQueue);
            i.putExtra("state", state);
            i.putExtra("active", active);
            sendBroadcast(i);
        } catch (Exception ignored) {}
    }

    private void captureSelectionForResume(String sourceType, Integer page, Integer ss, Integer sa, Integer es, Integer ea, Integer repeat) {
        lastSourceType = sourceType;
        lastPage = page;
        lastStartSurah = ss;
        lastStartAyah = sa;
        lastEndSurah = es;
        lastEndAyah = ea;
        lastRepeat = repeat;
        lastRecitersCsv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        saveResumeStateNow();
    }

    private void saveResumeStatePeriodic() {
        if (player == null) return;
        if (player.getMediaItemCount() == 0) return;
        saveResumeStateNow();
    }

    private void saveResumeStateNow() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
            android.content.SharedPreferences.Editor ed = prefs.edit();
            if (lastSourceType != null) ed.putString("resume.sourceType", lastSourceType);
            if (lastPage != null) ed.putInt("resume.page", lastPage);
            if (lastStartSurah != null) ed.putInt("resume.startSurah", lastStartSurah);
            if (lastStartAyah != null) ed.putInt("resume.startAyah", lastStartAyah);
            if (lastEndSurah != null) ed.putInt("resume.endSurah", lastEndSurah);
            if (lastEndAyah != null) ed.putInt("resume.endAyah", lastEndAyah);
            if (lastRepeat != null) ed.putInt("resume.repeat", lastRepeat);
            if (lastRecitersCsv != null) ed.putString("resume.recitersCsv", lastRecitersCsv);
            ed.putInt("resume.mediaIndex", player.getCurrentMediaItemIndex());
            ed.putLong("resume.positionMs", Math.max(0, player.getCurrentPosition()));
            ed.putLong("resume.timestamp", System.currentTimeMillis());
            ed.apply();
        } catch (Exception ignored) {}
    }

    private void onResumeRequested() {
        // Snapshot the most current position before reading prefs
        saveResumeStateNow();
        android.content.SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        String st = prefs.getString("resume.sourceType", null);
        if (st == null) {
            android.widget.Toast.makeText(this, "No recent session to resume", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        int mediaIndex = prefs.getInt("resume.mediaIndex", -1);
        long positionMs = prefs.getLong("resume.positionMs", 0);
        int repeat = prefs.getInt("resume.repeat", 1);
        String recitersCsv = prefs.getString("resume.recitersCsv", "");
        java.util.List<String> reciters = new java.util.ArrayList<>();
        if (recitersCsv != null && !recitersCsv.isEmpty()) {
            for (String s : recitersCsv.split(",")) if (!s.isEmpty()) reciters.add(s);
        }

        player.stop();
        player.clearMediaItems();
        playbackManager.setFeedingEnabled(false);

        // Use saved reciters for fidelity
        recitersOverride = reciters;

        if ("single".equals(st)) {
            int ss = prefs.getInt("resume.startSurah", 1);
            int sa = prefs.getInt("resume.startAyah", 1);
            CycleResult cycle = buildSingleAyahCycle(String.format("%03d", ss), String.format("%03d", sa));
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                recitersOverride = null;
                return;
            }
            enqueueCycles(cycle.items, repeat);
        } else if ("range".equals(st)) {
            int ss = prefs.getInt("resume.startSurah", 1);
            int sa = prefs.getInt("resume.startAyah", 1);
            int es = prefs.getInt("resume.endSurah", ss);
            int ea = prefs.getInt("resume.endAyah", sa);
            CycleResult cycle = buildRangeCycle(ss, sa, es, ea);
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                recitersOverride = null;
                return;
            }
            enqueueCycles(cycle.items, repeat);
        } else if ("page".equals(st)) {
            final int page = prefs.getInt("resume.page", 1);
            final int fRepeat = repeat;
            final int fMediaIndex = mediaIndex;
            final long fPositionMs = positionMs;
            ioExecutor.execute(() -> {
                CycleResult cycle = buildPageCycle(page); // DB access off main thread
                if (!com.repeatquran.util.NetworkUtil.isOnline(PlaybackService.this) && cycle.cachedCount == 0) {
                    mainHandler.post(() -> {
                        android.widget.Toast.makeText(PlaybackService.this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                        recitersOverride = null;
                    });
                    return;
                }
                mainHandler.post(() -> {
                    enqueueCycles(cycle.items, fRepeat);
                    recitersOverride = null;
                    player.prepare();
                    if (fMediaIndex >= 0) player.seekTo(fMediaIndex, Math.max(0, fPositionMs));
                    player.play();
                });
            });
            return;
        } else if ("surah".equals(st)) {
            int ss = prefs.getInt("resume.startSurah", 1);
            CycleResult cycle = buildSurahCycle(ss);
            if (!com.repeatquran.util.NetworkUtil.isOnline(this) && cycle.cachedCount == 0) {
                android.widget.Toast.makeText(this, "Offline: no cached audio to resume", android.widget.Toast.LENGTH_LONG).show();
                recitersOverride = null;
                return;
            }
            enqueueCycles(cycle.items, repeat);
        } else {
            android.widget.Toast.makeText(this, "Nothing to resume", android.widget.Toast.LENGTH_SHORT).show();
            recitersOverride = null;
            return;
        }

        recitersOverride = null;
        player.prepare();
        if (mediaIndex >= 0) player.seekTo(mediaIndex, Math.max(0, positionMs));
        player.play();
    }
}
