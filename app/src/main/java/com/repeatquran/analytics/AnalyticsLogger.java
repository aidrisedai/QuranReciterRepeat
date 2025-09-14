package com.repeatquran.analytics;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsLogger {
    private static final String TAG = "Analytics";
    private static volatile AnalyticsLogger INSTANCE;
    private final Context appContext;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final File logsDir;
    private static final int MAX_ROTATED_FILES = 5;
    private static final int MAX_FILE_BYTES = 256 * 1024; // 256 KB per file

    private AnalyticsLogger(Context context) {
        this.appContext = context.getApplicationContext();
        this.logsDir = new File(appContext.getFilesDir(), "logs");
        if (!logsDir.exists()) //noinspection ResultOfMethodCallIgnored
            logsDir.mkdirs();
    }

    public static AnalyticsLogger get(Context context) {
        if (INSTANCE == null) {
            synchronized (AnalyticsLogger.class) {
                if (INSTANCE == null) INSTANCE = new AnalyticsLogger(context);
            }
        }
        return INSTANCE;
    }

    public void log(String event, Map<String, Object> data) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("ts", isoNow());
            obj.put("event", event);
            if (data != null) {
                for (Map.Entry<String, Object> e : data.entrySet()) {
                    Object v = e.getValue();
                    if (v == null) continue;
                    obj.put(e.getKey(), String.valueOf(v));
                }
            }
            String line = obj.toString();
            Log.d(TAG, line);
            io.execute(() -> appendToFile(line + "\n"));
        } catch (Exception e) {
            Log.w(TAG, "log failed", e);
        }
    }

    private String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
    }

    private void appendToFile(String s) {
        try {
            File f = new File(logsDir, "analytics-0.log");
            if (f.exists() && f.length() + s.length() > MAX_FILE_BYTES) rotate();
            try (FileOutputStream out = new FileOutputStream(f, true)) {
                out.write(s.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    private void rotate() {
        try {
            // Delete last if exists
            File last = new File(logsDir, "analytics-" + (MAX_ROTATED_FILES - 1) + ".log");
            if (last.exists()) //noinspection ResultOfMethodCallIgnored
                last.delete();
            // Shift others
            for (int i = MAX_ROTATED_FILES - 2; i >= 0; i--) {
                File src = new File(logsDir, "analytics-" + i + ".log");
                if (src.exists()) {
                    File dst = new File(logsDir, "analytics-" + (i + 1) + ".log");
                    //noinspection ResultOfMethodCallIgnored
                    src.renameTo(dst);
                }
            }
        } catch (Exception ignored) {}
    }
}

