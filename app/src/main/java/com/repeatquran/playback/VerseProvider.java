package com.repeatquran.playback;

/**
 * Supplies verse audio URLs by index, used for lazy playlist building.
 */
public interface VerseProvider {
    /** Returns the total number of verse audio items available. */
    int size();

    /** Returns the streamable URL for the given index (0-based). */
    String urlAt(int index);
}

