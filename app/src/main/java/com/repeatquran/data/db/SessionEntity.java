package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public long startedAt;
    public Long endedAt; // nullable

    public String sourceType; // single | range | provider

    public Integer startSurah; // nullable for provider
    public Integer startAyah;  // nullable for provider
    public Integer endSurah;   // nullable for single/provider
    public Integer endAyah;    // nullable for single/provider

    public String recitersCsv; // comma-joined IDs (saved order)
    public int repeatCount;    // -1 = infinite

    public Integer cyclesRequested; // nullable; for multi-reciter cycles
    public Integer cyclesCompleted; // nullable; set on end
}

