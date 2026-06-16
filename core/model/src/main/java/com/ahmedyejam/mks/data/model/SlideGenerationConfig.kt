package com.ahmedyejam.mks.data.model

data class SlideGenerationConfig(
    // Title mapping
    val includeStemInTitle: Boolean = true,
    
    // Body mapping
    val includeOptionsInBody: Boolean = false,
    val includeAnswerInBody: Boolean = true,
    val includeExplanationInBody: Boolean = true,
    
    // Speaker notes mapping
    val includeHintInSpeakerNotes: Boolean = true,
    val includeReferenceInSpeakerNotes: Boolean = false,
    
    // Media
    val includeImage: Boolean = false
) {
    companion object {
        val DEFAULT = SlideGenerationConfig()
    }
}
