package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PageSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PageSegmentEntity> list);

    @Query("SELECT * FROM page_segments WHERE page = :page ORDER BY orderIndex ASC")
    List<PageSegmentEntity> segmentsForPage(int page);

    @Query("SELECT COUNT(*) FROM page_segments")
    int countAll();
}

