package com.repeatquran.data.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "page_segments", indices = {@Index(value = {"page", "orderIndex"})})
public class PageSegmentEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;

    public int page;
    public int orderIndex; // 0-based within page
    public int surah;      // 1..114
    public int startAyah;  // >=1
    public int endAyah;    // >= startAyah
}

