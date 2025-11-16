package com.signlearn.app.ui.auth

import com.google.firebase.auth.FirebaseUser

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: FirebaseUser) : AuthState
    data class Error(val message: String) : AuthState
}
