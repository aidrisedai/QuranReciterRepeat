package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SessionDao {
    @Insert
    long insert(SessionEntity e);

    @Update
    int update(SessionEntity e);

    @Query("UPDATE session SET endedAt = :endedAt, cyclesCompleted = :cyclesCompleted WHERE id = :id")
    int markEnded(long id, long endedAt, Integer cyclesCompleted);

    @Query("SELECT * FROM session ORDER BY startedAt DESC")
    List<SessionEntity> getAllOrdered();

    @Query("SELECT * FROM session ORDER BY startedAt DESC LIMIT :limit")
    List<SessionEntity> getLastN(int limit);
    
    // NEW: Session type specific queries
    @Query("SELECT * FROM session WHERE sessionType = :type ORDER BY startedAt DESC LIMIT :limit")
    List<SessionEntity> getByType(String type, int limit);
    
    @Query("SELECT * FROM session WHERE goalId = :goalId ORDER BY startedAt DESC")
    List<SessionEntity> getByGoal(long goalId);
    
    @Query("SELECT * FROM session WHERE sessionType = :type AND startedAt >= :startTime ORDER BY startedAt DESC")
    List<SessionEntity> getByTypeSince(String type, long startTime);
    
    @Query("SELECT COUNT(*) FROM session WHERE sessionType = :type AND startedAt >= :startTime")
    int countByTypeSince(String type, long startTime);
}
