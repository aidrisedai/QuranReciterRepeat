package com.repeatquran.memorization;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.repeatquran.R;
import com.repeatquran.data.db.MemorizationGoalEntity;
import com.repeatquran.util.SurahNames;

import java.util.ArrayList;
import java.util.List;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {
    
    private Context context;
    private List<MemorizationGoalEntity> goals = new ArrayList<>();
    private GoalListener listener;
    
    public interface GoalListener {
        void onContinueClicked(MemorizationGoalEntity goal);
        void onPauseClicked(MemorizationGoalEntity goal);
        void onResumeClicked(MemorizationGoalEntity goal);
        void onDeleteClicked(MemorizationGoalEntity goal);
    }
    
    public GoalsAdapter(Context context, GoalListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    public void setGoals(List<MemorizationGoalEntity> goals) {
        this.goals = goals != null ? goals : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        MemorizationGoalEntity goal = goals.get(position);
        holder.bind(goal);
    }
    
    @Override
    public int getItemCount() {
        return goals.size();
    }
    
    class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView goalTitle;
        TextView goalProgressText;
        LinearProgressIndicator goalProgress;
        TextView strictnessBadge;
        TextView goalTypeBadge;
        TextView daysRemaining;
        MaterialButton continueButton;
        MaterialButton pauseButton;
        ImageButton menuButton;
        View actionButtons;
        
        GoalViewHolder(View itemView) {
            super(itemView);
            goalTitle = itemView.findViewById(R.id.goalTitle);
            goalProgressText = itemView.findViewById(R.id.goalProgressText);
            goalProgress = itemView.findViewById(R.id.goalProgress);
            strictnessBadge = itemView.findViewById(R.id.strictnessBadge);
            goalTypeBadge = itemView.findViewById(R.id.goalTypeBadge);
            daysRemaining = itemView.findViewById(R.id.daysRemaining);
            continueButton = itemView.findViewById(R.id.continueButton);
            pauseButton = itemView.findViewById(R.id.pauseButton);
            menuButton = itemView.findViewById(R.id.goalMenuButton);
            actionButtons = itemView.findViewById(R.id.actionButtons);
        }
        
        void bind(MemorizationGoalEntity goal) {
            // Title
            String title = buildGoalTitle(goal);
            goalTitle.setText(title);
            
            // Progress
            int progress = goal.currentProgress;
            int total = goal.totalVerses;
            goalProgressText.setText(progress + "/" + total);
            goalProgress.setMax(total);
            goalProgress.setProgress(progress);
            
            // Strictness
            String strictnessText = capitalizeFirst(goal.strictnessLevel != null ? goal.strictnessLevel : "moderate");
            strictnessBadge.setText(strictnessText);
            
            // Goal type
            if (goal.versesPerDay != null && goal.versesPerDay > 0) {
                goalTypeBadge.setText(goal.versesPerDay + " verses/day");
            } else {
                goalTypeBadge.setText("One-time");
            }
            
            // Days remaining
            if (goal.targetEndDate != null && goal.versesPerDay != null && goal.versesPerDay > 0) {
                long now = System.currentTimeMillis();
                long remaining = goal.targetEndDate - now;
                int daysLeft = (int) (remaining / (24 * 60 * 60 * 1000));
                
                if (daysLeft > 0) {
                    daysRemaining.setText(daysLeft + " days left");
                } else if (daysLeft == 0) {
                    daysRemaining.setText("Due today");
                } else {
                    daysRemaining.setText("Overdue");
                }
            } else {
                daysRemaining.setVisibility(View.GONE);
            }
            
            // Action buttons
            if (goal.isPaused) {
                actionButtons.setVisibility(View.VISIBLE);
                continueButton.setText("Resume");
                continueButton.setOnClickListener(v -> listener.onResumeClicked(goal));
                pauseButton.setVisibility(View.GONE);
            } else if (goal.isCompleted) {
                actionButtons.setVisibility(View.GONE);
            } else {
                // Active
                actionButtons.setVisibility(View.VISIBLE);
                continueButton.setText("Continue");
                continueButton.setOnClickListener(v -> listener.onContinueClicked(goal));
                pauseButton.setVisibility(View.VISIBLE);
                pauseButton.setOnClickListener(v -> listener.onPauseClicked(goal));
            }
            
            // Menu
            menuButton.setOnClickListener(v -> showMenu(v, goal));
        }
        
        private void showMenu(View anchor, MemorizationGoalEntity goal) {
            PopupMenu popup = new PopupMenu(context, anchor);
            
            if (goal.isPaused) {
                popup.getMenu().add("Resume");
            } else if (!goal.isCompleted) {
                popup.getMenu().add("Pause");
            }
            popup.getMenu().add("Delete");
            
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if ("Pause".equals(title)) {
                    listener.onPauseClicked(goal);
                } else if ("Resume".equals(title)) {
                    listener.onResumeClicked(goal);
                } else if ("Delete".equals(title)) {
                    listener.onDeleteClicked(goal);
                }
                return true;
            });
            
            popup.show();
        }
        
        private String buildGoalTitle(MemorizationGoalEntity goal) {
            if (goal.targetSurahStart == null) return goal.goalText;
            
            if (goal.targetSurahStart.equals(goal.targetSurahEnd)) {
                // Single surah
                String surahName = SurahNames.name(goal.targetSurahStart);
                
                if (goal.targetAyahStart == 1 && goal.targetAyahEnd == com.repeatquran.util.AyahCounts.getCount(goal.targetSurahStart)) {
                    // Whole surah
                    return "Surah " + goal.targetSurahStart + " - " + surahName;
                } else {
                    // Partial
                    return surahName + " (" + goal.targetAyahStart + "-" + goal.targetAyahEnd + ")";
                }
            } else {
                // Multiple surahs
                return "Surahs " + goal.targetSurahStart + "-" + goal.targetSurahEnd;
            }
        }
        
        private String capitalizeFirst(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }
}
