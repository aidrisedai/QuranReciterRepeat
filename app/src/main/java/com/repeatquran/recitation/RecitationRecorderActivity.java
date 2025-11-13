package com.repeatquran.recitation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.repeatquran.HomeActivity;
import com.repeatquran.MainActivity;
import com.repeatquran.memorization.MemorizationActivity;
import com.repeatquran.R;
import com.repeatquran.settings.SettingsActivity;
import java.io.File;
import java.io.IOException;

public class RecitationRecorderActivity extends AppCompatActivity {
    
    private static final String TAG = "RecitationRecorder";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    // UI Components
    private TextView statusText;
    private TextView timerText;
    private MaterialButton recordButton;
    private CardView playbackCard;
    private MaterialButton playButton;
    private MaterialButton deleteButton;
    private MaterialButton analyzeButton;
    private CardView feedbackCard;
    private TextView feedbackText;
    private LinearLayout loadingLayout;
    private CardView verseIdentificationCard;
    private TextView verseIdentificationText;
    private MaterialButton playCorrectRecitationButton;
    private MaterialButton hearTeacherButton;
    private String currentArabicText = "";
    private RecitationFeedback currentFeedback = null;
    
    // Recording
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private File audioFile;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private long recordingStartTime = 0;
    private Handler timerHandler = new Handler();
    
    // AI Analyzer
    private GeminiRecitationAnalyzer analyzer;
    
    // Correct recitation playback
    private MediaPlayer correctRecitationPlayer;
    private int currentVerseIndex = 0;
    private int totalVerses = 0;
    private int correctSurah = 0;
    private int correctStartVerse = 0;
    private int correctEndVerse = 0;
    
    // OpenAI TTS for high-quality bilingual audio (testing vs ElevenLabs)
    private OpenAITTS openAITTS;
    private boolean isSpeaking = false;
    
    // Real-time teacher mode
    private RealtimeQuranTeacher realtimeTeacher;
    private GeminiLiveQuranTeacher geminiLiveTeacher;
    private GeminiRealtimeTeacher geminiRealtimeTeacher; // Hybrid: Gemini API + Android TTS
    private boolean isLiveMode = false;
    private MaterialButton liveTeacherButton;
    private TextView liveTeacherStatus;
    private boolean useOpenAI = true; // Use OpenAI - it was working
    
