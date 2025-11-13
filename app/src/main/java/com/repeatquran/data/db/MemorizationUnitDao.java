package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemorizationUnitDao {
    @Insert
    long insert(MemorizationUnitEntity unit);
    
    @Update
    void update(MemorizationUnitEntity unit);
    
    @Query("SELECT * FROM memorization_units WHERE id = :id")
    MemorizationUnitEntity getById(long id);
    
    @Query("SELECT * FROM memorization_units WHERE status = 'new' ORDER BY createdAt ASC LIMIT :limit")
    List<MemorizationUnitEntity> getNewUnits(int limit);
    
    @Query("SELECT * FROM memorization_units WHERE status IN ('learning', 'reviewing') AND nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    List<MemorizationUnitEntity> getDueReviews(long now);
    
    @Query("SELECT * FROM memorization_units WHERE status = 'learned' AND nextReviewAt > :now ORDER BY nextReviewAt ASC")
    List<MemorizationUnitEntity> getFutureReviews(long now);
    
    @Query("SELECT * FROM memorization_units ORDER BY createdAt DESC")
    List<MemorizationUnitEntity> getAll();
    
    @Query("SELECT COUNT(*) FROM memorization_units WHERE status = 'learned'")
    int getLearnedCount();
    
    @Query("SELECT COUNT(*) FROM memorization_units WHERE status = 'new'")
    int getNewCount();
    
    @Query("SELECT COUNT(*) FROM memorization_units WHERE status IN ('learning', 'reviewing') AND nextReviewAt <= :now")
    int getDueCount(long now);
    
    @Query("SELECT * FROM memorization_units WHERE reviewLevel = :level ORDER BY learnedAt DESC")
    List<MemorizationUnitEntity> getUnitsByReviewLevel(int level);
    
    @Query("SELECT * FROM memorization_units WHERE surah = :surah AND status = 'learned'")
    List<MemorizationUnitEntity> getLearnedUnitsForSurah(int surah);
    
    @Query("SELECT * FROM memorization_units WHERE status = 'learned' ORDER BY learnedAt DESC LIMIT :limit")
    List<MemorizationUnitEntity> getRecentlyLearnedUnits(int limit);
    
    @Query("SELECT AVG(successCount * 1.0 / (successCount + failCount)) FROM memorization_units WHERE (successCount + failCount) > 0")
    float getAverageSuccessRate();
    
    @Query("SELECT SUM(endAyah - startAyah + 1) FROM memorization_units WHERE status = 'learned'")
    int getTotalAyahsLearned();
}
