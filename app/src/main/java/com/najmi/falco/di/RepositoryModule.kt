package com.najmi.falco.di

import com.najmi.falco.data.remote.semanticscholar.SemanticScholarClient
import com.najmi.falco.data.repository.PaperRepositoryImpl
import com.najmi.falco.data.repository.QuotaRepositoryImpl
import com.najmi.falco.data.repository.VerdictRepositoryImpl
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.domain.repository.IQuotaRepository
import com.najmi.falco.domain.repository.IVerdictRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPaperRepository(impl: PaperRepositoryImpl): IPaperRepository

    @Binds
    @Singleton
    abstract fun bindVerdictRepository(impl: VerdictRepositoryImpl): IVerdictRepository

    @Binds
    @Singleton
    abstract fun bindQuotaRepository(impl: QuotaRepositoryImpl): IQuotaRepository
}
