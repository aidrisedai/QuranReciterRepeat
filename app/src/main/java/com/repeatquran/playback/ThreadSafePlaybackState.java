package com.repeatquran.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe state manager for PlaybackService to prevent race conditions
 * and ensure consistent state across multiple threads.
 */
public class ThreadSafePlaybackState {
    private static final String TAG = "ThreadSafePlaybackState";
    
    // Read-write lock for fine-grained control
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    
    private final Context context;
    
    // Resume state variables (now thread-safe)
    private volatile String lastSourceType = null;
    private volatile Integer lastPage = null;
    private volatile Integer lastStartSurah = null;
    private volatile Integer lastStartAyah = null;
    private volatile Integer lastEndSurah = null;
    private volatile Integer lastEndAyah = null;
    private volatile Integer lastRepeat = null;
    private volatile String lastRecitersCsv = null;
    private volatile Boolean lastHalfSplit = null;
    
    // Reciters override (now thread-safe)
    private volatile List<String> recitersOverride = null;
    
    // Loop control variables (now thread-safe)
    private volatile Integer loopCyclesTarget = null;
    private volatile int loopCyclesDone = 0;
    private volatile int cycleItemsCount = 0;
    private volatile int lastMediaIndex = -1;
    
    public ThreadSafePlaybackState(Context context) {
        this.context = context.getApplicationContext();
    }
    
    // Thread-safe resume state capture
    public void captureSelectionForResume(String sourceType, Integer page, Integer ss, Integer sa, 
                                        Integer es, Integer ea, Integer repeat) {
        writeLock.lock();
        try {
            this.lastSourceType = sourceType;
            this.lastPage = page;
            this.lastStartSurah = ss;
            this.lastStartAyah = sa;
            this.lastEndSurah = es;
            this.lastEndAyah = ea;
            this.lastRepeat = repeat;
            this.lastRecitersCsv = getSharedPreferences().getString("reciters.order", "");
            
            // Save to preferences atomically
            saveResumeStateNow();
            
            Log.d(TAG, "Captured resume state: " + sourceType + " (thread: " + Thread.currentThread().getName() + ")");
        } finally {
            writeLock.unlock();
        }
    }
    
    // Thread-safe reciters override management
    public void setRecitersOverride(List<String> reciters) {
        writeLock.lock();
        try {
            this.recitersOverride = reciters != null ? new ArrayList<>(reciters) : null;
            Log.d(TAG, "Set reciters override: " + (reciters != null ? reciters.size() : 0) + " reciters");
        } finally {
            writeLock.unlock();
        }
    }
    
    public List<String> getRecitersOverride() {
        readLock.lock();
        try {
            return recitersOverride != null ? new ArrayList<>(recitersOverride) : null;
        } finally {
            readLock.unlock();
        }
    }
    
    public void clearRecitersOverride() {
        writeLock.lock();
        try {
            this.recitersOverride = null;
        } finally {
            writeLock.unlock();
        }
    }
    
    // Thread-safe loop control
    public void setLoopControl(Integer target, int itemsCount) {
        writeLock.lock();
        try {
            this.loopCyclesTarget = target;
            this.loopCyclesDone = 0;
            this.cycleItemsCount = itemsCount;
            this.lastMediaIndex = -1;
        } finally {
            writeLock.unlock();
        }
    }
    
    public void clearLoopControl() {
        writeLock.lock();
        try {
            this.loopCyclesTarget = null;
            this.loopCyclesDone = 0;
            this.cycleItemsCount = 0;
            this.lastMediaIndex = -1;
        } finally {
            writeLock.unlock();
        }
    }
    
    public boolean checkAndIncrementLoop(int currentMediaIndex) {
        writeLock.lock();
        try {
            if (loopCyclesTarget != null && cycleItemsCount > 0) {
                // Detect wrap-around: last index -> 0
                if (currentMediaIndex == 0 && lastMediaIndex == cycleItemsCount - 1) {
                    loopCyclesDone++;
                    Log.d(TAG, "Loop cycle completed: " + loopCyclesDone + "/" + loopCyclesTarget);
                    
                    if (loopCyclesDone >= loopCyclesTarget) {
                        clearLoopControl();
                        return true; // Signal to stop looping
                    }
                }
                lastMediaIndex = currentMediaIndex;
            }
            return false;
        } finally {
            writeLock.unlock();
        }
    }
    
    // Thread-safe resume state retrieval
    public ResumeState getResumeState() {
        readLock.lock();
        try {
            SharedPreferences prefs = getSharedPreferences();
            
            ResumeState state = new ResumeState();
            state.sourceType = prefs.getString("resume.sourceType", lastSourceType);
            state.page = lastPage != null ? lastPage : prefs.getInt("resume.page", -1);
            state.startSurah = lastStartSurah != null ? lastStartSurah : prefs.getInt("resume.startSurah", -1);
            state.startAyah = lastStartAyah != null ? lastStartAyah : prefs.getInt("resume.startAyah", -1);
            state.endSurah = lastEndSurah != null ? lastEndSurah : prefs.getInt("resume.endSurah", -1);
            state.endAyah = lastEndAyah != null ? lastEndAyah : prefs.getInt("resume.endAyah", -1);
            state.repeat = lastRepeat != null ? lastRepeat : prefs.getInt("resume.repeat", 1);
            state.recitersCsv = lastRecitersCsv != null ? lastRecitersCsv : prefs.getString("resume.recitersCsv", "");
            state.halfSplit = lastHalfSplit != null ? lastHalfSplit : prefs.getBoolean("ui.half.split", false);
            state.mediaIndex = prefs.getInt("resume.mediaIndex", -1);
            state.positionMs = prefs.getLong("resume.positionMs", 0);
            
            return state;
        } finally {
            readLock.unlock();
        }
    }
    
    private void saveResumeStateNow() {
        try {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            
            if (lastSourceType != null) editor.putString("resume.sourceType", lastSourceType);
            if (lastPage != null) editor.putInt("resume.page", lastPage);
            if (lastStartSurah != null) editor.putInt("resume.startSurah", lastStartSurah);
            if (lastStartAyah != null) editor.putInt("resume.startAyah", lastStartAyah);
            if (lastEndSurah != null) editor.putInt("resume.endSurah", lastEndSurah);
            if (lastEndAyah != null) editor.putInt("resume.endAyah", lastEndAyah);
            if (lastRepeat != null) editor.putInt("resume.repeat", lastRepeat);
            if (lastRecitersCsv != null) editor.putString("resume.recitersCsv", lastRecitersCsv);
            
            editor.putLong("resume.timestamp", System.currentTimeMillis());
            editor.apply(); // Use apply() for async persistence
        } catch (Exception e) {
            Log.e(TAG, "Failed to save resume state", e);
        }
    }
    
    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences("rq_prefs", Context.MODE_PRIVATE);
    }
    
    // Data class for resume state
    public static class ResumeState {
        public String sourceType;
        public int page = -1;
        public int startSurah = -1;
        public int startAyah = -1;
        public int endSurah = -1;
        public int endAyah = -1;
        public int repeat = 1;
        public String recitersCsv = "";
        public boolean halfSplit = false;
        public int mediaIndex = -1;
        public long positionMs = 0;
    }
}