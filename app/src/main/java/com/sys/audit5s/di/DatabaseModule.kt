package com.sys.audit5s.di

import android.content.Context
import androidx.room.Room
import com.sys.audit5s.data.db.AppDatabase
import com.sys.audit5s.data.db.dao.AuditDao
import com.sys.audit5s.data.db.dao.QuestionScoreDao
import com.sys.audit5s.data.db.dao.RemarkDao
import com.sys.audit5s.data.db.dao.RemarkPhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "audit5s_database"
        ).build()

    @Provides
    fun provideAuditDao(db: AppDatabase): AuditDao = db.auditDao()

    @Provides
    fun provideQuestionScoreDao(db: AppDatabase): QuestionScoreDao = db.questionScoreDao()

    @Provides
    fun provideRemarkDao(db: AppDatabase): RemarkDao = db.remarkDao()

    @Provides
    fun provideRemarkPhotoDao(db: AppDatabase): RemarkPhotoDao = db.remarkPhotoDao()
}