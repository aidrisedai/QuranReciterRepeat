package com.repeatquran.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ModesPagerAdapter extends FragmentStateAdapter {
    public ModesPagerAdapter(@NonNull FragmentActivity fa) { super(fa); }

    @NonNull @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new VerseTabFragment();
            case 1: return new RangeTabFragment();
            case 2: return new PageTabFragment();
            case 3: return new SurahTabFragment();
        }
        return new VerseTabFragment();
    }

    @Override public int getItemCount() { return 4; }
}

