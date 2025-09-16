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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.repeatquran.R;
import com.repeatquran.playback.PlaybackService;

public class RangeTabFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_range_tab, container, false);
        setupUi(v);
        return v;
    }

    private void setupUi(View root) {
        AutoCompleteTextView ddStart = root.findViewById(R.id.startSurahDropdown);
        AutoCompleteTextView ddEnd = root.findViewById(R.id.endSurahDropdown);
        TextInputLayout startSurahLayout = root.findViewById(R.id.startSurahLayout);
        TextInputLayout endSurahLayout = root.findViewById(R.id.endSurahLayout);
        TextInputLayout startAyahLayout = root.findViewById(R.id.startAyahLayout);
        TextInputLayout endAyahLayout = root.findViewById(R.id.endAyahLayout);
        TextInputEditText editStartAyah = root.findViewById(R.id.editStartAyah);
        TextInputEditText editEndAyah = root.findViewById(R.id.editEndAyah);

        String[] nums = requireContext().getResources().getStringArray(R.array.surah_numbers);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, nums);
        ddStart.setAdapter(adapter); ddStart.setThreshold(0);
        ddEnd.setAdapter(adapter); ddEnd.setThreshold(0);

        int lastStart = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah.range.start", 1);
        int lastEnd = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("last.surah.range.end", 1);
        if (lastStart>=1 && lastStart<=114) ddStart.setText(String.format("%03d", lastStart), false);
        if (lastEnd>=1 && lastEnd<=114) ddEnd.setText(String.format("%03d", lastEnd), false);

        startAyahLayout.setHelperText("Max ayah: " + getAyahCount(Math.max(1, Math.min(114, lastStart))));
        endAyahLayout.setHelperText("Max ayah: " + getAyahCount(Math.max(1, Math.min(114, lastEnd))));
        ddStart.setOnItemClickListener((p, v, pos, id) -> startAyahLayout.setHelperText("Max ayah: " + getAyahCount(pos + 1)));
        ddEnd.setOnItemClickListener((p, v, pos, id) -> endAyahLayout.setHelperText("Max ayah: " + getAyahCount(pos + 1)));

        // Half-split now controlled via Settings only

        root.findViewById(R.id.btnPlay).setOnClickListener(v -> {
            clearError(startSurahLayout); clearError(endSurahLayout); clearError(startAyahLayout); clearError(endAyahLayout);
            String s1 = ddStart.getText()!=null?ddStart.getText().toString().trim():"";
            String s2 = ddEnd.getText()!=null?ddEnd.getText().toString().trim():"";
            if (s1.length()<3) { showError(startSurahLayout, "Select start"); return; }
            if (s2.length()<3) { showError(endSurahLayout, "Select end"); return; }
            int ss, es;
            try { ss = Integer.parseInt(s1.substring(0,3)); } catch (Exception e) { showError(startSurahLayout, "Select start"); return; }
            try { es = Integer.parseInt(s2.substring(0,3)); } catch (Exception e) { showError(endSurahLayout, "Select end"); return; }
            if (ss<1||ss>114) { showError(startSurahLayout, "1..114"); return; }
            if (es<1||es>114) { showError(endSurahLayout, "1..114"); return; }
            int sa = parseIntSafe(editStartAyah);
            int ea = parseIntSafe(editEndAyah);
            if (sa<1||sa>getAyahCount(ss)) { showError(startAyahLayout, "Ayah 1.."+getAyahCount(ss)); return; }
            if (ea<1||ea>getAyahCount(es)) { showError(endAyahLayout, "Ayah 1.."+getAyahCount(es)); return; }
            if (!isStartBeforeOrEqual(ss, sa, es, ea)) { showError(endSurahLayout, "End before start"); showError(endAyahLayout, "End before start"); return; }

            requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).edit()
                    .putInt("last.surah.range.start", ss)
                    .putInt("last.surah.range.end", es)
                    .apply();

            int repeat = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getInt("repeat.count", 1);
            boolean half = requireContext().getSharedPreferences("rq_prefs", requireContext().MODE_PRIVATE).getBoolean("ui.half.split", false);

            Intent intent = new Intent(requireContext(), PlaybackService.class);
            intent.setAction(PlaybackService.ACTION_LOAD_RANGE);
            intent.putExtra("ss", ss);
            intent.putExtra("sa", sa);
            intent.putExtra("es", es);
            intent.putExtra("ea", ea);
            intent.putExtra("repeat", repeat);
            intent.putExtra("halfSplit", half);
            if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
        });

        // Pause/Resume moved to global toolbar control
    }

    private void sendService(String action) {
        Intent intent = new Intent(requireContext(), PlaybackService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= 26) requireContext().startForegroundService(intent); else requireContext().startService(intent);
    }

    private boolean isStartBeforeOrEqual(int ss, int sa, int es, int ea) {
        if (ss < es) return true; if (ss > es) return false; return sa <= ea;
    }
    private void showError(TextInputLayout layout, String msg) { layout.setError(msg); }
    private void clearError(TextInputLayout layout) { layout.setError(null); layout.setErrorEnabled(false); }
    private int parseIntSafe(TextInputEditText edit) { try { return Integer.parseInt(edit.getText()==null?"":edit.getText().toString().trim()); } catch (Exception e) { return -1; } }
    private int getAyahCount(int surah) {
        final int[] AYAH_COUNTS = new int[] {
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
        return AYAH_COUNTS[surah - 1];
    }
}
