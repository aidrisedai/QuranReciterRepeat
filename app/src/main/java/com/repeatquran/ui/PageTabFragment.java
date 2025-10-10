package com.repeatquran.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class PageTabFragment extends Fragment {
    private android.content.BroadcastReceiver playbackBr;
    private boolean isPlaying = false;
    private boolean hasQueue = false;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_page_tab, container, false);
        setupUi(v);
        return v;
    }

    private void setupUi(View root) {
        TextInputLayout pageLayout = root.findViewById(R.id.pageInputLayout);
        TextInputEditText editPage = root.findViewById(R.id.editPage);
        int last = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.page", 1);
        editPage.setText(String.valueOf(last));

        // Half-split now controlled via Settings only

        root.findViewById(R.id.btnPlayPause).setOnClickListener(v -> {
            // Guard against rapid clicks
            android.view.View btn = root.findViewById(R.id.btnPlayPause);
            if (!btn.isEnabled()) return;
            
            // If currently playing, pause
            if (isPlaying) {
                sendService(PlaybackService.ACTION_PAUSE);
                return;
            }
            
            // If has queue but not playing, resume
            if (hasQueue && !isPlaying) {
                sendService(PlaybackService.ACTION_PLAY);
                return;
            }
            
            // Otherwise, load new page
            loadAndPlayPage(root, btn, editPage, pageLayout);
        });

        // Setup broadcast receiver to handle playback state changes
        playbackBr = new android.content.BroadcastReceiver() {
            @Override 
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                android.view.View rootView = getView();
                if (rootView == null) return;
                
                hasQueue = intent.getBooleanExtra("hasQueue", false);
                isPlaying = intent.getBooleanExtra("playing", false);
                
                updatePlayPauseButton(rootView);
            }
        };
        
        // Stop button
        root.findViewById(R.id.btnStop).setOnClickListener(v -> {
            sendService(PlaybackService.ACTION_STOP);
            android.widget.Toast.makeText(requireContext(), "Stopped", android.widget.Toast.LENGTH_SHORT).show();
        });
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
    private int parseIntSafe(TextInputEditText edit) { try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; } }
    
    private void loadAndPlayPage(View root, View btn, TextInputEditText editPage, TextInputLayout pageLayout) {
        clearError(pageLayout);
        int page = parseIntSafe(editPage);
        if (page < 1 || page > 604) { showError(pageLayout, "Enter 1–604"); return; }
        
        // Check for reciter selection before proceeding
        String savedOrder = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getString("reciters.order", "");
        if (savedOrder == null || savedOrder.trim().isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Select at least one reciter first", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).edit().putInt("last.page", page).apply();
        int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
        boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getBoolean("ui.half.split", false);
        
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_LOAD_PAGE);
        intent.putExtra("page", page);
        intent.putExtra("repeat", repeat);
        intent.putExtra("halfSplit", half);
        
        // Disable button immediately and show loading state
        btn.setEnabled(false);
        android.widget.Toast.makeText(requireContext(), "Loading page " + page + "…", android.widget.Toast.LENGTH_SHORT).show();
        
        if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
        
        // Re-enable after shorter delay, but service broadcast will manage state properly
        btn.postDelayed(() -> {
            if (btn.isEnabled() == false) { // Only re-enable if still disabled
                btn.setEnabled(true);
            }
        }, 800); // Reduced from 1200ms
    }
    
    private void updatePlayPauseButton(View rootView) {
        Button playPauseBtn = rootView.findViewById(R.id.btnPlayPause);
        if (playPauseBtn == null) return;
        
        playPauseBtn.setEnabled(true);
        
        if (isPlaying) {
            playPauseBtn.setText("Pause");
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_pause), null, null, null);
        } else if (hasQueue) {
            playPauseBtn.setText("Play");
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_play), null, null, null);
        } else {
            playPauseBtn.setText("Play");
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_play), null, null, null);
        }
    }
}
