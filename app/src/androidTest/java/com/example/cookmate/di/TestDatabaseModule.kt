package com.example.cookmate

import android.content.Context
import androidx.room.Room
import com.example.cookmate.data.db.CookMateDatabase
import com.example.cookmate.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {
    
    @Singleton
    @Provides
    fun provideInMemoryDatabase(context: Context): CookMateDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            CookMateDatabase::class.java
        ).allowMainThreadQueries().build()
    }
}
