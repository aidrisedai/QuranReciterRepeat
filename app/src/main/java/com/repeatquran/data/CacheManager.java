package com.repeatquran.data;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    private static final String TAG = "CacheManager";
    private static volatile CacheManager INSTANCE;
    private final Context appContext;
    // Bounded executor to avoid unbounded task queue memory growth
    private final ExecutorService ioExecutor = new ThreadPoolExecutor(
            2, // core
            2, // max
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(64) // cap outstanding tasks
    );
    // Track in-flight downloads to prevent duplicate scheduling
    private final Set<String> inflight = Collections.synchronizedSet(new HashSet<>());

    private CacheManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static CacheManager get(Context context) {
        if (INSTANCE == null) {
            synchronized (CacheManager.class) {
                if (INSTANCE == null) INSTANCE = new CacheManager(context);
            }
        }
        return INSTANCE;
    }

    public File getTargetFile(String reciterId, String sss, String aaa) {
        File base = new File(appContext.getFilesDir(), "audio");
        File reciterDir = new File(base, safe(reciterId));
        if (!reciterDir.exists()) reciterDir.mkdirs();
        return new File(reciterDir, sss + aaa + ".mp3");
    }

    public boolean isCached(String reciterId, String sss, String aaa) {
        return getTargetFile(reciterId, sss, aaa).exists();
    }

    public void cacheAsync(String url, String reciterId, String sss, String aaa) {
        File target = getTargetFile(reciterId, sss, aaa);
        if (target.exists()) return;
        String key = safe(reciterId) + ":" + sss + aaa;
        if (!inflight.add(key)) return; // already scheduled
        try {
            ioExecutor.execute(() -> {
                try { downloadTo(url, target); }
                finally { inflight.remove(key); }
            });
        } catch (RejectedExecutionException rex) {
            // Queue is full; drop this background cache to avoid OOM pressure
            inflight.remove(key);
            Log.w(TAG, "Cache queue full; dropping: " + url);
        }
    }

    private void downloadTo(String urlStr, File target) {
        File tmp = new File(target.getParentFile(), target.getName() + ".part");
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP " + conn.getResponseCode() + " for " + urlStr);
                conn.disconnect();
                return;
            }
            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            conn.disconnect();
            // Atomically move into place
            if (!tmp.renameTo(target)) {
                // Fallback: delete target and rename
                //noinspection ResultOfMethodCallIgnored
                target.delete();
                //noinspection ResultOfMethodCallIgnored
                tmp.renameTo(target);
            }
            Log.d(TAG, "Cached: " + target.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Cache failed for " + urlStr, e);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    private String safe(String s) {
        // Escape dash properly in Java string for regex character class
        return s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
