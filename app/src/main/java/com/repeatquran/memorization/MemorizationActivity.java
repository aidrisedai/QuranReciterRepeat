package com.repeatquran.memorization;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.repeatquran.R;
import com.repeatquran.data.db.MemorizationAttemptDao;
import com.repeatquran.data.db.MemorizationAttemptEntity;
import com.repeatquran.data.db.MemorizationUnitDao;
import com.repeatquran.data.db.MemorizationUnitEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;
import com.repeatquran.playback.PlaybackService;

import java.util.ArrayList;
import java.util.List;

public class MemorizationActivity extends AppCompatActivity {
    private LinearLayout sectionNew, sectionRecent, sectionOld;
    private TextView txtCurrentUnit, txtProgress, txtStats;
    private MaterialButton btnStart, btnYes, btnNo, btnDashboard, btnTimelineSettings;
    private View groupPrompt;
    private MaterialCardView cardTimeline;
    private TextView txtTimelineGoal, txtTimelineProgress, txtTimelineCompletion, txtTimelineStatus;

    private MemorizationUnitDao unitDao;
    private MemorizationAttemptDao attemptDao;
    private DailyPlanGenerator planGenerator;
    private MemorizationSession session;
    private PlaybackLoopController playbackController;
    private PerformanceTracker performanceTracker;
    private TimelineCalculator timelineCalculator;

