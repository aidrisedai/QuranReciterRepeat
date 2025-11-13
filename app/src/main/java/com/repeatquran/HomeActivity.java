package com.repeatquran;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.repeatquran.data.SessionRepository;
import com.repeatquran.data.db.SessionEntity;
import com.repeatquran.memorization.MemorizationActivity;
import com.repeatquran.settings.SettingsActivity;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    
    private SessionRepository sessionRepository;
    private TextView greetingText;
    private TextView versesListenedCount, versesTodayCount, versesWeekCount;
    private TextView listeningTimeCount, timeTodayCount, timeWeekCount;
    private MaterialCardView memorizationSessionCard;
    private MaterialCardView readingSessionCard;
    private MaterialCardView recitationPracticeCard;
    private android.content.BroadcastReceiver sessionEndedReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check onboarding
        boolean seen = getSharedPreferences("rq_prefs", MODE_PRIVATE).getBoolean("onboarding.seen", false);
        if (!seen) {
            Intent i = new Intent(this, com.repeatquran.onboarding.OnboardingActivity.class);
            startActivity(i);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main_redesign);
        
        sessionRepository = new SessionRepository(this);
        
        // Initialize views
        greetingText = findViewById(R.id.greetingText);
        versesListenedCount = findViewById(R.id.versesListenedCount);
        versesTodayCount = findViewById(R.id.versesTodayCount);
        versesWeekCount = findViewById(R.id.versesWeekCount);
        listeningTimeCount = findViewById(R.id.listeningTimeCount);
        timeTodayCount = findViewById(R.id.timeTodayCount);
        timeWeekCount = findViewById(R.id.timeWeekCount);
        memorizationSessionCard = findViewById(R.id.memorizationSessionCard);
        readingSessionCard = findViewById(R.id.readingSessionCard);
        recitationPracticeCard = findViewById(R.id.recitationPracticeCard);
        
        // Set greeting based on time of day
        setupGreeting();
        
        // Load listening stats
        loadListeningStats();
        
        // Setup streak tracker
        setupStreakTracker();
        
        // Setup session card clicks
        setupSessionCards();
        
        // Setup bottom navigation
        setupBottomNavigation();
        
        // Setup real-time verse tracking listener
        setupVerseTrackingListener();
        
        // Analytics
        com.repeatquran.analytics.AnalyticsLogger.get(this).log("home_opened", java.util.Collections.emptyMap());
    }
    
    private android.content.BroadcastReceiver verseTrackedReceiver;
    
    private void setupVerseTrackingListener() {
        verseTrackedReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                if ("com.repeatquran.action.VERSE_TRACKED".equals(intent.getAction())) {
                    int surah = intent.getIntExtra("surah", 0);
                    int ayah = intent.getIntExtra("ayah", 0);
                    Log.d("HomeActivity", "Verse tracked broadcast received: " + surah + ":" + ayah);
                    
                    // Refresh stats in real-time
                    loadListeningStats();
                }
            }
        };
        
        android.content.IntentFilter filter = new android.content.IntentFilter("com.repeatquran.action.VERSE_TRACKED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(verseTrackedReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(verseTrackedReceiver, filter);
        }
    }
    
    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        String[] morningGreetings = {
            "Sabah al-khair! ☀️",
            "Rise and shine! 🌅",
            "Good Morning! 🌤️",
            "Blessed morning to you! ✨"
        };
        
        String[] afternoonGreetings = {
            "Masa' al-khair! ☀️",
            "Good Afternoon! 🌞",
            "Hope you're having a blessed day! 💫",
            "Great to see you! 🌟"
        };
        
        String[] eveningGreetings = {
            "Masa' al-khair! 🌙",
            "Good Evening! ⭐",
            "Blessed evening to you! 🌃",
            "Peace be upon you! 🌙"
        };
        
        String[] nightGreetings = {
            "Tisbah 'ala khair! 🌙",
            "Good Night! ✨",
            "May your night be blessed! 💤",
            "Rest well! 🌠"
        };
        
        // Get day of year for consistent but varied greeting
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        
        if (hourOfDay >= 5 && hourOfDay < 12) {
            greeting = morningGreetings[dayOfYear % morningGreetings.length];
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            greeting = afternoonGreetings[dayOfYear % afternoonGreetings.length];
        } else if (hourOfDay >= 17 && hourOfDay < 21) {
            greeting = eveningGreetings[dayOfYear % eveningGreetings.length];
        } else {
            greeting = nightGreetings[dayOfYear % nightGreetings.length];
        }
        
        greetingText.setText(greeting);
    }
    
    private void loadListeningStats() {
        // Load listening stats in background using new verse progress table
        new Thread(() -> {
            com.repeatquran.data.db.VerseProgressDao verseDao = 
                com.repeatquran.data.db.RepeatQuranDatabase.get(this).verseProgressDao();
            
            // Get real-time verse counts
            int totalVerses = verseDao.getTotalVerseCount();
            Log.d("HomeActivity", "Total verses from progress table: " + totalVerses);
            
            // Calculate listening time from sessions (still use sessions for time tracking)
            List<SessionEntity> allSessions = sessionRepository.getLastSessions(10000);
            long totalListeningTimeMs = 0;
            long currentTime = System.currentTimeMillis();
            
            for (SessionEntity session : allSessions) {
                // Calculate listening time
                if (session.startedAt > 0) {
                    // If session ended, use endedAt; otherwise use current time (for active sessions)
                    long endTime = session.endedAt != null ? session.endedAt : currentTime;
                    long duration = endTime - session.startedAt;
                    
                    // Only count reasonable durations (max 24 hours per session)
                    if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                        totalListeningTimeMs += duration;
                    }
                }
            }
            
            Log.d("HomeActivity", "Total verses: " + totalVerses + ", Total time: " + (totalListeningTimeMs / 60000) + " min");
            
            // Calculate today's stats
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long todayStart = today.getTimeInMillis();
            
            // Calculate this week's stats (Sunday to Saturday)
            Calendar weekStart = Calendar.getInstance();
            weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            weekStart.set(Calendar.HOUR_OF_DAY, 0);
            weekStart.set(Calendar.MINUTE, 0);
            weekStart.set(Calendar.SECOND, 0);
            weekStart.set(Calendar.MILLISECOND, 0);
            long weekStartMs = weekStart.getTimeInMillis();
            
            // Get verse counts from verse progress table
            int todayVerses = verseDao.getTodayVerseCount(todayStart);
            int weekVerses = verseDao.getWeekVerseCount(weekStartMs);
            
            long todayListeningTimeMs = 0;
            long weekListeningTimeMs = 0;
            
            // Get listening time from sessions
            for (SessionEntity session : allSessions) {
                long sessionDuration = 0;
                
                if (session.startedAt > 0) {
                    // If session ended, use endedAt; otherwise use current time (for active sessions)
                    long endTime = session.endedAt != null ? session.endedAt : currentTime;
                    sessionDuration = endTime - session.startedAt;
                    
                    // Only count reasonable durations (max 24 hours per session)
                    if (sessionDuration < 0 || sessionDuration >= 24 * 60 * 60 * 1000) {
                        sessionDuration = 0;
                    }
                }
                
                // Today's time
                if (session.startedAt >= todayStart) {
                    todayListeningTimeMs += sessionDuration;
                }
                
                // This week's time
                if (session.startedAt >= weekStartMs) {
                    weekListeningTimeMs += sessionDuration;
                }
            }
            
            final int finalTotalVerses = totalVerses;
            final long finalTotalTimeMs = totalListeningTimeMs;
            final int finalTodayVerses = todayVerses;
            final long finalTodayTimeMs = todayListeningTimeMs;
            final int finalWeekVerses = weekVerses;
            final long finalWeekTimeMs = weekListeningTimeMs;
            
            runOnUiThread(() -> {
                Log.d("HomeActivity", "Updating UI - Today: " + finalTodayVerses + ", Week: " + finalWeekVerses + ", Total: " + finalTotalVerses);
                
                // Update verses
                versesTodayCount.setText(String.valueOf(finalTodayVerses));
                versesWeekCount.setText(String.valueOf(finalWeekVerses));
                versesListenedCount.setText(String.valueOf(finalTotalVerses));
                
                // Format today's time
                timeTodayCount.setText(formatTime(finalTodayTimeMs, true));
                
                // Format week's time
                timeWeekCount.setText(formatTime(finalWeekTimeMs, false));
                
                // Format all-time
                listeningTimeCount.setText(formatTime(finalTotalTimeMs, false));
                
                // Store today's stats for easy access
                getSharedPreferences("rq_prefs", MODE_PRIVATE).edit()
                    .putInt("today_verses", finalTodayVerses)
                    .putInt("today_minutes", (int)(finalTodayTimeMs / (1000 * 60)))
                    .putInt("total_verses", finalTotalVerses)
                    .putInt("total_minutes", (int)(finalTotalTimeMs / (1000 * 60)))
                    .apply();
            });
        }).start();
    }
    private int calculateVersesInSession(SessionEntity session) {
        if (session.startSurah == null || session.startAyah == null) {
            return 0;
        }
        
        // Calculate base verse count for the selection
        int baseVerses = 0;
        
        if ("single".equals(session.sourceType)) {
            // Single verse
            baseVerses = 1;
        } else if ("range".equals(session.sourceType) && session.endSurah != null && session.endAyah != null) {
            // Range of verses
            if (session.startSurah.equals(session.endSurah)) {
                // Same surah
                baseVerses = session.endAyah - session.startAyah + 1;
            } else {
                // Multiple surahs
                baseVerses = getAyahCount(session.startSurah) - session.startAyah + 1;
                for (int s = session.startSurah + 1; s < session.endSurah; s++) {
                    baseVerses += getAyahCount(s);
                }
                baseVerses += session.endAyah;
            }
        } else if ("surah".equals(session.sourceType)) {
            // Full surah
            baseVerses = getAyahCount(session.startSurah);
        } else if ("page".equals(session.sourceType)) {
            // Page - approximate 15 verses per page
            baseVerses = 15;
        }
        
        // Use cyclesCompleted if available (actual playback), otherwise use repeatCount (requested)
        int effectiveCycles = 1;
        if (session.cyclesCompleted != null && session.cyclesCompleted > 0) {
            // Use actual completed cycles
            effectiveCycles = session.cyclesCompleted;
            Log.d("HomeActivity", "Session " + session.id + ": Using cyclesCompleted=" + effectiveCycles);
        } else if (session.repeatCount > 0) {
            // Fallback to requested count (for old sessions or if not yet completed)
            effectiveCycles = session.repeatCount;
            Log.d("HomeActivity", "Session " + session.id + ": Using repeatCount=" + effectiveCycles);
        }
        
        // For multi-reciter sessions, multiply by number of reciters
        int reciterMultiplier = 1;
        if (session.recitersCsv != null && !session.recitersCsv.isEmpty()) {
            String[] reciters = session.recitersCsv.split(",");
            reciterMultiplier = reciters.length;
        }
        
        int totalVerses = baseVerses * effectiveCycles * reciterMultiplier;
        Log.d("HomeActivity", "Session " + session.id + ": " + baseVerses + " verses × " + effectiveCycles + " cycles × " + reciterMultiplier + " reciters = " + totalVerses);
        
        return totalVerses;
    }
    
    private int getAyahCount(int surah) {
        int[] AYAH_COUNTS = new int[] {
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
            123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
            54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
            60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
            28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
            15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
            11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
            5, 4, 5, 6
        };
        
        if (surah < 1 || surah > 114) return 0;
        return AYAH_COUNTS[surah - 1];
    }
    
    private String formatTime(long timeMs, boolean compact) {
        long totalMinutes = timeMs / (1000 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        if (compact) {
            // Compact format for today (shorter)
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            } else if (minutes > 0) {
                return minutes + "m";
            } else {
                return "0m";
            }
        } else {
            // Full format for week/all-time
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            } else if (minutes > 0) {
                return minutes + "m";
            } else {
                return "0m";
            }
        }
    }
    
    private void setupStreakTracker() {
        // Load streak data from SessionRepository
        new Thread(() -> {
            List<SessionEntity> recentSessions = sessionRepository.getLastSessions(100);
            Log.d("HomeActivity", "Checking streak from " + recentSessions.size() + " recent sessions");
            
            // Calculate which days this week had sessions
            Calendar calendar = Calendar.getInstance();
            boolean[] weekCompleted = new boolean[7]; // Sun-Sat (0=Sun, 6=Sat)
            
            // Get start of this week (Sunday at midnight)
            Calendar weekStart = Calendar.getInstance();
            weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            weekStart.set(Calendar.HOUR_OF_DAY, 0);
            weekStart.set(Calendar.MINUTE, 0);
            weekStart.set(Calendar.SECOND, 0);
            weekStart.set(Calendar.MILLISECOND, 0);
            long weekStartMs = weekStart.getTimeInMillis();
            
            Log.d("HomeActivity", "Week starts at: " + new Date(weekStartMs));
            
            for (SessionEntity session : recentSessions) {
                if (session.startedAt > 0 && session.startedAt >= weekStartMs) {
                    Calendar sessionCal = Calendar.getInstance();
                    sessionCal.setTimeInMillis(session.startedAt);
                    
                    int dayOfWeek = sessionCal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 7=Sat
                    weekCompleted[dayOfWeek - 1] = true;
                    
                    Log.d("HomeActivity", "Session on " + new SimpleDateFormat("EEE", Locale.ENGLISH).format(sessionCal.getTime()) + ": " + session.sourceType);
                }
            }
            
            Log.d("HomeActivity", "Week completed: Sun=" + weekCompleted[0] + ", Mon=" + weekCompleted[1] + ", Tue=" + weekCompleted[2] + ", Wed=" + weekCompleted[3] + ", Thu=" + weekCompleted[4] + ", Fri=" + weekCompleted[5] + ", Sat=" + weekCompleted[6]);
            
            runOnUiThread(() -> {
                // Update UI with streak data
                // Map Sunday=0 to actual UI order (M,T,W,T,F,S,S)
                int[] dayMapping = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, 
                                   Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
                int[] checkIds = {R.id.streakCheckMon, R.id.streakCheckTue, R.id.streakCheckWed,
                                 R.id.streakCheckThu, R.id.streakCheckFri, R.id.streakCheckSat, R.id.streakCheckSun};
                
                for (int i = 0; i < dayMapping.length; i++) {
                    TextView checkView = findViewById(checkIds[i]);
                    if (checkView != null) {
                        boolean completed = weekCompleted[dayMapping[i] - 1];
                        if (completed) {
                            checkView.setBackgroundResource(R.drawable.streak_day_complete);
                            checkView.setText("✓");
                            checkView.setTextColor(0xFFFFFFFF);
                        } else {
                            checkView.setBackgroundResource(R.drawable.streak_day_incomplete);
                            checkView.setText("");
                        }
                    }
                }
            });
        }).start();
    }
    
    private void setupSessionCards() {
        // Memorization card -> Opens the new goal management system
        memorizationSessionCard.setOnClickListener(v -> {
            // Show dialog to choose between creating new goal or viewing goals
            new android.app.AlertDialog.Builder(this)
                .setTitle("Memorization")
                .setItems(new CharSequence[]{"Create New Goal", "View My Goals", "[Old] Legacy Mode"}, (dialog, which) -> {
                    Intent intent;
                    switch (which) {
                        case 0:
                            // Create new goal
                            intent = new Intent(this, com.repeatquran.memorization.GoalInputActivity.class);
                            startActivity(intent);
                            break;
                        case 1:
                            // View goals
                            intent = new Intent(this, com.repeatquran.memorization.GoalListActivity.class);
                            startActivity(intent);
                            break;
                        case 2:
                            // Legacy memorization activity
                            intent = new Intent(this, MemorizationActivity.class);
                            startActivity(intent);
                            break;
                    }
                })
                .show();
            
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("source", "home");
            com.repeatquran.analytics.AnalyticsLogger.get(this).log("memorization_opened", ev);
        });
        
        // Reading card -> Opens the original MainActivity with tabs
        readingSessionCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("source", "home");
            com.repeatquran.analytics.AnalyticsLogger.get(this).log("reading_opened", ev);
        });
        
        // Recitation Practice card -> Opens RecitationRecorderActivity
        recitationPracticeCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.repeatquran.recitation.RecitationRecorderActivity.class);
            startActivity(intent);
            
            java.util.Map<String, Object> ev = new java.util.HashMap<>();
            ev.put("source", "home");
            com.repeatquran.analytics.AnalyticsLogger.get(this).log("recitation_practice_opened", ev);
        });
        
        // See all sessions -> Opens MainActivity
        findViewById(R.id.seeAllSessions).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_learn) {
                // Open main activity with tabs
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_progress) {
                // Open memorization activity (which shows progress)
                Intent intent = new Intent(this, MemorizationActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Open settings
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            
            return false;
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to home
        loadListeningStats();
        setupStreakTracker();
        
        // Register broadcast receiver for session updates
        if (sessionEndedReceiver == null) {
            sessionEndedReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    Log.d("HomeActivity", "Session ended broadcast received - refreshing stats");
                    // Refresh stats when a session ends
                    loadListeningStats();
                    setupStreakTracker();
                }
            };
        }
        
        android.content.IntentFilter filter = new android.content.IntentFilter(com.repeatquran.playback.PlaybackService.ACTION_SESSION_ENDED);
        androidx.core.content.ContextCompat.registerReceiver(this, sessionEndedReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        if (sessionEndedReceiver != null) {
            try {
                unregisterReceiver(sessionEndedReceiver);
            } catch (Exception ignored) {}
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister verse tracking receiver
        if (verseTrackedReceiver != null) {
            try {
                unregisterReceiver(verseTrackedReceiver);
            } catch (Exception ignored) {}
        }
    }
}
