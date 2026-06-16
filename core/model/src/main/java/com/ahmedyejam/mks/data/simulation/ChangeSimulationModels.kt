package com.ahmedyejam.mks.data.simulation

data class SimulatedItem(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String? = null,
    val reason: String? = null
)

data class ChangeSimulationResult(
    val title: String,
    val summary: String,
    val warnings: List<String> = emptyList(),
    val affectedBooks: Int = 0,
    val affectedQuizzes: Int = 0,
    val affectedQuestions: Int = 0,
    val createdItems: List<SimulatedItem> = emptyList(),
    val updatedItems: List<SimulatedItem> = emptyList(),
    val deletedItems: List<SimulatedItem> = emptyList(),
    val skippedItems: List<SimulatedItem> = emptyList(),
    val blockedItems: List<SimulatedItem> = emptyList()
) {
    val hasBlockingItems: Boolean get() = blockedItems.isNotEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    val hasChanges: Boolean
        get() = createdItems.isNotEmpty() || updatedItems.isNotEmpty() || deletedItems.isNotEmpty()
}
