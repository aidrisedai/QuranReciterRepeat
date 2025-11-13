package com.repeatquran.memorization;

import android.content.Context;
import android.os.Build;

import com.repeatquran.playback.PlaybackService;

public class PlaybackLoopController {
    private final Context context;
    private int targetReps;
    private float speed;
    private int completed;

    public interface Callback {
        void onPhaseCompleted();
    }

    public PlaybackLoopController(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public void startPhase(String sourceType, int surah, int startAyah, int endSurah, int endAyah, int page, int repeat, float speed) {
        this.targetReps = Math.max(1, repeat);
        this.speed = speed;
        this.completed = 0;
        // Set speed globally
        android.content.Intent sp = new android.content.Intent(context, PlaybackService.class);
        sp.setAction(PlaybackService.ACTION_SET_SPEED);
        sp.putExtra("speed", speed);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(sp); else context.startService(sp);

        // Trigger playback load by source
        android.content.Intent intent = new android.content.Intent(context, PlaybackService.class);
        if ("single".equals(sourceType)) {
            intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
            intent.putExtra("sura", surah);
            intent.putExtra("ayah", startAyah);
            intent.putExtra("repeat", repeat);
        } else if ("range".equals(sourceType)) {
            intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
            intent.putExtra("ss", surah);
            intent.putExtra("sa", startAyah);
            intent.putExtra("es", endSurah);
            intent.putExtra("ea", endAyah);
            intent.putExtra("repeat", repeat);
        } else if ("page".equals(sourceType)) {
            intent.setAction(PlaybackService.ACTION_LOAD_PAGE);
            intent.putExtra("page", page);
            intent.putExtra("repeat", repeat);
        } else if ("surah".equals(sourceType)) {
            intent.setAction(PlaybackService.ACTION_LOAD_SURAH);
            intent.putExtra("surah", surah);
            intent.putExtra("repeat", repeat);
        }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent); else context.startService(intent);
    }
}
