package com.repeatquran.memorization;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.repeatquran.R;
import com.repeatquran.data.MemorizationGoalRepository;
import com.repeatquran.data.SessionRepository;
import com.repeatquran.data.SessionType;
import com.repeatquran.data.db.MemorizationGoalEntity;
import com.repeatquran.data.db.SessionEntity;
import com.repeatquran.util.AyahCounts;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Memorization Session Activity
 * AI-powered memorization session with strictness-based feedback and goal tracking
 */
public class MemorizationSessionActivity extends AppCompatActivity {
    
    private static final String TAG = "MemorizationSession";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    public static final String EXTRA_GOAL_ID = "goal_id";
    
    // UI Components
    private TextView goalTitleText;
    private TextView goalDetailsText;
    private TextView strictnessBadge;
    private ProgressBar goalProgressBar;
    private TextView progressText;
    private TextView statusText;
    private TextView timerText;
    private MaterialButton startButton;
    private MaterialButton stopButton;
    private CardView feedbackCard;
    private TextView feedbackText;
    private LinearLayout loadingLayout;
    private MaterialButton playVerseButton;
    private MaterialButton nextVerseButton;
    
    // Data
    private MemorizationGoalRepository goalRepository;
    private SessionRepository sessionRepository;
    private MemorizationGoalEntity currentGoal;
    private long goalId;
    private long sessionId;
    private long sessionStartTime = 0;
    
    // Session tracking
    private int currentVerseIndex = 0; // Which verse in the range we're on
    private int totalVerses = 0;
    private int versesCompleted = 0;
    
    // Error tracking for summary
    private java.util.List<VerseError> verseErrors = new java.util.ArrayList<>();
    
    // Helper class to track errors
    private static class VerseError {
        int verseNumber;
        String errorType;
        
        VerseError(int verseNumber, String errorType) {
            this.verseNumber = verseNumber;
            this.errorType = errorType;
        }
    }
    
    // Local STT components
    private LocalSpeechRecognizer localRecognizer;
    private QuranVerseProvider verseProvider;
    private String currentExpectedVerseText;
    private StringBuilder currentTranscript = new StringBuilder();
    
    private boolean isSessionActive = false;
    private Handler timerHandler = new Handler();
    
