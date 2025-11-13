package com.repeatquran.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionEntity.class, PageSegmentEntity.class, PresetEntity.class, MemorizationUnitEntity.class, MemorizationAttemptEntity.class, MemorizationGoalEntity.class, InsightSummaryEntity.class, VerseProgressEntity.class, QuizResultEntity.class}, version = 8, exportSchema = false)
public abstract class RepeatQuranDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract PageSegmentDao pageSegmentDao();
    public abstract PresetDao presetDao();
    public abstract MemorizationUnitDao memorizationUnitDao();
    public abstract MemorizationAttemptDao memorizationAttemptDao();
    public abstract MemorizationGoalDao memorizationGoalDao();
    public abstract InsightSummaryDao insightSummaryDao();
    public abstract VerseProgressDao verseProgressDao();
    public abstract QuizResultDao quizResultDao();

    private static volatile RepeatQuranDatabase INSTANCE;

    public static RepeatQuranDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (RepeatQuranDatabase.class) {
                if (INSTANCE == null) {
                    RoomDatabase.Builder<RepeatQuranDatabase> builder = Room.databaseBuilder(context.getApplicationContext(), RepeatQuranDatabase.class, "repeat_quran.db")
                            .fallbackToDestructiveMigration();
                    if (android.os.Build.FINGERPRINT != null && android.os.Build.FINGERPRINT.contains("robolectric")) {
                        builder.allowMainThreadQueries();
                    }
                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }
}
