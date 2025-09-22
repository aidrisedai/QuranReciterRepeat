package com.repeatquran.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class SurahTabFragment extends Fragment {
    private android.content.BroadcastReceiver playbackBr;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_surah_tab, container, false);
        setupUi(v);
        return v;
    }

    private void setupUi(View root) {
        AutoCompleteTextView dd = root.findViewById(R.id.surahDropdown);
        TextInputLayout layout = root.findViewById(R.id.surahSelectLayout);

        String[] display = com.repeatquran.util.SurahNames.displayList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, display);
        dd.setAdapter(adapter);
        dd.setThreshold(0);

        int last = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah", 1);
        if (last>=1 && last<=114) dd.setText(com.repeatquran.util.SurahNames.display(last), false);

        // Half-split now controlled via Settings only

        root.findViewById(R.id.btnPlay).setOnClickListener(v -> {
            clearError(layout);
            String txt = dd.getText()!=null?dd.getText().toString().trim():"";
            if (txt.length()<3) { showError(layout, "Select a surah"); return; }
            int surah;
            try { surah = Integer.parseInt(txt.substring(0,3)); } catch (Exception e) { showError(layout, "Select a surah"); return; }
            if (surah<1||surah>114) { showError(layout, "Invalid surah"); return; }
            requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).edit().putInt("last.surah", surah).apply();
            int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
            boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getBoolean("ui.half.split", false);
            Intent intent = new Intent(requireContext(), PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_LOAD_SURAH);
            intent.putExtra("surah", surah);
            intent.putExtra("repeat", repeat);
            intent.putExtra("halfSplit", half);
            if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
            android.widget.Toast.makeText(requireContext(), "Loading surah " + String.format("%03d", surah) + "…", android.widget.Toast.LENGTH_SHORT).show();
            android.view.View btn = root.findViewById(R.id.btnPlay);
            btn.setEnabled(false);
            btn.postDelayed(() -> btn.setEnabled(true), 1200);
        });

        root.findViewById(R.id.btnPause).setOnClickListener(v -> sendService(PlaybackService.ACTION_PAUSE));
        root.findViewById(R.id.btnPause).setOnLongClickListener(v -> {
            sendService(PlaybackService.ACTION_STOP);
            android.widget.Toast.makeText(requireContext(), "Stopped", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });
        playbackBr = new android.content.BroadcastReceiver() {
            @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
                android.view.View rootView = getView();
                if (rootView == null) return;
                boolean hasQueue = intent.getBooleanExtra("hasQueue", false);
                boolean playing = intent.getBooleanExtra("playing", false);
                android.view.View btn = rootView.findViewById(R.id.btnPause);
                if (btn instanceof com.google.android.material.button.MaterialButton) {
                    com.google.android.material.button.MaterialButton b = (com.google.android.material.button.MaterialButton) btn;
                    b.setText(playing ? "Pause" : "Resume");
                    b.setEnabled(hasQueue);
                }
            }
        };
        // Speed next to Play/Pause
        com.google.android.material.button.MaterialButton btnSpeed = root.findViewById(R.id.btnSpeed);
        com.repeatquran.ui.SpeedControlHelper.setup(requireContext(), btnSpeed);
    }

    @Override public void onStart() {
        super.onStart();
        if (playbackBr != null) {
            android.content.IntentFilter f = new android.content.IntentFilter(PlaybackService.ACTION_PLAYBACK_STATE);
            if (android.os.Build.VERSION.SDK_INT >= 33) requireContext().registerReceiver(playbackBr, f, android.content.Context.RECEIVER_NOT_EXPORTED); else requireContext().registerReceiver(playbackBr, f);
        }
    }

    @Override public void onStop() {
        super.onStop();
        if (playbackBr != null) {
            try { requireContext().unregisterReceiver(playbackBr); } catch (Exception ignored) {}
        }
    }

    private void sendService(String action) {
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
    }

    private void showError(TextInputLayout layout, String msg) { layout.setError(msg); }
    private void clearError(TextInputLayout layout) { layout.setError(null); layout.setErrorEnabled(false); }
}
