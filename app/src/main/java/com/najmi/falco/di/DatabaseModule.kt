package com.najmi.falco.di

import android.content.Context
import androidx.room.Room
import com.najmi.falco.data.local.FalcoDatabase
import com.najmi.falco.data.local.dao.ClaimDao
import com.najmi.falco.data.local.dao.PaperStanceDao
import com.najmi.falco.data.local.dao.QuotaDao
import com.najmi.falco.data.local.dao.VerdictDao
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
    fun provideDatabase(@ApplicationContext context: Context): FalcoDatabase {
        return Room.databaseBuilder(
            context,
            FalcoDatabase::class.java,
            FalcoDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideClaimDao(db: FalcoDatabase): ClaimDao = db.claimDao()

    @Provides
    fun provideVerdictDao(db: FalcoDatabase): VerdictDao = db.verdictDao()

    @Provides
    fun providePaperStanceDao(db: FalcoDatabase): PaperStanceDao = db.paperStanceDao()

    @Provides
    fun provideQuotaDao(db: FalcoDatabase): QuotaDao = db.quotaDao()
}
