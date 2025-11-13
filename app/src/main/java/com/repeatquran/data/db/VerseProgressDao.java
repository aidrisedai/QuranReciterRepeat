package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface VerseProgressDao {
    @Insert
    long insert(VerseProgressEntity entity);
    
    // Get total verse count
    @Query("SELECT COUNT(*) FROM verse_progress")
    int getTotalVerseCount();
    
    // Get verse count by activity type
    @Query("SELECT COUNT(*) FROM verse_progress WHERE activityType = :type")
    int getVerseCountByType(String type);
    
    // Get today's verse count
    @Query("SELECT COUNT(*) FROM verse_progress WHERE timestamp >= :startOfDay")
    int getTodayVerseCount(long startOfDay);
    
    // Get this week's verse count
    @Query("SELECT COUNT(*) FROM verse_progress WHERE timestamp >= :startOfWeek")
    int getWeekVerseCount(long startOfWeek);
    
    // Get verse count for specific surah
    @Query("SELECT COUNT(*) FROM verse_progress WHERE surah = :surah")
    int getVerseCountForSurah(int surah);
    
    // Get all progress for a session
    @Query("SELECT * FROM verse_progress WHERE sessionId = :sessionId ORDER BY timestamp")
    List<VerseProgressEntity> getProgressForSession(long sessionId);
    
    // Check if specific verse was listened to
    @Query("SELECT COUNT(*) FROM verse_progress WHERE surah = :surah AND ayah = :ayah AND activityType = :type LIMIT 1")
    int hasListenedToVerse(int surah, int ayah, String type);
}
