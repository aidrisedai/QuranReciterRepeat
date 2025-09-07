package com.repeatquran.playback;

import java.util.Arrays;
import java.util.List;

/**
 * Simple in-memory provider with placeholder MP3 URLs for demo.
 * Replace with real Quran verse URLs during integration.
 */
public class SimpleVerseProvider implements VerseProvider {
    private final List<String> urls = Arrays.asList(
            // Al-Fatiha (001:1–7) from everyayah.com — Abdurrahmaan As-Sudais 64kbps
            // Pattern: https://everyayah.com/data/<RECITER>/<SSSAAA>.mp3
            // Where SSS = surah (001) and AAA = ayah (001..nnn), zero-padded to 3 digits
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001001.mp3",
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001002.mp3",
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001003.mp3",
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001004.mp3",
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001005.mp3",
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001006.mp3",
            "https://everyayah.com/data/Abdurrahmaan_As-Sudais_64kbps/001007.mp3"
    );

    @Override
    public int size() {
        return urls.size();
    }

    @Override
    public String urlAt(int index) {
        return urls.get(index);
    }
}
