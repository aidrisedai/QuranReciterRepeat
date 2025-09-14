package com.repeatquran.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionEntity.class, PageSegmentEntity.class, PresetEntity.class}, version = 3, exportSchema = false)
public abstract class RepeatQuranDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract PageSegmentDao pageSegmentDao();
    public abstract PresetDao presetDao();

    private static volatile RepeatQuranDatabase INSTANCE;

    public static RepeatQuranDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (RepeatQuranDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), RepeatQuranDatabase.class, "repeat_quran.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
