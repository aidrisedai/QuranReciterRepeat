package com.repeatquran.ui.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * ArrayAdapter with search-as-you-type for Surah selection.
 * Matches on:
 *  - surah number ("2", "02", "002")
 *  - English name substring (case-insensitive)
 *  - Full display string substring (e.g., "002 — Al-Baqarah")
 */
public class SurahAutoCompleteAdapter extends ArrayAdapter<String> {
    private final List<String> allItems;
    private final List<String> filteredItems = new ArrayList<>();

    public SurahAutoCompleteAdapter(Context context, int itemLayoutResId, String[] displayItems) {
        super(context, itemLayoutResId, new ArrayList<>());
        this.allItems = new ArrayList<>();
        if (displayItems != null) {
            for (String s : displayItems) this.allItems.add(s);
        }
        // initialize with all
        filteredItems.addAll(this.allItems);
        addAll(filteredItems);
    }

    @Override
    public android.widget.Filter getFilter() {
        return new android.widget.Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    results.values = allItems;
                    results.count = allItems.size();
                } else {
                    String q = normalize(constraint.toString());
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < allItems.size(); i++) {
                        String item = allItems.get(i);
                        if (matches(item, i + 1, q)) list.add(item);
                    }
                    results.values = list;
                    results.count = list.size();
                }
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results != null && results.values != null) {
                    filteredItems.clear();
                    filteredItems.addAll((List<String>) results.values);
                    addAll(filteredItems);
                }
                notifyDataSetChanged();
            }
        };
    }

    private boolean matches(String display, int surahNumber, String q) {
        String disp = normalize(display);
        if (disp.contains(q)) return true;

        // Match number forms
        String n = String.valueOf(surahNumber);        // e.g., 2
        String n2 = (surahNumber < 10 ? "0" : "") + surahNumber; // 02
        String n3 = String.format("%03d", surahNumber); // 002
        return n.startsWith(q) || n2.startsWith(q) || n3.startsWith(q);
    }

    private String normalize(String s) {
        String out = s == null ? "" : s.toLowerCase();
        // Replace various dashes with space to make matching easier
        out = out.replace('\u2014', ' ').replace('\u2013', ' ').replace('-', ' ');
        return out.trim();
    }
}

