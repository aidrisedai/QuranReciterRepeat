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
}

