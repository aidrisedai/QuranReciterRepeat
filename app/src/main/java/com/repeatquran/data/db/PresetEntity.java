package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "preset")
public class PresetEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public String name;
    public String sourceType; // single | range | page | surah

    public Integer page;
    public Integer startSurah;
    public Integer startAyah;
    public Integer endSurah;
    public Integer endAyah;

    public String recitersCsv;
    public int repeatCount; // -1 = ∞

    public long createdAt;
    public long updatedAt;
}

