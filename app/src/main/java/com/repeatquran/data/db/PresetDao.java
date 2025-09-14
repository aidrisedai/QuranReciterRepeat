package com.repeatquran.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PresetEntity e);

    @Update
    int update(PresetEntity e);

    @Delete
    int delete(PresetEntity e);

    @Query("SELECT * FROM preset ORDER BY updatedAt DESC")
    List<PresetEntity> getAllOrdered();
}

