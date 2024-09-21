package com.gun.personalcolor.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun providesFirebaseAnalyticsInstance(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context).apply {
            setAnalyticsCollectionEnabled(true)
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseCrashlyticsInstance(): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
        }
    }
}