package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ChaupalDao
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        PostEntity::class,
        PostImageEntity::class,
        LikeEntity::class,
        CommentEntity::class,
        CommentLikeEntity::class,
        FollowerEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        HashtagEntity::class,
        PostHashtagEntity::class,
        BookmarkEntity::class,
        ReportEntity::class,
        BlockedUserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chaupalDao(): ChaupalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chaupal_social.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
