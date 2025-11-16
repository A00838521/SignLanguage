package com.signlearn.app.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val totalPoints: Int = 0
)
