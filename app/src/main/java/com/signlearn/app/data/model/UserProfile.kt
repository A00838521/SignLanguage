package com.signlearn.app.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val totalPoints: Int = 0,
    val streak: Int = 0,
    val dailyGoal: Int = 50,
    val lastActive: Long = 0L,
    val xpHistory: List<Int> = emptyList()
)
