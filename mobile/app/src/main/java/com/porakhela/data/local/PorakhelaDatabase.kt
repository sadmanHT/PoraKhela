package com.porakhela.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * Room Database for Porakhela App
 * Offline-first architecture for educational content
 */
@Database(
    entities = [
        UserEntity::class,
        ChildProfileEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        LessonEntity::class,
        LessonProgressEntity::class,
        QuizQuestionEntity::class,
        ChildAvatarEntity::class,
        DownloadEntity::class,
        AppSettingsEntity::class,
        SyncStatusEntity::class,
        SessionActivityEntity::class,
        AchievementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PorakhelaDatabase : RoomDatabase() {
    
    // Core DAOs
    abstract fun userDao(): UserDao
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun lessonDao(): LessonDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun quizQuestionDao(): QuizQuestionDao
    
    // Extended DAOs
    abstract fun childAvatarDao(): ChildAvatarDao
    abstract fun downloadDao(): DownloadDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun sessionActivityDao(): SessionActivityDao
    abstract fun achievementDao(): AchievementDao
    
    companion object {
        @Volatile
        private var INSTANCE: PorakhelaDatabase? = null
        
        fun getDatabase(context: Context): PorakhelaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PorakhelaDatabase::class.java,
                    "porakhela_database"
                )
                .fallbackToDestructiveMigration() // For development only
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}