package com.ahmedyejam.mks.data.model

data class FlashcardGenerationConfig(
    // Front options
    val includeStemInFront: Boolean = true,
    val includeOptionsInFront: Boolean = false,
    val includeImageInFront: Boolean = false,
    
    // Back options
    val includeAnswerInBack: Boolean = true,
    val includeExplanationInBack: Boolean = true,
    val includeHintInBack: Boolean = false,
    val includeReferenceInBack: Boolean = false,
    val includeAdditionalInfoInBack: Boolean = false,
    val includeImageInBack: Boolean = false
) {
    companion object {
        val DEFAULT = FlashcardGenerationConfig()
    }
}