    private List<MemorizationUnitEntity> newLearningUnits = new ArrayList<>();
    private List<MemorizationUnitEntity> reviewUnits = new ArrayList<>();
    private int currentNewIndex = 0;
    private int currentReviewIndex = 0;
    private boolean isReviewMode = false;
    private BroadcastReceiver stateReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memorization);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.memorizationToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Memorization");
        }

        bindViews();

        RepeatQuranDatabase db = RepeatQuranDatabase.get(this);
        unitDao = db.memorizationUnitDao();
        attemptDao = db.memorizationAttemptDao();
        planGenerator = new DailyPlanGenerator(this);
        session = new MemorizationSession();
        playbackController = new PlaybackLoopController(this);
        performanceTracker = new PerformanceTracker(this);
        timelineCalculator = new TimelineCalculator(this);

        wireButtons();
        updateTimelineCard();
        loadDailyPlan();
    }

    private void bindViews() {
        sectionNew = findViewById(R.id.sectionNewUnits);
        sectionRecent = findViewById(R.id.sectionRecentReviews);
        sectionOld = findViewById(R.id.sectionOldReviews);
        txtCurrentUnit = findViewById(R.id.txtCurrentUnit);
        txtProgress = findViewById(R.id.txtProgress);
        txtStats = findViewById(R.id.txtStats);
        btnStart = findViewById(R.id.btnStartMemorization);
        btnYes = findViewById(R.id.btnReciteYes);
        btnNo = findViewById(R.id.btnReciteNo);
        btnDashboard = findViewById(R.id.btnDashboard);
        btnTimelineSettings = findViewById(R.id.btnTimelineSettings);
        groupPrompt = findViewById(R.id.groupPrompt);
        cardTimeline = findViewById(R.id.cardTimeline);
        txtTimelineGoal = findViewById(R.id.txtTimelineGoal);
        txtTimelineProgress = findViewById(R.id.txtTimelineProgress);
        txtTimelineCompletion = findViewById(R.id.txtTimelineCompletion);
        txtTimelineStatus = findViewById(R.id.txtTimelineStatus);
    }

    private void wireButtons() {
        btnStart.setOnClickListener(v -> startNextUnit());
        btnYes.setOnClickListener(v -> handleReciteAnswer(true));
        btnNo.setOnClickListener(v -> handleReciteAnswer(false));
        btnDashboard.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, PerformanceDashboardActivity.class));
        });
        btnTimelineSettings.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, TimelineSettingsActivity.class));
        });
    }

    private void loadDailyPlan() {
        new Thread(() -> {
            DailyPlanGenerator.DailyPlan plan = planGenerator.generate();
            newLearningUnits.clear();
            reviewUnits.clear();
            
            newLearningUnits.addAll(plan.newUnits);
            reviewUnits.addAll(plan.recentReviews);
            reviewUnits.addAll(plan.oldReviews);

            runOnUiThread(() -> {
                displayPlanSections(plan);
                updateStats();
                
                if (plan.hasNewLearning()) {
                    txtCurrentUnit.setText(String.format("New Learning: %d units (≈%d min)\nReviews: %d units (≈%d min)\n\nTap Start to begin",
                        newLearningUnits.size(), plan.newEstimatedMinutes(),
                        reviewUnits.size(), plan.reviewEstimatedMinutes()));
                } else if (plan.hasReviews()) {
                    txtCurrentUnit.setText(String.format("Reviews: %d units (≈%d min)\n\nTap Start to begin reviews",
                        reviewUnits.size(), plan.reviewEstimatedMinutes()));
                } else {
                    txtCurrentUnit.setText("No units scheduled for today");
                }
            });
        }).start();
    }

    private void displayPlanSections(DailyPlanGenerator.DailyPlan plan) {
        sectionNew.removeAllViews();
        sectionRecent.removeAllViews();
        sectionOld.removeAllViews();

        for (MemorizationUnitEntity u : plan.newUnits) sectionNew.addView(createUnitView(u, "new"));
        for (MemorizationUnitEntity u : plan.recentReviews) sectionRecent.addView(createUnitView(u, "recent"));
        for (MemorizationUnitEntity u : plan.oldReviews) sectionOld.addView(createUnitView(u, "old"));
    }

    private View createUnitView(MemorizationUnitEntity unit, String category) {
        TextView tv = new TextView(this);
        String status = "learned".equals(unit.status) ? " ✓" : "";
        tv.setText(String.format("%03d:%d-%d%s", unit.surah, unit.startAyah, unit.endAyah, status));
        tv.setTextSize(14);
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }

    private void startNextUnit() {
        // Try new learning first
        if (!isReviewMode && currentNewIndex < newLearningUnits.size()) {
            MemorizationUnitEntity unit = newLearningUnits.get(currentNewIndex);
            session.startUnit(unit, MemorizationSession.SessionType.NEW_LEARNING);
            txtCurrentUnit.setText(String.format("New Learning %d/%d: Surah %03d, Ayahs %d-%d",
                    currentNewIndex + 1, newLearningUnits.size(), unit.surah, unit.startAyah, unit.endAyah));
            groupPrompt.setVisibility(View.GONE);
            startSlowPhase();
        }
        // Then reviews
        else if (currentReviewIndex < reviewUnits.size()) {
            isReviewMode = true;
            MemorizationUnitEntity unit = reviewUnits.get(currentReviewIndex);
            
            // Determine review type based on age
            long now = System.currentTimeMillis();
            long fourteenDaysAgo = now - (14 * 24 * 60 * 60 * 1000);
            MemorizationSession.SessionType reviewType = (unit.learnedAt != null && unit.learnedAt > fourteenDaysAgo)
                ? MemorizationSession.SessionType.RECENT_REVIEW
                : MemorizationSession.SessionType.OLD_REVIEW;
            
            session.startUnit(unit, reviewType);
            txtCurrentUnit.setText(String.format("Review %d/%d: Surah %03d, Ayahs %d-%d (Level %d)",
                    currentReviewIndex + 1, reviewUnits.size(), unit.surah, unit.startAyah, unit.endAyah, unit.reviewLevel));
            groupPrompt.setVisibility(View.GONE);
            startReviewFlow(reviewType);
        }
        // All done
        else {
            txtCurrentUnit.setText("All units completed! 🎉");
            txtProgress.setText("");
        }
    }

    private void startReviewFlow(MemorizationSession.SessionType reviewType) {
        ensureReceiver();
        
        if (reviewType == MemorizationSession.SessionType.RECENT_REVIEW) {
            // Recent reviews: fast×3 playback
            session.startFastPhase();
            updateProgress();
            
            MemorizationUnitEntity u = session.getCurrentUnit();
            String sourceType = (u.endAyah == u.startAyah) ? "single" : "range";
            playbackController.startPhase(sourceType, u.surah, u.startAyah, u.surah, u.endAyah, -1,
                    MemorizationSession.RECENT_REVIEW_REPS, MemorizationSession.FAST_SPEED);
        } else {
            // Old reviews: review speed×2 playback
            session.startReviewPlayback();
            updateProgress();
            
            MemorizationUnitEntity u = session.getCurrentUnit();
            String sourceType = (u.endAyah == u.startAyah) ? "single" : "range";
            playbackController.startPhase(sourceType, u.surah, u.startAyah, u.surah, u.endAyah, -1,
                    MemorizationSession.OLD_REVIEW_REPS, MemorizationSession.REVIEW_SPEED);
        }
    }
    
    private void startSlowPhase() {
        session.startSlowPhase();
        updateProgress();
        ensureReceiver();

        MemorizationUnitEntity u = session.getCurrentUnit();
        String sourceType = (u.endAyah == u.startAyah) ? "single" : "range";
        playbackController.startPhase(sourceType, u.surah, u.startAyah, u.surah, u.endAyah, -1,
                MemorizationSession.SLOW_REPS, MemorizationSession.SLOW_SPEED);
    }

    private void startFastPhase() {
        session.startFastPhase();
        updateProgress();

        MemorizationUnitEntity u = session.getCurrentUnit();
        String sourceType = (u.endAyah == u.startAyah) ? "single" : "range";
        playbackController.startPhase(sourceType, u.surah, u.startAyah, u.surah, u.endAyah, -1,
                MemorizationSession.FAST_REPS, MemorizationSession.FAST_SPEED);
    }

    private void promptUser() {
        if (session.getSessionType() == MemorizationSession.SessionType.NEW_LEARNING) {
            session.startPrompting();
        } else {
            session.startReviewSelfTest();
        }
        updateProgress();
        groupPrompt.setVisibility(View.VISIBLE);
    }

    private void handleReciteAnswer(boolean success) {
        groupPrompt.setVisibility(View.GONE);
        new Thread(() -> {
            MemorizationUnitEntity unit = session.getCurrentUnit();
            long duration = session.getSessionDuration();

            // Record attempt
            MemorizationAttemptEntity attempt = new MemorizationAttemptEntity();
            attempt.unitId = unit.id;
            attempt.startedAt = System.currentTimeMillis() - duration;
            attempt.completedAt = System.currentTimeMillis();
            attempt.slowRepetitions = MemorizationSession.SLOW_REPS;
            attempt.fastRepetitions = MemorizationSession.FAST_REPS;
            attempt.success = success;
            attempt.wasSplit = false;
            attempt.durationMs = duration;
            attemptDao.insert(attempt);

            // Update unit
            unit.totalRepetitions += (MemorizationSession.SLOW_REPS + MemorizationSession.FAST_REPS);
            unit.totalTimeMs += duration;

            if (success) {
                unit.successCount++;
                unit.status = "learned";
                unit.learnedAt = System.currentTimeMillis();
                unit.reviewLevel++;
                unit.nextReviewAt = ReviewScheduler.calculateNextReview(unit, true);
                unit.ease = ReviewScheduler.updateEase(unit.ease, true, unit.successCount);

                // Record session complete and check for adaptive adjustments
                performanceTracker.recordSessionComplete();
                checkPerformanceAndAdjust();

                runOnUiThread(() -> {
                    if (session.getSessionType() == MemorizationSession.SessionType.NEW_LEARNING) {
                        txtProgress.setText("Learned \u2713");
                        android.widget.Toast.makeText(this, "Great! Unit learned", android.widget.Toast.LENGTH_SHORT).show();
                        currentNewIndex++;
                    } else {
                        txtProgress.setText("Review complete \u2713");
                        android.widget.Toast.makeText(this, "Review successful!", android.widget.Toast.LENGTH_SHORT).show();
                        currentReviewIndex++;
                    }
                    btnStart.postDelayed(() -> startNextUnit(), 1500);
                });
            } else {
                unit.failCount++;

                if (ReviewScheduler.shouldSplit(unit)) {
                    // Split unit into smaller chunks
                    splitUnit(unit);
                    attempt.wasSplit = true;
                    runOnUiThread(() -> {
                        txtProgress.setText("Let's try smaller chunks");
                        android.widget.Toast.makeText(this, "Split into smaller units", android.widget.Toast.LENGTH_SHORT).show();
                        btnStart.postDelayed(() -> loadDailyPlan(), 1500);
                    });
                } else {
                    // Retry fast phase
                    unit.nextReviewAt = ReviewScheduler.calculateNextReview(unit, false);
                    unit.ease = ReviewScheduler.updateEase(unit.ease, false, 0);
                    runOnUiThread(() -> {
                        txtProgress.setText("Let's try the fast phase again");
                        android.widget.Toast.makeText(this, "Repeating fast phase", android.widget.Toast.LENGTH_SHORT).show();
                        btnStart.postDelayed(() -> startFastPhase(), 1500);
                    });
                }
            }

            unitDao.update(unit);
            runOnUiThread(() -> updateStats());
        }).start();
    }

    private void splitUnit(MemorizationUnitEntity parent) {
        int totalVerses = parent.endAyah - parent.startAyah + 1;
        int half = totalVerses / 2;

        MemorizationUnitEntity first = new MemorizationUnitEntity();
        first.surah = parent.surah;
        first.startAyah = parent.startAyah;
        first.endAyah = parent.startAyah + half - 1;
        first.status = "new";
        first.createdAt = System.currentTimeMillis();
        first.totalRepetitions = 0;
        first.successCount = 0;
        first.failCount = 0;
        first.totalTimeMs = 0;
        first.reviewLevel = 0;
        first.ease = 2.5f;
        first.parentUnitId = parent.id;
        unitDao.insert(first);

        MemorizationUnitEntity second = new MemorizationUnitEntity();
        second.surah = parent.surah;
        second.startAyah = parent.startAyah + half;
        second.endAyah = parent.endAyah;
        second.status = "new";
        second.createdAt = System.currentTimeMillis();
        second.totalRepetitions = 0;
        second.successCount = 0;
        second.failCount = 0;
        second.totalTimeMs = 0;
        second.reviewLevel = 0;
        second.ease = 2.5f;
        second.parentUnitId = parent.id;
        unitDao.insert(second);

        // Mark parent as obsolete
        parent.status = "split";
        unitDao.update(parent);
    }

    private void updateProgress() {
        txtProgress.setText(session.getProgressText());
    }

    private void updateStats() {
        new Thread(() -> {
            int learned = unitDao.getLearnedCount();
            int due = unitDao.getDueCount(System.currentTimeMillis());
            
            // Get performance metrics
            PerformanceTracker.PerformanceMetrics metrics = performanceTracker.analyzePerformance();
            int minutes = (int) (metrics.totalStudyTime / 60000);
            int successRate = (int) (metrics.successRate * 100);

            runOnUiThread(() -> {
                String stats = String.format("Learned: %d | Due: %d | Time: %d min\nStreak: %d | Success: %d%% | Chunk: %d aya | Next adjust: %d sessions",
                    learned, due, minutes, metrics.currentStreak, successRate, metrics.currentChunkSize, metrics.sessionsUntilNextAdjustment);
                txtStats.setText(stats);
            });
        }).start();
    }
    
    private void checkPerformanceAndAdjust() {
        new Thread(() -> {
            PerformanceTracker.PerformanceMetrics metrics = performanceTracker.analyzePerformance();
            
            if (metrics.shouldIncreaseChunk) {
                performanceTracker.applyAdjustment(metrics);
                runOnUiThread(() -> {
                    String msg = String.format("🎉 Great progress! Chunk size increased to %d ayahs", metrics.recommendedChunkSize);
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
                });
            } else if (metrics.shouldDecreaseChunk) {
                performanceTracker.applyAdjustment(metrics);
                runOnUiThread(() -> {
                    String msg = String.format("Chunk size adjusted to %d ayahs for easier learning", metrics.recommendedChunkSize);
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
                });
            }
            
            // Refresh stats after adjustment
            runOnUiThread(() -> updateStats());
        }).start();
    }

    private void ensureReceiver() {
        if (stateReceiver != null) return;
        stateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!PlaybackService.ACTION_PLAYBACK_STATE.equals(intent.getAction())) return;
                boolean playing = intent.getBooleanExtra("playing", false);
                int state = intent.getIntExtra("state", 1);

                if (!playing && state == 4) { // STATE_ENDED
                    MemorizationSession.Phase phase = session.getCurrentPhase();
                    
                    if (phase == MemorizationSession.Phase.PLAYING_SLOW) {
                        session.incrementSlowReps();
                        if (session.isSlowPhaseComplete()) {
                            startFastPhase();
                        }
                    } else if (phase == MemorizationSession.Phase.PLAYING_FAST) {
                        session.incrementFastReps();
                        if (session.isFastPhaseComplete()) {
                            promptUser();
                        }
                    } else if (phase == MemorizationSession.Phase.REVIEW_PLAYBACK) {
                        session.incrementReviewReps();
                        if (session.isReviewPlaybackComplete()) {
                            promptUser();
                        }
                    }
                }
            }
        };
        IntentFilter f = new IntentFilter(PlaybackService.ACTION_PLAYBACK_STATE);
        ContextCompat.registerReceiver(this, stateReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stateReceiver != null) {
            try {
                unregisterReceiver(stateReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    private void updateTimelineCard() {
        new Thread(() -> {
            TimelineCalculator.TimelineProjection proj = timelineCalculator.calculateTimeline();
            
            runOnUiThread(() -> {
                txtTimelineGoal.setText("🎯 " + proj.goalName);
                txtTimelineProgress.setText(String.format("Progress: %d/%d ayahs (%.1f%%)",
                    proj.learnedAyahs, proj.goalTotalAyahs, proj.progressPercent));
                txtTimelineCompletion.setText(String.format("Completion: %s (%d days)",
                    TimelineCalculator.formatCompletionDate(proj.projectedCompletionDate),
                    proj.daysToCompletion));
                
                // Status message
                if (proj.actualAyahsPerDay > 0) {
                    if (proj.onTrack) {
                        if (proj.daysAheadBehind > 0) {
                            txtTimelineStatus.setText(String.format("✅ %d days ahead!", proj.daysAheadBehind));
                        } else {
                            txtTimelineStatus.setText("✅ On track!");
                        }
                    } else {
                        txtTimelineStatus.setText(String.format("⚠️ %d days behind", Math.abs(proj.daysAheadBehind)));
                    }
                } else {
                    txtTimelineStatus.setText(TimelineCalculator.getMilestoneMessage(proj.progressPercent));
                }
            });
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateTimelineCard();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