    private Executor executor = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memorization_session);
        
        // Initialize repositories
        goalRepository = new MemorizationGoalRepository(this);
        sessionRepository = new SessionRepository(this);
        
        // Get goal ID from intent
        goalId = getIntent().getLongExtra(EXTRA_GOAL_ID, -1);
        if (goalId == -1) {
            Toast.makeText(this, "Error: No goal specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        initViews();
        
        // Load goal
        loadGoal();
        
        // Request microphone permission
        requestAudioPermission();
        
        // Setup button listeners
        setupListeners();
    }
    
    private void initViews() {
        goalTitleText = findViewById(R.id.goalTitle);
        goalDetailsText = findViewById(R.id.goalDetails);
        strictnessBadge = findViewById(R.id.strictnessBadge);
        goalProgressBar = findViewById(R.id.goalProgress);
        progressText = findViewById(R.id.progressText);
        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        feedbackCard = findViewById(R.id.feedbackCard);
        feedbackText = findViewById(R.id.feedbackText);
        loadingLayout = findViewById(R.id.loadingLayout);
        playVerseButton = findViewById(R.id.playVerseButton);
        nextVerseButton = findViewById(R.id.nextVerseButton);
    }
    
    private void setupListeners() {
        findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());
        startButton.setOnClickListener(v -> startSession());
        stopButton.setOnClickListener(v -> {
            // Stop and show summary
            stopSession();
            showSummary();
        });
        playVerseButton.setOnClickListener(v -> playCurrentVerse());
        nextVerseButton.setOnClickListener(v -> moveToNextVerse());
    }
    
    private void loadGoal() {
        executor.execute(() -> {
            currentGoal = goalRepository.getById(goalId);
            
            if (currentGoal == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: Goal not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }
            
            runOnUiThread(() -> {
                // Display goal info
                goalTitleText.setText(currentGoal.goalText);
                
                // Calculate total verses
                totalVerses = calculateTotalVerses(currentGoal);
                versesCompleted = currentGoal.currentProgress;
                
                String details = String.format("Surah %d:%d-%d • %d verses", 
                    currentGoal.targetSurahStart, 
                    currentGoal.targetAyahStart, 
                    currentGoal.targetAyahEnd, 
                    totalVerses);
                goalDetailsText.setText(details);
                
                // Strictness badge
                String strictnessText = currentGoal.strictnessLevel != null ? 
                    currentGoal.strictnessLevel : "moderate";
                strictnessBadge.setText(capitalize(strictnessText));
                
                // Progress
                updateProgress();
                
                // Status
                statusText.setText("Ready to start");
            });
        });
    }
    
    private int calculateTotalVerses(MemorizationGoalEntity goal) {
        return goal.targetAyahEnd - goal.targetAyahStart + 1;
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Moderate";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private void updateProgress() {
        if (currentGoal == null) return;
        
        int progress = totalVerses > 0 ? (versesCompleted * 100) / totalVerses : 0;
        goalProgressBar.setProgress(progress);
        progressText.setText(versesCompleted + "/" + totalVerses);
    }
    
    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission required for recitation", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void startSession() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant microphone permission", Toast.LENGTH_SHORT).show();
            requestAudioPermission();
            return;
        }
        
        // Create session in database
        sessionStartTime = System.currentTimeMillis();
        executor.execute(() -> {
            SessionEntity session = new SessionEntity();
            session.startedAt = sessionStartTime;
            session.endedAt = null; // Active session
            session.sourceType = "memorization";
            session.sessionType = SessionType.MEMORIZATION;
            session.strictnessLevel = currentGoal.strictnessLevel;
            session.goalId = goalId;
            
            sessionId = sessionRepository.insert(session);
            
            runOnUiThread(() -> {
                // Update UI
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                loadingLayout.setVisibility(View.VISIBLE);
                statusText.setText("Loading verse...");
                
                // Start local speech recognition
                initializeLocalRecognition();
            });
        });
    }
    
    private void initializeLocalRecognition() {
        // Check if speech recognition is available
        if (!LocalSpeechRecognizer.isAvailable(this)) {
            runOnUiThread(() -> {
                loadingLayout.setVisibility(View.GONE);
                Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show();
                statusText.setText("Error: Speech recognition unavailable");
            });
            return;
        }
        
        // Initialize components
        localRecognizer = new LocalSpeechRecognizer(this);
        verseProvider = new QuranVerseProvider();
        
        // Load the current verse text
        int currentSurah = currentGoal.targetSurahStart;
        int currentAyah = currentGoal.targetAyahStart + currentVerseIndex;
        
        verseProvider.getVerseText(currentSurah, currentAyah, new QuranVerseProvider.VerseCallback() {
            @Override
            public void onVerseLoaded(String arabicText) {
                currentExpectedVerseText = arabicText;
                Log.d(TAG, "Loaded verse " + currentSurah + ":" + currentAyah + " - " + arabicText);
                
                // Prefetch next few verses for smoother experience
                int endAyah = Math.min(currentAyah + 5, currentGoal.targetAyahEnd);
                verseProvider.prefetchVerses(currentSurah, currentAyah + 1, endAyah);
                
                // Start listening
                startListening();
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading verse: " + error);
                    loadingLayout.setVisibility(View.GONE);
                    Toast.makeText(MemorizationSessionActivity.this, 
                        "Failed to load verse text. Check internet connection.", Toast.LENGTH_LONG).show();
                    statusText.setText("Error loading verse");
                });
            }
        });
    }
    
    private void startListening() {
        runOnUiThread(() -> {
            isSessionActive = true;
            loadingLayout.setVisibility(View.GONE);
            statusText.setText("🎤 Listening... Start reciting");
            startTimer();
            
            int currentVerseNum = currentGoal.targetAyahStart + currentVerseIndex;
            feedbackCard.setVisibility(View.VISIBLE);
            feedbackText.setText(String.format("📖 Verse %d\n\nListening...", currentVerseNum));
        });
        
        // Clear transcript for new verse
        currentTranscript.setLength(0);
        
        localRecognizer.startListening(new LocalSpeechRecognizer.TranscriptionCallback() {
            @Override
            public void onPartialResult(String text) {
                // Update UI with real-time transcription
                runOnUiThread(() -> {
                    int currentVerseNum = currentGoal.targetAyahStart + currentVerseIndex;
                    String expectedPreview = currentExpectedVerseText != null ? 
                        currentExpectedVerseText.substring(0, Math.min(50, currentExpectedVerseText.length())) + "..." : 
                        "Loading...";
                    feedbackText.setText(String.format("📖 Verse %d\n\nExpected:\n%s\n\n🎤 Hearing:\n%s...", 
                        currentVerseNum, expectedPreview, text));
                });
            }
            
            @Override
            public void onFinalResult(String text) {
                Log.d(TAG, "Final result: " + text);
                
                // Append to continuous transcript
                if (currentTranscript.length() > 0) {
                    currentTranscript.append(" ");
                }
                currentTranscript.append(text);
                
                // Check if verse is complete
                String transcriptSoFar = currentTranscript.toString();
                
                // Debug logging
                Log.d(TAG, "=== Verse Matching Debug ===");
                Log.d(TAG, "Transcript so far: " + transcriptSoFar);
                Log.d(TAG, "Expected verse: " + currentExpectedVerseText);
                Log.d(TAG, "Transcript length: " + transcriptSoFar.length());
                Log.d(TAG, "Expected length: " + (currentExpectedVerseText != null ? currentExpectedVerseText.length() : "null"));
                
                if (currentExpectedVerseText == null) {
                    Log.e(TAG, "ERROR: Expected verse text is NULL!");
                    runOnUiThread(() -> {
                        feedbackText.setText("⚠️ Error: Verse text not loaded\n\nPlease stop and restart session");
                    });
                    return;
                }
                
                // Simple length-based check: if transcript is at least 40% of expected length
                String normalizedTranscript = VerseMatchingEngine.normalizeArabicPublic(transcriptSoFar);
                String normalizedExpected = VerseMatchingEngine.normalizeArabicPublic(currentExpectedVerseText);
                
                double lengthRatio = (double) normalizedTranscript.length() / normalizedExpected.length();
                Log.d(TAG, "Length ratio: " + (lengthRatio * 100) + "%");
                
                // If we've recited at least 40% of the verse length, check similarity
                if (lengthRatio >= 0.4) {
                    double similarity = VerseMatchingEngine.calculateSimilarity(
                        transcriptSoFar,
                        currentExpectedVerseText
                    );
                    
                    Log.d(TAG, "Verse match similarity: " + (similarity * 100) + "%");
                    
                    // VERY LENIENT: Accept 30% match OR just enable Next button
                    if (similarity >= 0.30) {
                        // Good enough match - advance verse
                        onVerseCompleted(true, similarity);
                    } else {
                        // Show score and enable manual advance (always)
                        runOnUiThread(() -> {
                            int currentVerse = currentGoal.targetAyahStart + currentVerseIndex;
                            feedbackText.setText(String.format(
                                "📖 Verse %d: %.0f%% match\n\nExpected:\n%s\n\n🎤 You said:\n%s\n\n👉 Tap Next Verse when ready",
                                currentVerse, 
                                similarity * 100,
                                currentExpectedVerseText.substring(0, Math.min(80, currentExpectedVerseText.length())) + "...",
                                transcriptSoFar));
                            nextVerseButton.setVisibility(View.VISIBLE);
                        });
                    }
                } else {
                    Log.d(TAG, "Keep reciting (length: " + lengthRatio * 100 + "%)...");
                    // Show real-time progress
                    runOnUiThread(() -> {
                        int currentVerseNum = currentGoal.targetAyahStart + currentVerseIndex;
                        String expectedShort = currentExpectedVerseText.length() > 60 ? 
                            currentExpectedVerseText.substring(0, 60) + "..." : currentExpectedVerseText;
                        feedbackText.setText(String.format(
                            "📖 Verse %d (%.0f%% length)\n\nExpected:\n%s\n\n🎤 You:\n%s\n\nKeep reciting...",
                            currentVerseNum,
                            lengthRatio * 100,
                            expectedShort,
                            transcriptSoFar));
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Speech recognition error: " + error);
                    // Don't stop the session - just log the error
                    // LocalSpeechRecognizer will auto-restart
                });
            }
        });
    }
    
    private void onVerseCompleted(boolean isCorrect, double similarity) {
        runOnUiThread(() -> {
            int currentVerse = currentGoal.targetAyahStart + currentVerseIndex;
            
            if (!isCorrect) {
                // Track error
                verseErrors.add(new VerseError(currentVerse, 
                    String.format("%.0f%% match - needs review", similarity * 100)));
                
                // Show feedback
                feedbackText.setText(String.format(
                    "⚠️ Verse %d: %.0f%% match\n\nContinue to next verse",
                    currentVerse, similarity * 100));
            } else {
                // Show success
                feedbackText.setText(String.format(
                    "✓ Verse %d: %.0f%% match\n\nExcellent!",
                    currentVerse, similarity * 100));
            }
            
            // Advance to next verse after short delay
            new Handler().postDelayed(() -> {
                advanceToNextVerse();
            }, 2000);
        });
    }
    
    private void advanceToNextVerse() {
        currentVerseIndex++;
        versesCompleted++;
        updateProgress();
        updateGoalProgress();
        
        // Check if done
        if (currentVerseIndex >= totalVerses) {
            // Finished all verses!
            stopSession();
            showSummary();
            return;
        }
        
        // Load next verse
        int nextSurah = currentGoal.targetSurahStart;
        int nextAyah = currentGoal.targetAyahStart + currentVerseIndex;
        
        statusText.setText(String.format("Loading verse %d...", nextAyah));
        
        verseProvider.getVerseText(nextSurah, nextAyah, new QuranVerseProvider.VerseCallback() {
            @Override
            public void onVerseLoaded(String arabicText) {
                currentExpectedVerseText = arabicText;
                Log.d(TAG, "Loaded next verse: " + nextSurah + ":" + nextAyah);
                
                // Reset transcript and continue listening
                currentTranscript.setLength(0);
                
                runOnUiThread(() -> {
                    statusText.setText("🎤 Listening...");
                    feedbackText.setText(String.format("📖 Verse %d\n\nListening...", nextAyah));
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading next verse: " + error);
                    Toast.makeText(MemorizationSessionActivity.this, 
                        "Failed to load next verse", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateGoalProgress() {
        // Save progress to goal in background
        executor.execute(() -> {
            currentGoal.currentProgress = versesCompleted;
            currentGoal.lastActivityAt = System.currentTimeMillis();
            
            if (versesCompleted >= totalVerses) {
                // Goal completed!
                currentGoal.isCompleted = true;
                currentGoal.completedAt = System.currentTimeMillis();
            }
            
            goalRepository.update(currentGoal);
        });
    }
    
    
    private void showSummary() {
        runOnUiThread(() -> {
            // Build summary message
            StringBuilder summary = new StringBuilder();
            summary.append(String.format("✅ Completed: %d/%d verses\n\n", 
                versesCompleted, totalVerses));
            
            if (verseErrors.isEmpty()) {
                summary.append("🎉 Perfect! No errors detected.\n\nMashaAllah!");
            } else {
                summary.append(String.format("📊 Mistakes: %d error%s detected\n\n", 
                    verseErrors.size(), verseErrors.size() > 1 ? "s" : ""));
                
                // List errors
                for (VerseError error : verseErrors) {
                    summary.append(String.format("• Verse %d: %s\n", 
                        error.verseNumber, error.errorType));
                }
            }
            
            // Show summary dialog
            new android.app.AlertDialog.Builder(this)
                .setTitle(versesCompleted >= totalVerses ? "🎉 Session Complete!" : "Session Summary")
                .setMessage(summary.toString())
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
        });
    }
    
    private void stopSession() {
        if (!isSessionActive) return;
        
        isSessionActive = false;
        
        // Stop local speech recognizer
        if (localRecognizer != null) {
            localRecognizer.stopListening();
            localRecognizer.destroy();
            localRecognizer = null;
        }
        
        // Stop timer
        timerHandler.removeCallbacks(timerRunnable);
        
        // Update progress one final time
        updateGoalProgress();
        
        // End session in database
        executor.execute(() -> {
            sessionRepository.markEnded(sessionId, System.currentTimeMillis(), null);
        });
        
        runOnUiThread(() -> {
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.GONE);
            statusText.setText("Session ended");
        });
    }
    
    private void playCurrentVerse() {
        // TODO: Implement verse playback using EveryAyah API
        Toast.makeText(this, "Verse playback coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void moveToNextVerse() {
        if (!isSessionActive) {
            Toast.makeText(this, "Start a session first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Advance to next verse (marks current as complete)
        nextVerseButton.setVisibility(View.GONE);
        advanceToNextVerse();
    }
    
    private void startTimer() {
        timerHandler.post(timerRunnable);
    }
    
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSessionActive) {
                long elapsed = System.currentTimeMillis() - sessionStartTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                
                timerText.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isSessionActive) {
            stopSession();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isSessionActive) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("End Session?")
                .setMessage("Are you sure you want to end this memorization session?")
                .setPositiveButton("End", (dialog, which) -> {
                    stopSession();
                    super.onBackPressed();
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }
    
    private int getStrictnessInt(String strictness) {
        if (strictness == null) return 2;
        switch (strictness.toLowerCase()) {
            case "lenient": return 1;
            case "strict": return 3;
            default: return 2;
        }
    }
}
