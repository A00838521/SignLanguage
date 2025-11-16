package com.signlearn.app.data.model

data class UserProgress(
    val lessonId: String = "",
    val score: Int = 0,
    val completedAt: Long = 0L
)
