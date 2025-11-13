package com.repeatquran.memorization;

import com.repeatquran.data.db.MemorizationUnitEntity;

public class MemorizationSession {
    public enum SessionType {
        NEW_LEARNING,    // Full flow: slow×5 → fast×10 → prompt
        RECENT_REVIEW,   // Simplified: fast×3 → self-test
        OLD_REVIEW       // Page-level: fast×2 → self-test
    }
    
    public enum Phase {
        IDLE,
        PLAYING_SLOW,      // 0.8× speed, 5 reps (new learning only)
        PLAYING_FAST,      // 1.5× speed, 10 reps (new) or 3 reps (recent review)
        REVIEW_PLAYBACK,   // 1.2× speed, 2 reps (old review)
        PROMPTING,         // Asking "Can you recite?" (new learning)
        REVIEW_SELF_TEST,  // Asking "Did you recall correctly?" (reviews)
        COMPLETED
    }
    
    private MemorizationUnitEntity currentUnit;
    private Phase currentPhase = Phase.IDLE;
    private SessionType sessionType = SessionType.NEW_LEARNING;
    private long sessionStartTime;
    private long phaseStartTime;
    
    private int slowRepsCompleted = 0;
    private int fastRepsCompleted = 0;
    private int reviewRepsCompleted = 0;
    
    // New learning constants
    public static final int SLOW_REPS = 5;
    public static final int FAST_REPS = 10;
    public static final float SLOW_SPEED = 0.8f;
    public static final float FAST_SPEED = 1.5f;
    
    // Review constants
    public static final int RECENT_REVIEW_REPS = 3;
    public static final int OLD_REVIEW_REPS = 2;
    public static final float REVIEW_SPEED = 1.2f;
    
    public void startUnit(MemorizationUnitEntity unit, SessionType type) {
        this.currentUnit = unit;
        this.sessionType = type;
        this.currentPhase = Phase.IDLE;
        this.sessionStartTime = System.currentTimeMillis();
        this.slowRepsCompleted = 0;
        this.fastRepsCompleted = 0;
        this.reviewRepsCompleted = 0;
    }
    
    public void startSlowPhase() {
        this.currentPhase = Phase.PLAYING_SLOW;
        this.phaseStartTime = System.currentTimeMillis();
    }
    
    public void startFastPhase() {
        this.currentPhase = Phase.PLAYING_FAST;
        this.phaseStartTime = System.currentTimeMillis();
    }
    
    public void startPrompting() {
        this.currentPhase = Phase.PROMPTING;
        this.phaseStartTime = System.currentTimeMillis();
    }
    
    public void startReviewPlayback() {
        this.currentPhase = Phase.REVIEW_PLAYBACK;
        this.phaseStartTime = System.currentTimeMillis();
    }
    
    public void startReviewSelfTest() {
        this.currentPhase = Phase.REVIEW_SELF_TEST;
        this.phaseStartTime = System.currentTimeMillis();
    }
    
    public void complete() {
        this.currentPhase = Phase.COMPLETED;
    }
    
    public MemorizationUnitEntity getCurrentUnit() {
        return currentUnit;
    }
    
    public Phase getCurrentPhase() {
        return currentPhase;
    }
    
    public SessionType getSessionType() {
        return sessionType;
    }
    
    public int getSlowRepsCompleted() {
        return slowRepsCompleted;
    }
    
    public void incrementSlowReps() {
        slowRepsCompleted++;
    }
    
    public int getFastRepsCompleted() {
        return fastRepsCompleted;
    }
    
    public void incrementFastReps() {
        fastRepsCompleted++;
    }
    
    public int getReviewRepsCompleted() {
        return reviewRepsCompleted;
    }
    
    public void incrementReviewReps() {
        reviewRepsCompleted++;
    }
    
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public boolean isSlowPhaseComplete() {
        return slowRepsCompleted >= SLOW_REPS;
    }
    
    public boolean isFastPhaseComplete() {
        if (sessionType == SessionType.RECENT_REVIEW) {
            return fastRepsCompleted >= RECENT_REVIEW_REPS;
        }
        return fastRepsCompleted >= FAST_REPS;
    }
    
    public boolean isReviewPlaybackComplete() {
        return reviewRepsCompleted >= OLD_REVIEW_REPS;
    }
    
    public String getProgressText() {
        switch (currentPhase) {
            case PLAYING_SLOW:
                return "Slow playback: " + slowRepsCompleted + "/" + SLOW_REPS;
            case PLAYING_FAST:
                if (sessionType == SessionType.RECENT_REVIEW) {
                    return "Review playback: " + fastRepsCompleted + "/" + RECENT_REVIEW_REPS;
                }
                return "Fast playback: " + fastRepsCompleted + "/" + FAST_REPS;
            case REVIEW_PLAYBACK:
                return "Review playback: " + reviewRepsCompleted + "/" + OLD_REVIEW_REPS;
            case PROMPTING:
                return "Can you recite this from memory?";
            case REVIEW_SELF_TEST:
                return "Did you recall this correctly?";
            case COMPLETED:
                return "Completed";
            default:
                return "Ready";
        }
    }
}
