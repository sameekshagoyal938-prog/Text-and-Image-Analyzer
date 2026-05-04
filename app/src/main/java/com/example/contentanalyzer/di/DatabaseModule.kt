package com.example.contentanalyzer.di

import android.content.Context
import com.example.contentanalyzer.data.local.AppDatabase
import com.example.contentanalyzer.data.local.dao.AnalysisHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): AnalysisHistoryDao {
        return database.historyDao()
    }
}
