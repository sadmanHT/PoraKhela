package com.porakhela

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Porakhela Application Class
 * Entry point for Hilt dependency injection
 */
@HiltAndroidApp
class PorakhelaApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any required libraries here
        initializeApp()
    }
    
    private fun initializeApp() {
        // Initialize crash reporting, analytics, etc.
        // For now, this is a placeholder for future integrations
    }
}