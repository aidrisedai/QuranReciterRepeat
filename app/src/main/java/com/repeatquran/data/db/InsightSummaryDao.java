package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InsightSummaryDao {
    @Insert
    long insert(InsightSummaryEntity summary);
    
    @Update
    void update(InsightSummaryEntity summary);
    
    @Query("SELECT * FROM insight_summaries WHERE id = :id")
    InsightSummaryEntity getById(long id);
    
    @Query("SELECT * FROM insight_summaries ORDER BY createdAt DESC LIMIT :limit")
    List<InsightSummaryEntity> getRecent(int limit);
    
    @Query("SELECT * FROM insight_summaries WHERE summaryType = :type ORDER BY createdAt DESC")
    List<InsightSummaryEntity> getBySummaryType(String type);
    
    @Query("SELECT * FROM insight_summaries WHERE isRead = 0 ORDER BY createdAt DESC")
    List<InsightSummaryEntity> getUnread();
    
    @Query("SELECT * FROM insight_summaries WHERE periodEnd >= :timestamp ORDER BY createdAt DESC LIMIT 1")
    InsightSummaryEntity getLatestAfter(long timestamp);
    
    @Query("UPDATE insight_summaries SET isRead = 1 WHERE id = :id")
    void markAsRead(long id);
}
