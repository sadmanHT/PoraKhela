package com.porakhela.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for app initialization and global state
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    // Add repositories here when implemented
) : ViewModel() {
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        initializeApp()
    }
    
    private fun initializeApp() {
        viewModelScope.launch {
            // Simulate app initialization
            delay(1000) // Show splash screen for 1 second
            
            // Check if user is already logged in
            checkLoginStatus()
            
            _isReady.value = true
        }
    }
    
    private suspend fun checkLoginStatus() {
        // TODO: Check shared preferences or database for login status
        // For now, assume user is not logged in
        _isLoggedIn.value = false
    }
    
    fun updateLoginState(isLoggedIn: Boolean) {
        _isLoggedIn.value = isLoggedIn
    }
}