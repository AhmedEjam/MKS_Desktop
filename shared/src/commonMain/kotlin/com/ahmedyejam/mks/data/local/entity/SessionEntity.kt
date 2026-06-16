package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class SessionEntity(
    val id: Long = 0,
    val quizId: Long,
    val label: String = "",
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val incorrectCount: Int = 0,
    val answers: Map<Long, List<Int>> = emptyMap(),
    val answersByIndex: Map<Int, List<Int>> = emptyMap(),
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastModifiedAt: Long = 0L,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0L,
    val questionIds: List<Long> = emptyList(),
    val originalQuestionCount: Int = 0,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val rapidMode: Boolean = false,
    val repeatWrong: Boolean = true,
    val quizTimerSeconds: Int = 0,
    val questionTimerSeconds: Int = 0,
    val rangeFrom: Int = 0,
    val rangeTo: Int = -1,
    val includeFilters: List<String> = emptyList(),
    val droppedOptions: Map<Long, List<Int>> = emptyMap(),
    val droppedOptionsByIndex: Map<Int, List<Int>> = emptyMap(),
    val visibleOptionsCount: Map<Long, Int> = emptyMap(),
    val visibleOptionsCountByIndex: Map<Int, Int> = emptyMap(),
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val resultTaxonomy: Map<Int, String> = emptyMap(),
    val deletedAt: Long? = null
)
