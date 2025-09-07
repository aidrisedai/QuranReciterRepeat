package com.repeatquran.playback;

/**
 * VerseProvider for a single ayah from everyayah.com.
 */
public class SingleVerseProvider implements VerseProvider {
    private final String reciterPath; // e.g., Abdurrahmaan_As-Sudais_64kbps
    private final int surah; // 1..114
    private final int ayah;  // 1..max of surah (not validated here)

    public SingleVerseProvider(String reciterPath, int surah, int ayah) {
        this.reciterPath = reciterPath;
        this.surah = surah;
        this.ayah = ayah;
    }

    @Override
    public int size() { return 1; }

    @Override
    public String urlAt(int index) {
        if (index != 0) throw new IndexOutOfBoundsException("SingleVerseProvider only has index 0");
        String sss = String.format("%03d", surah);
        String aaa = String.format("%03d", ayah);
        return "https://everyayah.com/data/" + reciterPath + "/" + sss + aaa + ".mp3";
    }
}