    // Comprehension level
    private ComprehensionLevel currentComprehensionLevel = ComprehensionLevel.MEDIUM; // Default
    private MaterialButton comprehensionLevelButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recitation_recorder);
        
        // Initialize views
        initViews();
        
        // Setup navigation
        setupBottomNavigation();
        
        // Initialize AI analyzer
        analyzer = new GeminiRecitationAnalyzer(this);
        
        // Initialize OpenAI TTS (testing vs ElevenLabs)
        openAITTS = new OpenAITTS(this);
        
        // Load saved comprehension level
        loadComprehensionLevel();
        
        // Request permissions
        requestAudioPermission();
        
        // Setup button listeners
        setupListeners();
    }
    
    private void loadComprehensionLevel() {
        String savedLevel = getSharedPreferences("rq_prefs", MODE_PRIVATE)
            .getString("recitation.comprehension_level", "MEDIUM");
        
        try {
            currentComprehensionLevel = ComprehensionLevel.valueOf(savedLevel);
        } catch (Exception e) {
            currentComprehensionLevel = ComprehensionLevel.MEDIUM;
        }
        
        updateComprehensionLevelButton();
    }
    
    
    private void initViews() {
        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        recordButton = findViewById(R.id.recordButton);
        playbackCard = findViewById(R.id.playbackCard);
        playButton = findViewById(R.id.playButton);
        deleteButton = findViewById(R.id.deleteButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        feedbackCard = findViewById(R.id.feedbackCard);
        feedbackText = findViewById(R.id.feedbackText);
        loadingLayout = findViewById(R.id.loadingLayout);
        verseIdentificationCard = findViewById(R.id.verseIdentificationCard);
        verseIdentificationText = findViewById(R.id.verseIdentificationText);
        playCorrectRecitationButton = findViewById(R.id.playCorrectRecitationButton);
        hearTeacherButton = findViewById(R.id.hearTeacherButton);
        liveTeacherButton = findViewById(R.id.liveTeacherButton);
        comprehensionLevelButton = findViewById(R.id.comprehensionLevelButton);
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (itemId == R.id.nav_learn) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_progress) {
                startActivity(new Intent(this, MemorizationActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }
    
    private void setupListeners() {
        recordButton.setOnClickListener(v -> toggleRecording());
        playButton.setOnClickListener(v -> togglePlayback());
        deleteButton.setOnClickListener(v -> deleteRecording());
        analyzeButton.setOnClickListener(v -> analyzeRecitation());
        liveTeacherButton.setOnClickListener(v -> toggleLiveTeacherMode());
        comprehensionLevelButton.setOnClickListener(v -> showComprehensionLevelDialog());
    }
    
    private void showComprehensionLevelDialog() {
        String[] levels = new String[3];
        for (int i = 0; i < ComprehensionLevel.values().length; i++) {
            ComprehensionLevel level = ComprehensionLevel.values()[i];
            levels[i] = level.getIcon() + " " + level.getDisplayName() + "\n" + level.getShortDescription();
        }
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("📚 Choose Feedback Level")
            .setSingleChoiceItems(levels, currentComprehensionLevel.ordinal(), (dialog, which) -> {
                currentComprehensionLevel = ComprehensionLevel.values()[which];
                updateComprehensionLevelButton();
                
                // Save preference
                getSharedPreferences("rq_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("recitation.comprehension_level", currentComprehensionLevel.name())
                    .apply();
                
                dialog.dismiss();
                
                Toast.makeText(this, 
                    "Feedback level: " + currentComprehensionLevel.getDisplayName(), 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void updateComprehensionLevelButton() {
        comprehensionLevelButton.setText(
            currentComprehensionLevel.getIcon() + " " + 
            currentComprehensionLevel.getDisplayName() + " - " + 
            currentComprehensionLevel.getShortDescription()
        );
    }
    
    private void toggleLiveTeacherMode() {
        if (isLiveMode) {
            // Stop live mode
            stopLiveTeacherMode();
        } else {
            // Start live mode
            startLiveTeacherMode();
        }
    }
    
    private void startLiveTeacherMode() {
        liveTeacherButton.setEnabled(false);
        liveTeacherButton.setText("⏳ Connecting...");
        
        // Use OpenAI for now - it works reliably
        startOpenAILiveMode();
    }
    
    private void startGeminiRealtimeMode() {
        if (geminiRealtimeTeacher == null) {
            geminiRealtimeTeacher = new GeminiRealtimeTeacher(this);
        }
        
        // Show initializing message
        statusText.setText("📢 Initializing voice system...");
        Toast.makeText(this, "Setting up voice... this may take a few seconds", Toast.LENGTH_LONG).show();
        
        // Set comprehension level before starting
        geminiRealtimeTeacher.setComprehensionLevel(currentComprehensionLevel);
        Log.d(TAG, "Starting Gemini Realtime teacher with comprehension level: " + currentComprehensionLevel.getDisplayName());
        
        geminiRealtimeTeacher.startSession(new GeminiRealtimeTeacher.SessionCallback() {
            @Override
            public void onSessionStarted() {
                runOnUiThread(() -> {
                    isLiveMode = true;
                    liveTeacherButton.setEnabled(true);
                    liveTeacherButton.setText("⏹️ Stop Gemini Teacher");
                    statusText.setText("🎧 Gemini is listening...");
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Teacher ready! Start reciting...", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onTeacherSpeaking(String text) {
                runOnUiThread(() -> {
                    statusText.setText("👨‍🏫 " + text);
                });
            }
            
            @Override
            public void onTeacherFinished() {
                runOnUiThread(() -> {
                    statusText.setText("🎧 Continue reciting...");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isLiveMode = false;
                    liveTeacherButton.setEnabled(true);
                    liveTeacherButton.setText("🎧 Start Live Teacher (Gemini)");
                    statusText.setText("Ready to Record");
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void startGeminiLiveMode() {
        if (geminiLiveTeacher == null) {
            geminiLiveTeacher = new GeminiLiveQuranTeacher(this);
        }
        
        // Set comprehension level before starting
        geminiLiveTeacher.setComprehensionLevel(currentComprehensionLevel);
        Log.d(TAG, "Starting Gemini Live teacher with comprehension level: " + currentComprehensionLevel.getDisplayName());
        
        geminiLiveTeacher.startSession(new GeminiLiveQuranTeacher.SessionCallback() {
            @Override
            public void onSessionStarted() {
                runOnUiThread(() -> {
                    isLiveMode = true;
                    liveTeacherButton.setEnabled(true);
                    liveTeacherButton.setText("⏹️ Stop Gemini Live");
                    statusText.setText("🎧 Gemini Live: Start reciting!");
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Gemini teacher is listening. Start reciting!", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onTeacherSpeaking(String text) {
                runOnUiThread(() -> {
                    statusText.setText("👨‍🏫 Teacher: " + text);
                });
            }
            
            @Override
            public void onTeacherFinished() {
                runOnUiThread(() -> {
                    statusText.setText("🎧 Continue reciting...");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    stopLiveTeacherMode();
                });
            }
        });
    }
    
    private void startOpenAILiveMode() {
        if (realtimeTeacher == null) {
            realtimeTeacher = new RealtimeQuranTeacher(this);
        }
        
        // Set comprehension level before starting
        realtimeTeacher.setComprehensionLevel(currentComprehensionLevel);
        Log.d(TAG, "Starting OpenAI live teacher with comprehension level: " + currentComprehensionLevel.getDisplayName());
        
        realtimeTeacher.startSession(new RealtimeQuranTeacher.SessionCallback() {
            @Override
            public void onSessionStarted() {
                runOnUiThread(() -> {
                    isLiveMode = true;
                    liveTeacherButton.setEnabled(true);
                    liveTeacherButton.setText("⏹️ Stop Live Teacher");
                    statusText.setText("🎧 Live Mode: Recite now!");
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Live teacher is listening. Start reciting!", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onTeacherSpeaking(String text) {
                runOnUiThread(() -> {
                    // Show what teacher is saying
                    statusText.setText("👨‍🏫 Teacher: " + text);
                });
            }
            
            @Override
            public void onTeacherFinished() {
                runOnUiThread(() -> {
                    statusText.setText("🎧 Continue reciting...");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    stopLiveTeacherMode();
                });
            }
        });
    }
    
    private void stopLiveTeacherMode() {
        if (realtimeTeacher != null) {
            realtimeTeacher.stopSession();
        }
        if (geminiRealtimeTeacher != null) {
            geminiRealtimeTeacher.stopSession();
        }
        if (geminiLiveTeacher != null) {
            geminiLiveTeacher.stopSession();
        }
        isLiveMode = false;
        liveTeacherButton.setText("🎧 Start Live Teacher");
        liveTeacherButton.setEnabled(true);
        statusText.setText("Ready to Record");
        Toast.makeText(this, "Live mode stopped", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Audio permission is required to record", 
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }
    
    private void startRecording() {
        try {
            // Create audio file
            audioFile = new File(getCacheDir(), "recitation_" + System.currentTimeMillis() + ".m4a");
            
            // Initialize MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            // Update UI
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            statusText.setText("Recording... 🔴");
            recordButton.setText("⏹️");
            recordButton.setBackgroundColor(0xFFFF5252);
            
            // Hide previous recording UI
            playbackCard.setVisibility(View.GONE);
            analyzeButton.setVisibility(View.GONE);
            feedbackCard.setVisibility(View.GONE);
            
            // Start timer
            startTimer();
            
            Log.d(TAG, "Recording started: " + audioFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            // Update UI
            isRecording = false;
            statusText.setText("Recording Complete ✅");
            recordButton.setText("🎙️");
            recordButton.setBackgroundColor(0xFFFFFFFF);
            
            // Stop timer
            timerHandler.removeCallbacks(timerRunnable);
            
            // Show playback controls
            playbackCard.setVisibility(View.VISIBLE);
            analyzeButton.setVisibility(View.VISIBLE);
            
            Log.d(TAG, "Recording stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
            Toast.makeText(this, "Failed to stop recording: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void togglePlayback() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }
    
    private void startPlayback() {
        if (audioFile == null || !audioFile.exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            isPlaying = true;
            playButton.setText("⏸️ Pause");
            
            // Listen for completion
            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlayback();
            });
            
            Log.d(TAG, "Playback started");
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to play recording", e);
            Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        
        isPlaying = false;
        playButton.setText("▶️ Play");
        Log.d(TAG, "Playback stopped");
    }
    
    private void deleteRecording() {
        stopPlayback();
        
        if (audioFile != null && audioFile.exists()) {
            audioFile.delete();
            audioFile = null;
        }
        
        // Reset UI
        statusText.setText("Ready to Record");
        timerText.setText("00:00");
        playbackCard.setVisibility(View.GONE);
        analyzeButton.setVisibility(View.GONE);
        feedbackCard.setVisibility(View.GONE);
        
        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Recording deleted");
    }
    
    private void analyzeRecitation() {
        if (audioFile == null || !audioFile.exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        loadingLayout.setVisibility(View.VISIBLE);
        feedbackCard.setVisibility(View.GONE);
        analyzeButton.setEnabled(false);
        
        Log.d(TAG, "Starting analysis of recitation");
        
        // Analyze with Gemini (AI will identify the verses automatically)
        analyzer.analyzeRecitation(audioFile, "Quran recitation", new GeminiRecitationAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(RecitationFeedback feedback) {
                runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    
                    // Display verse identification if detected
                    if (feedback.verseIdentification.identified) {
                        verseIdentificationText.setText(feedback.verseIdentification.getDisplayText());
                        verseIdentificationCard.setVisibility(View.VISIBLE);
                        
                        // Store complete feedback for TTS
                        currentArabicText = feedback.arabicText;
                        currentFeedback = feedback;
                        
                        // Setup correct recitation playback
                        setupCorrectRecitationPlayback(
                            feedback.verseIdentification.surahNumber,
                            feedback.verseIdentification.startVerse,
                            feedback.verseIdentification.endVerse
                        );
                        
                        // Setup teacher voice button
                        setupTeacherVoice();
                    } else {
                        verseIdentificationCard.setVisibility(View.GONE);
                    }
                    
                    // Display feedback
                    feedbackText.setText(feedback.toDisplayString());
                    feedbackCard.setVisibility(View.VISIBLE);
                    
                    Log.d(TAG, "Analysis complete");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    
                    Toast.makeText(RecitationRecorderActivity.this,
                            "Analysis failed: " + error, Toast.LENGTH_LONG).show();
                    
                    Log.e(TAG, "Analysis error: " + error);
                });
            }
        });
    }
    
    private void startTimer() {
        timerHandler.post(timerRunnable);
    }
    
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsed = System.currentTimeMillis() - recordingStartTime;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) (elapsed / 1000 / 60);
                
                timerText.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop recording if activity is paused
        if (isRecording) {
            stopRecording();
        }
        stopPlayback();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (correctRecitationPlayer != null) {
            correctRecitationPlayer.release();
        }
        if (openAITTS != null) {
            openAITTS.stop();
        }
        timerHandler.removeCallbacks(timerRunnable);
    }
    
    private void setupTeacherVoice() {
        hearTeacherButton.setOnClickListener(v -> {
            speakArabicText();
        });
    }
    
    private String buildTeachingScript(RecitationFeedback feedback) {
        StringBuilder script = new StringBuilder();
        
        // Teaching comment (main correction)
        if (feedback.overallComment != null && !feedback.overallComment.isEmpty()) {
            script.append(feedback.overallComment).append(" ");
        }
        
        // Encouragement
        if (feedback.encouragement != null && !feedback.encouragement.isEmpty()) {
            script.append(feedback.encouragement);
        }
        
        return script.toString();
    }
    
    private void speakArabicText() {
        if (currentFeedback == null) {
            Toast.makeText(this, "No feedback available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Stop any ongoing speech
        if (isSpeaking) {
            openAITTS.stop();
            isSpeaking = false;
            hearTeacherButton.setText("🗣️ Hear Teacher");
            return;
        }
        
        Log.d(TAG, "Speaking teaching feedback with OpenAI TTS");
        
        // Only speak the teaching feedback (Arabic examples will be embedded in the teaching script)
        String teachingScript = buildTeachingScript(currentFeedback);
        
        openAITTS.speak(teachingScript, new OpenAITTS.TTSCallback() {
            @Override
            public void onStart() {
                runOnUiThread(() -> {
                    isSpeaking = true;
                    hearTeacherButton.setText("⏸️ Speaking...");
                });
            }
            
            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    isSpeaking = false;
                    hearTeacherButton.setText("🗣️ Hear Teacher");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isSpeaking = false;
                    hearTeacherButton.setText("🗣️ Hear Teacher");
                    Toast.makeText(RecitationRecorderActivity.this, 
                        "Speech error: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "OpenAI TTS error: " + error);
                });
            }
        });
    }
    
    private void setupCorrectRecitationPlayback(int surahNumber, int startVerse, int endVerse) {
        correctSurah = surahNumber;
        correctStartVerse = startVerse;
        correctEndVerse = endVerse;
        totalVerses = endVerse - startVerse + 1;
        
        Log.d(TAG, "Setting up playback for Surah " + surahNumber + ", verses " + startVerse + "-" + endVerse);
        
        playCorrectRecitationButton.setOnClickListener(v -> {
            playCorrectRecitation();
        });
    }
    
    private void playCorrectRecitation() {
        if (correctSurah == 0 || correctStartVerse == 0) {
            Toast.makeText(this, "No verses identified", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Stop any ongoing playback
        stopPlayback();
        if (correctRecitationPlayer != null && correctRecitationPlayer.isPlaying()) {
            correctRecitationPlayer.stop();
            correctRecitationPlayer.release();
            correctRecitationPlayer = null;
            playCorrectRecitationButton.setText("🔊 Listen to Correct Recitation");
            return;
        }
        
        // Start playing from the first verse
        currentVerseIndex = 0;
        playCorrectRecitationButton.setText("⏸️ Stop Correct Recitation");
        playNextVerse();
    }
    
    private void playNextVerse() {
        if (currentVerseIndex >= totalVerses) {
            // Finished playing all verses
            if (correctRecitationPlayer != null) {
                correctRecitationPlayer.release();
                correctRecitationPlayer = null;
            }
            playCorrectRecitationButton.setText("🔊 Listen to Correct Recitation");
            Toast.makeText(this, "Correct recitation complete", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int verseNumber = correctStartVerse + currentVerseIndex;
        String audioUrl = buildAudioUrl(correctSurah, verseNumber);
        
        Log.d(TAG, "Playing verse " + verseNumber + " from " + audioUrl);
        
        try {
            if (correctRecitationPlayer != null) {
                correctRecitationPlayer.release();
            }
            
            correctRecitationPlayer = new MediaPlayer();
            correctRecitationPlayer.setDataSource(audioUrl);
            correctRecitationPlayer.prepareAsync();
            
            correctRecitationPlayer.setOnPreparedListener(mp -> {
                mp.start();
                // Update button text with progress
                int progress = currentVerseIndex + 1;
                playCorrectRecitationButton.setText("🔊 Playing (" + progress + "/" + totalVerses + ")");
            });
            
            correctRecitationPlayer.setOnCompletionListener(mp -> {
                currentVerseIndex++;
                playNextVerse(); // Play next verse
            });
            
            correctRecitationPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Error playing verse: what=" + what + ", extra=" + extra);
                Toast.makeText(RecitationRecorderActivity.this, 
                    "Error playing verse " + verseNumber, Toast.LENGTH_SHORT).show();
                currentVerseIndex++;
                playNextVerse(); // Try next verse
                return true;
            });
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to load audio", e);
            Toast.makeText(this, "Failed to load audio for verse " + verseNumber, 
                Toast.LENGTH_SHORT).show();
            currentVerseIndex++;
            playNextVerse(); // Try next verse
        }
    }
    
    private String buildAudioUrl(int surahNumber, int verseNumber) {
        // Get reciter from shared preferences (same as main app)
        android.content.SharedPreferences prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        String reciter = prefs.getString("reciter", "Abdurrahmaan_As-Sudais_64kbps");
        
        // Format: https://everyayah.com/data/<RECITER>/<SSSAAA>.mp3
        // SSS = surah (001-114), AAA = ayah (001-nnn)
        String surahPadded = String.format("%03d", surahNumber);
        String versePadded = String.format("%03d", verseNumber);
        
        return "https://everyayah.com/data/" + reciter + "/" + surahPadded + versePadded + ".mp3";
    }
}
