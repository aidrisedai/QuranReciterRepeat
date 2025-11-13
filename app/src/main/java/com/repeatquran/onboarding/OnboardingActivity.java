package com.repeatquran.onboarding;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.repeatquran.MainActivity;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class OnboardingActivity extends AppCompatActivity {
    private View stepWelcome;
    private View stepArabic;
    private View stepTime;
    private View stepTest;
    private View stepResult;

    private RadioButton radioArabicYes;
    private RadioButton radioArabicNo;
    private SeekBar timeSeek;
    private TextView timeValue;

    private TextView testStatus;
    private MaterialButton btnStartTest;
    private MaterialButton btnTestYes;
    private MaterialButton btnTestNo;

    private TextView planSummary;
    private MaterialButton btnFinish;

    private int testPhase = 0; // 0=idle, 1=slow(5), 2=fast(10), 3=done
    private int repetitionsPlanned = 0;
    private boolean testPassed = false;

    private BroadcastReceiver stateReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        bindViews();
        wireWelcome();
        wireArabic();
        wireTime();
        wireTest();
        wireResult();

        show(stepWelcome);
    }

    private void bindViews() {
        stepWelcome = findViewById(R.id.stepWelcome);
        stepArabic = findViewById(R.id.stepArabic);
        stepTime = findViewById(R.id.stepTime);
        stepTest = findViewById(R.id.stepTest);
        stepResult = findViewById(R.id.stepResult);

        radioArabicYes = findViewById(R.id.radioArabicYes);
        radioArabicNo = findViewById(R.id.radioArabicNo);
        timeSeek = findViewById(R.id.seekDailyTime);
        timeValue = findViewById(R.id.txtDailyTimeValue);

        testStatus = findViewById(R.id.txtTestStatus);
        btnStartTest = findViewById(R.id.btnStartTest);
        btnTestYes = findViewById(R.id.btnTestYes);
        btnTestNo = findViewById(R.id.btnTestNo);

        planSummary = findViewById(R.id.txtPlanSummary);
        btnFinish = findViewById(R.id.btnFinish);
    }

    private void wireWelcome() {
        MaterialButton btn = findViewById(R.id.btnGetStarted);
        btn.setOnClickListener(v -> {
            show(stepArabic);
        });
    }

    private void wireArabic() {
        MaterialButton next = findViewById(R.id.btnArabicNext);
        next.setOnClickListener(v -> {
            boolean isArabic = radioArabicYes != null && radioArabicYes.isChecked();
            getSharedPreferences("rq_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding.is_arabic_speaker", isArabic)
                    .apply();
            show(stepTime);
        });
    }

    private void wireTime() {
        // Min 30, Max 180
        timeSeek.setMax(150); // 30..180 -> offset by 30
        int saved = Math.max(30, getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("onboarding.preferred_daily_minutes", 45));
        timeSeek.setProgress(saved - 30);
        updateTimeLabel(saved);
        timeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateTimeLabel(progress + 30); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        MaterialButton next = findViewById(R.id.btnTimeNext);
        next.setOnClickListener(v -> {
            int minutes = timeSeek.getProgress() + 30;
            if (minutes < 30) minutes = 30;
            getSharedPreferences("rq_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("onboarding.preferred_daily_minutes", minutes)
                    .apply();
            ensureDefaultReciter();
            show(stepTest);
        });
    }

    private void wireTest() {
        btnStartTest.setOnClickListener(v -> startAssessment());
        btnTestYes.setOnClickListener(v -> { testPassed = true; finishAssessment(); });
        btnTestNo.setOnClickListener(v -> { testPassed = false; finishAssessment(); });
    }

    private void wireResult() {
        btnFinish.setOnClickListener(v -> {
            getSharedPreferences("rq_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding.seen", true)
                    .apply();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void updateTimeLabel(int minutes) {
        if (timeValue != null) timeValue.setText(minutes + " min/day");
    }

    private void show(View step) {
        stepWelcome.setVisibility(step == stepWelcome ? View.VISIBLE : View.GONE);
        stepArabic.setVisibility(step == stepArabic ? View.VISIBLE : View.GONE);
        stepTime.setVisibility(step == stepTime ? View.VISIBLE : View.GONE);
        stepTest.setVisibility(step == stepTest ? View.VISIBLE : View.GONE);
        stepResult.setVisibility(step == stepResult ? View.VISIBLE : View.GONE);
    }

    private void startAssessment() {
        testPhase = 1; // slow first
        repetitionsPlanned = 0;
        testStatus.setText("Playing test: slow × 5…");
        // Set slow speed and play S1:A1 five times
        sendServiceAction(PlaybackService.ACTION_SET_SPEED, 0.75f);
        playSingle(1, 1, 5);
        repetitionsPlanned += 5;
        ensureReceiver();
    }

    private void continueAssessmentFast() {
        testPhase = 2;
        testStatus.setText("Playing test: fast × 10…");
        sendServiceAction(PlaybackService.ACTION_SET_SPEED, 1.5f);
        playSingle(1, 1, 10);
        repetitionsPlanned += 10;
    }

    private void finishAssessment() {
        // Persist results
        int minutes = Math.max(30, getSharedPreferences("rq_prefs", MODE_PRIVATE).getInt("onboarding.preferred_daily_minutes", 45));
        float efficiency = testPassed ? 0.7f : 0.4f; // initial heuristic
        String memSpeed = testPassed ? "normal" : "foundational";

        // Plan split
        int newMin = Math.round(minutes * 0.5f);
        int recentMin = Math.round(minutes * 0.3f);
        int oldMin = Math.max(1, minutes - newMin - recentMin);

        getSharedPreferences("rq_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding.test.pass", testPassed)
                .putInt("onboarding.test.repetitions", repetitionsPlanned)
                .putString("profile.memorization_speed", memSpeed)
                .putFloat("profile.initial_efficiency_score", efficiency)
                .putInt("plan.daily.total", minutes)
                .putInt("plan.daily.new_minutes", newMin)
                .putInt("plan.daily.recent_minutes", recentMin)
                .putInt("plan.daily.old_minutes", oldMin)
                .putInt("plan.start.surah", 1)
                .putInt("plan.start.ayah", 1)
                .apply();

        String plan = "Daily plan (" + minutes + " min):\n" +
                "• New memorization: " + newMin + " min\n" +
                "• Recent review: " + recentMin + " min\n" +
                "• Old review: " + oldMin + " min\n\n" +
                "Suggested start: Surah 001 — Al-Fatihah : Ayah 1";
        planSummary.setText(plan);
        show(stepResult);
    }

    private void ensureDefaultReciter() {
        String csv = getSharedPreferences("rq_prefs", MODE_PRIVATE).getString("reciters.order", "");
        if (TextUtils.isEmpty(csv)) {
            // Default to first reciter in arrays
            String[] ids = getResources().getStringArray(R.array.reciter_ids);
            if (ids != null && ids.length > 0) {
                getSharedPreferences("rq_prefs", MODE_PRIVATE).edit().putString("reciters.order", ids[0]).apply();
            }
        }
    }

    private void playSingle(int sura, int ayah, int repeat) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_SINGLE);
        intent.putExtra("sura", sura);
        intent.putExtra("ayah", ayah);
        intent.putExtra("repeat", repeat);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void sendServiceAction(String action, float speed) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        intent.putExtra("speed", speed);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
    }

    private void ensureReceiver() {
        if (stateReceiver != null) return;
        stateReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!PlaybackService.ACTION_PLAYBACK_STATE.equals(intent.getAction())) return;
                boolean playing = intent.getBooleanExtra("playing", false);
                int state = intent.getIntExtra("state", 1);
                // Player.STATE_ENDED == 4 in ExoPlayer
                if (!playing && state == 4) {
                    if (testPhase == 1) {
                        continueAssessmentFast();
                    } else if (testPhase == 2) {
                        testPhase = 3;
                        testStatus.setText("Test finished. Can you repeat this Aya from memory?");
                        // Show Yes/No
                        findViewById(R.id.groupTestAnswer).setVisibility(View.VISIBLE);
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
            try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
        }
    }
}

