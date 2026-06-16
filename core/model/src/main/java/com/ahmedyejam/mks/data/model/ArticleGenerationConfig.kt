package com.ahmedyejam.mks.data.model

data class ArticleGenerationConfig(
    val includeStemAsTitle: Boolean = true,
    val includeExplanationInBody: Boolean = true,
    val includeOptionsAsBulletPoints: Boolean = true,
    val includeHintInBody: Boolean = false,
    val includeReferenceInBody: Boolean = false,
    val includeTags: Boolean = true,
    val articleMode: String = "SIMPLE_NOTE"
) {
    companion object {
        val DEFAULT = ArticleGenerationConfig()
    }
}
