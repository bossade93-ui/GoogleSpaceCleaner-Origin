package com.googlespacecleaner.core.data.local.selection.di

import com.googlespacecleaner.core.data.local.selection.InMemorySelectionRepository
import com.googlespacecleaner.core.domain.repository.SelectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SelectionModule {

    @Binds
    abstract fun bindSelectionRepository(impl: InMemorySelectionRepository): SelectionRepository
}
