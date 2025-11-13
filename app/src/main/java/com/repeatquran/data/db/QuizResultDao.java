package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuizResultDao {
    @Insert
    long insert(QuizResultEntity result);
    
    @Query("SELECT * FROM quiz_results WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<QuizResultEntity> getBySession(long sessionId);
    
    @Query("SELECT * FROM quiz_results WHERE sessionId = :sessionId AND wasCorrect = 0")
    List<QuizResultEntity> getIncorrectBySession(long sessionId);
    
    @Query("SELECT * FROM quiz_results WHERE surah = :surah AND ayah = :ayah ORDER BY timestamp DESC LIMIT 10")
    List<QuizResultEntity> getRecentForVerse(int surah, int ayah);
    
    @Query("SELECT COUNT(*) FROM quiz_results WHERE sessionId = :sessionId AND wasCorrect = 1")
    int getCorrectCount(long sessionId);
    
    @Query("SELECT COUNT(*) FROM quiz_results WHERE sessionId = :sessionId")
    int getTotalCount(long sessionId);
    
    @Query("SELECT * FROM quiz_results WHERE wasCorrect = 0 GROUP BY surah, ayah ORDER BY COUNT(*) DESC LIMIT :limit")
    List<QuizResultEntity> getMostProblematicVerses(int limit);
}
