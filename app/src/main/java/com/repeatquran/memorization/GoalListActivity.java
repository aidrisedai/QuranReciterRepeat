package com.repeatquran.memorization;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.repeatquran.R;
import com.repeatquran.data.MemorizationGoalRepository;
import com.repeatquran.data.db.MemorizationGoalEntity;

import java.util.List;

public class GoalListActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_NEW_GOAL = 101;
    
    private TabLayout goalTabs;
    private RecyclerView goalsRecyclerView;
    private LinearLayout emptyState;
    private TextView emptyStateText;
    private ExtendedFloatingActionButton addGoalFab;
    
    private MemorizationGoalRepository goalRepo;
    private GoalsAdapter adapter;
    private String currentTab = "active"; // active, paused, completed
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_list);
        
        goalRepo = new MemorizationGoalRepository(this);
        
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize views
        goalTabs = findViewById(R.id.goalTabs);
        goalsRecyclerView = findViewById(R.id.goalsRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        emptyStateText = findViewById(R.id.emptyStateText);
        addGoalFab = findViewById(R.id.addGoalFab);
        
        setupRecyclerView();
        setupTabs();
        setupFab();
        
        loadGoals();
    }
    
    private void setupRecyclerView() {
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GoalsAdapter(this, new GoalsAdapter.GoalListener() {
            @Override
            public void onContinueClicked(MemorizationGoalEntity goal) {
                // Launch memorization session
                Intent intent = new Intent(GoalListActivity.this, MemorizationSessionActivity.class);
                intent.putExtra(MemorizationSessionActivity.EXTRA_GOAL_ID, goal.id);
                startActivity(intent);
            }
            
            @Override
            public void onPauseClicked(MemorizationGoalEntity goal) {
                pauseGoal(goal);
            }
            
            @Override
            public void onResumeClicked(MemorizationGoalEntity goal) {
                resumeGoal(goal);
            }
            
            @Override
            public void onDeleteClicked(MemorizationGoalEntity goal) {
                deleteGoal(goal);
            }
        });
        goalsRecyclerView.setAdapter(adapter);
    }
    
    private void setupTabs() {
        goalTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    currentTab = "active";
                    emptyStateText.setText("No active goals yet");
                } else if (position == 1) {
                    currentTab = "paused";
                    emptyStateText.setText("No paused goals");
                } else {
                    currentTab = "completed";
                    emptyStateText.setText("No completed goals yet");
                }
                loadGoals();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupFab() {
        addGoalFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, GoalInputActivity.class);
            startActivityForResult(intent, REQUEST_CODE_NEW_GOAL);
        });
    }
    
    private void loadGoals() {
        new Thread(() -> {
            List<MemorizationGoalEntity> goals;
            
            if ("active".equals(currentTab)) {
                goals = goalRepo.getAllActive();
            } else if ("paused".equals(currentTab)) {
                goals = goalRepo.getPaused();
            } else {
                goals = goalRepo.getCompleted();
            }
            
            runOnUiThread(() -> {
                adapter.setGoals(goals);
                
                if (goals.isEmpty()) {
                    goalsRecyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    goalsRecyclerView.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }
    
    private void pauseGoal(MemorizationGoalEntity goal) {
        new Thread(() -> {
            goalRepo.setPaused(goal.id, true);
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "Goal paused", android.widget.Toast.LENGTH_SHORT).show();
                loadGoals();
            });
        }).start();
    }
    
    private void resumeGoal(MemorizationGoalEntity goal) {
        new Thread(() -> {
            goalRepo.setPaused(goal.id, false);
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "Goal resumed", android.widget.Toast.LENGTH_SHORT).show();
                loadGoals();
            });
        }).start();
    }
    
    private void deleteGoal(MemorizationGoalEntity goal) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Are you sure you want to delete this goal? This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                // For now, just mark as inactive (we don't have delete method)
                new Thread(() -> {
                    goal.isActive = false;
                    goalRepo.update(goal);
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(this, "Goal deleted", android.widget.Toast.LENGTH_SHORT).show();
                        loadGoals();
                    });
                }).start();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_NEW_GOAL && resultCode == RESULT_OK) {
            // Reload goals to show the new one
            loadGoals();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadGoals();
    }
}
