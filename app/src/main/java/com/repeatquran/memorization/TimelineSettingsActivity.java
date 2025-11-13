package com.repeatquran.memorization;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.repeatquran.R;
import com.repeatquran.data.db.MemorizationGoalDao;
import com.repeatquran.data.db.MemorizationGoalEntity;
import com.repeatquran.data.db.RepeatQuranDatabase;

public class TimelineSettingsActivity extends AppCompatActivity {
    private TextInputEditText editDailyMinutes;
    private AutoCompleteTextView goalDropdown;
    private TextView txtGoalName, txtProgressInfo, txtVelocity, txtCompletionDate, txtOnTrack;
    private MaterialButton btnSave;
    
    private SharedPreferences prefs;
    private MemorizationGoalDao goalDao;
    private TimelineCalculator calculator;
    
    private static final String[] GOAL_NAMES = {
        "Juz ʿAmma (Surahs 78-114)",
        "Juz 29 (Surahs 67-77)",
        "Last 10 Surahs",
        "Full Quran"
    };
    
    private static final String[] GOAL_KEYS = {
        "juz_amma",
        "juz_29",
        "last_10",
        "full_quran"
    };
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline_settings);
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.timelineToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Timeline Settings");
        }
        
        prefs = getSharedPreferences("rq_prefs", MODE_PRIVATE);
        goalDao = RepeatQuranDatabase.get(this).memorizationGoalDao();
        calculator = new TimelineCalculator(this);
        
        bindViews();
        setupGoalDropdown();
        loadCurrentSettings();
        wireButtons();
        updateProjection();
    }
    
    private void bindViews() {
        editDailyMinutes = findViewById(R.id.editDailyMinutes);
        goalDropdown = findViewById(R.id.goalDropdown);
        txtGoalName = findViewById(R.id.txtGoalName);
        txtProgressInfo = findViewById(R.id.txtProgressInfo);
        txtVelocity = findViewById(R.id.txtVelocity);
        txtCompletionDate = findViewById(R.id.txtCompletionDate);
        txtOnTrack = findViewById(R.id.txtOnTrack);
        btnSave = findViewById(R.id.btnSaveTimeline);
    }
    
    private void setupGoalDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, GOAL_NAMES);
        goalDropdown.setAdapter(adapter);
        goalDropdown.setOnItemClickListener((parent, view, position, id) -> {
            goalDropdown.setTag(GOAL_KEYS[position]);
            updateProjection();
        });
    }
    
    private void loadCurrentSettings() {
        // Load daily minutes
        int dailyMinutes = prefs.getInt("plan.daily.total", 45);
        editDailyMinutes.setText(String.valueOf(dailyMinutes));
        
        // Load current goal
        String currentGoal = prefs.getString("timeline.goal", "juz_amma");
        goalDropdown.setTag(currentGoal);
        for (int i = 0; i < GOAL_KEYS.length; i++) {
            if (GOAL_KEYS[i].equals(currentGoal)) {
                goalDropdown.setText(GOAL_NAMES[i], false);
                break;
            }
        }
        
        // Listen for changes to daily minutes
        editDailyMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateProjection();
        });
    }
    
    private void wireButtons() {
        btnSave.setOnClickListener(v -> saveSettings());
    }
    
    private void updateProjection() {
        new Thread(() -> {
            // Get input values
            int dailyMinutes = parseDailyMinutes();
            if (dailyMinutes < 30) dailyMinutes = 30;
            
            String goalType = (String) goalDropdown.getTag();
            if (goalType == null) goalType = "juz_amma";
            
            // Temporarily update prefs for calculation
            prefs.edit()
                .putInt("plan.daily.total", dailyMinutes)
                .putString("timeline.goal", goalType)
                .apply();
            
            // Calculate projection
            TimelineCalculator.TimelineProjection proj = calculator.calculateTimeline();
            
            runOnUiThread(() -> displayProjection(proj));
        }).start();
    }
    
    private void displayProjection(TimelineCalculator.TimelineProjection proj) {
        txtGoalName.setText("Goal: " + proj.goalName);
        txtProgressInfo.setText(String.format("Progress: %d/%d ayahs (%.1f%%)", 
            proj.learnedAyahs, proj.goalTotalAyahs, proj.progressPercent));
        txtVelocity.setText(String.format("Projected pace: %.1f ayahs/day", proj.projectedAyahsPerDay));
        
        String completionText = String.format("Completion: %s (%d days)", 
            TimelineCalculator.formatCompletionDate(proj.projectedCompletionDate), 
            proj.daysToCompletion);
        txtCompletionDate.setText(completionText);
        
        // On track status
        if (proj.actualAyahsPerDay > 0) {
            if (proj.onTrack) {
                if (proj.daysAheadBehind > 0) {
                    txtOnTrack.setText(String.format("✅ %d days ahead of schedule!", proj.daysAheadBehind));
                } else {
                    txtOnTrack.setText("✅ On track!");
                }
            } else {
                txtOnTrack.setText(String.format("⚠️ %d days behind schedule", Math.abs(proj.daysAheadBehind)));
            }
        } else {
            txtOnTrack.setText(TimelineCalculator.getMilestoneMessage(proj.progressPercent));
        }
    }
    
    private void saveSettings() {
        int dailyMinutes = parseDailyMinutes();
        if (dailyMinutes < 30) {
            android.widget.Toast.makeText(this, "Minimum 30 minutes required", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        String goalType = (String) goalDropdown.getTag();
        if (goalType == null) goalType = "juz_amma";
        
        // Save to prefs
        prefs.edit()
            .putInt("plan.daily.total", dailyMinutes)
            .putString("timeline.goal", goalType)
            .apply();
        
        // Recalculate time allocations
        calculator.recalculateOnSettingsChange(dailyMinutes);
        
        // Save/update goal in database
        final String finalGoalType = goalType;
        new Thread(() -> {
            MemorizationGoalEntity existingGoal = goalDao.getActiveGoal();
            TimelineCalculator.TimelineProjection proj = calculator.calculateTimeline();
            
            if (existingGoal == null || !existingGoal.goalType.equals(finalGoalType)) {
                // Deactivate all goals
                goalDao.deactivateAll();
                
                // Create new active goal
                MemorizationGoalEntity newGoal = new MemorizationGoalEntity();
                newGoal.goalType = finalGoalType;
                newGoal.goalName = proj.goalName;
                newGoal.totalAyahs = proj.goalTotalAyahs;
                newGoal.isActive = true;
                newGoal.createdAt = System.currentTimeMillis();
                newGoal.baselineDate = System.currentTimeMillis();
                newGoal.baselineAyahsLearned = proj.learnedAyahs;
                newGoal.projectedDaysToComplete = proj.daysToCompletion;
                goalDao.insert(newGoal);
            } else {
                // Update existing goal's timeline projection
                existingGoal.baselineDate = System.currentTimeMillis();
                existingGoal.baselineAyahsLearned = proj.learnedAyahs;
                existingGoal.projectedDaysToComplete = proj.daysToCompletion;
                goalDao.update(existingGoal);
            }
            
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "Settings saved!", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
    
    private int parseDailyMinutes() {
        try {
            String text = editDailyMinutes.getText() != null ? editDailyMinutes.getText().toString().trim() : "";
            return Integer.parseInt(text);
        } catch (Exception e) {
            return 45;
        }
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
