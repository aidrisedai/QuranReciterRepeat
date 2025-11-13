package com.repeatquran.memorization;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.data.MemorizationGoalRepository;
import com.repeatquran.data.SessionType;
import com.repeatquran.data.db.MemorizationGoalEntity;
import com.repeatquran.util.SurahNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalInputActivity extends AppCompatActivity {
    
    private static final String TAG = "GoalInputActivity";
    private static final int REQUEST_CODE_SPEECH = 100;
    
    private TextInputEditText goalInput;
    private TextInputLayout goalInputLayout;
    private MaterialButton voiceInputButton;
    private RadioGroup strictnessGroup;
    private MaterialCardView parsedGoalCard;
    private android.widget.TextView parsedGoalText;
    private MaterialButton createGoalButton;
    private MaterialButton cancelButton;
    
    private MemorizationGoalRepository goalRepo;
    private GoalParser.ParsedGoal currentParsedGoal;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_input);
        
        goalRepo = new MemorizationGoalRepository(this);
        
        // Initialize views
        goalInput = findViewById(R.id.goalInput);
        goalInputLayout = findViewById(R.id.goalInputLayout);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        strictnessGroup = findViewById(R.id.strictnessGroup);
        parsedGoalCard = findViewById(R.id.parsedGoalCard);
        parsedGoalText = findViewById(R.id.parsedGoalText);
        createGoalButton = findViewById(R.id.createGoalButton);
        cancelButton = findViewById(R.id.cancelButton);
        
        setupListeners();
    }
    
    private void setupListeners() {
        // Text input listener - parse goal as user types
        goalInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    parseAndDisplayGoal(s.toString());
                } else {
                    hideGoalPreview();
                }
            }
        });
        
        // Voice input
        voiceInputButton.setOnClickListener(v -> startVoiceInput());
        
        // Create goal
        createGoalButton.setOnClickListener(v -> createGoal());
        
        // Cancel
        cancelButton.setOnClickListener(v -> finish());
    }
    
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell us your memorization goal");
        
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                goalInput.setText(spokenText);
                Log.d(TAG, "Voice input: " + spokenText);
            }
        }
    }
    
    private void parseAndDisplayGoal(String goalText) {
        currentParsedGoal = GoalParser.parse(goalText);
        
        if (currentParsedGoal.isValid()) {
            // Show parsed goal preview
            String preview = buildGoalPreview(currentParsedGoal);
            parsedGoalText.setText(preview);
            parsedGoalCard.setVisibility(View.VISIBLE);
            createGoalButton.setEnabled(true);
            goalInputLayout.setError(null);
            
            Log.d(TAG, "Goal parsed successfully: " + preview);
        } else {
            // Show error
            hideGoalPreview();
            
            if (goalText.length() > 3) {
                // Only show error if user has typed enough
                goalInputLayout.setError(currentParsedGoal.error);
            }
        }
    }
    
    private String buildGoalPreview(GoalParser.ParsedGoal goal) {
        StringBuilder sb = new StringBuilder();
        
        // Surah info
        if (goal.startSurah.equals(goal.endSurah)) {
            // Single surah
            String surahName = SurahNames.name(goal.startSurah);
            sb.append("Surah ").append(goal.startSurah);
            if (surahName != null) {
                sb.append(" (").append(surahName).append(")");
            }
            
            if (goal.startAyah == 1 && goal.endAyah == com.repeatquran.util.AyahCounts.getCount(goal.startSurah)) {
                // Whole surah
                sb.append(" • Complete surah");
            } else {
                // Partial surah
                sb.append(" • Verses ").append(goal.startAyah).append("-").append(goal.endAyah);
            }
        } else {
            // Multiple surahs
            sb.append("Surahs ").append(goal.startSurah).append("-").append(goal.endSurah);
        }
        
        // Total verses
        sb.append(" • ").append(goal.totalVerses).append(" verses");
        
        // Goal type
        if (goal.versesPerDay != null && goal.versesPerDay > 0) {
            sb.append(" • ").append(goal.versesPerDay).append(" verses/day");
            
            // Estimate days
            int estimatedDays = (int) Math.ceil((double) goal.totalVerses / goal.versesPerDay);
            sb.append(" (~").append(estimatedDays).append(" days)");
        }
        
        return sb.toString();
    }
    
    private void hideGoalPreview() {
        parsedGoalCard.setVisibility(View.GONE);
        createGoalButton.setEnabled(false);
    }
    
    private void createGoal() {
        if (currentParsedGoal == null || !currentParsedGoal.isValid()) {
            Toast.makeText(this, "Please enter a valid goal", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get strictness level
        String strictness = getSelectedStrictness();
        
        // Create goal entity
        new Thread(() -> {
            MemorizationGoalEntity goal = new MemorizationGoalEntity();
            goal.goalText = currentParsedGoal.goalText;
            goal.goalType = currentParsedGoal.goalType;
            goal.targetSurahStart = currentParsedGoal.startSurah;
            goal.targetAyahStart = currentParsedGoal.startAyah;
            goal.targetSurahEnd = currentParsedGoal.endSurah;
            goal.targetAyahEnd = currentParsedGoal.endAyah;
            goal.versesPerDay = currentParsedGoal.versesPerDay;
            goal.strictnessLevel = strictness;
            goal.startDate = System.currentTimeMillis();
            goal.isActive = true;
            goal.isCompleted = false;
            goal.isPaused = false;
            goal.currentProgress = 0;
            goal.totalVerses = currentParsedGoal.totalVerses;
            goal.createdAt = System.currentTimeMillis();
            
            // Calculate target end date for recurring goals
            if (goal.versesPerDay != null && goal.versesPerDay > 0) {
                int estimatedDays = (int) Math.ceil((double) goal.totalVerses / goal.versesPerDay);
                goal.targetEndDate = goal.startDate + (estimatedDays * 24L * 60 * 60 * 1000);
            }
            
            // Deactivate any existing active goals
            goalRepo.deactivateAll();
            
            // Insert new goal
            long goalId = goalRepo.insert(goal);
            
            runOnUiThread(() -> {
                Log.d(TAG, "Goal created with ID: " + goalId);
                Toast.makeText(this, "Goal created successfully!", Toast.LENGTH_SHORT).show();
                
                // Return result
                Intent resultIntent = new Intent();
                resultIntent.putExtra("goal_id", goalId);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }).start();
    }
    
    private String getSelectedStrictness() {
        int selectedId = strictnessGroup.getCheckedRadioButtonId();
        
        if (selectedId == R.id.strictnessLenient) {
            return SessionType.STRICTNESS_LENIENT;
        } else if (selectedId == R.id.strictnessStrict) {
            return SessionType.STRICTNESS_STRICT;
        } else {
            return SessionType.STRICTNESS_MODERATE; // default
        }
    }
}
