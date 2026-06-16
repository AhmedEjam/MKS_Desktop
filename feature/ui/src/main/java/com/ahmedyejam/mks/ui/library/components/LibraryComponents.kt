package com.ahmedyejam.mks.ui.library.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.ui.tooling.preview.Preview
import com.ahmedyejam.mks.ui.theme.MKSTheme
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.ui.theme.normalizeMksThemeMode
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import java.text.SimpleDateFormat
import java.util.*

private enum class BannerAction {
    Brand,
    Conversion,
    Feedback,
    Promotion
}

private data class LocalBannerSlide(
    val action: BannerAction,
    val title: String,
    val subtitle: String,
    val cta: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryBanner(
    resumeQuiz: QuizEntity?,
    recentQuizzes: List<QuizEntity>,
    currentThemeMode: String,
    onQuizClick: (QuizEntity) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetQuiz = resumeQuiz ?: recentQuizzes.firstOrNull()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val normalizedTheme = normalizeMksThemeMode(currentThemeMode)
    val isSystemDark = isSystemInDarkTheme()
    val isActuallyDark = when (normalizedTheme) {
        "MIDNIGHT", "PLAIN_DARK" -> true
        "SYSTEM" -> isSystemDark
        else -> false
    }
    val tokens = LocalMksDesignTokens.current
    val bannerShape = RoundedCornerShape(if (tokens.isPlain) tokens.cardRadius else 30.dp)
    
    val pagerState = rememberPagerState(initialPage = 501, pageCount = { 1000 })
    val currentRealPage = remember { derivedStateOf { pagerState.currentPage % 2 } }

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged) {
        if (!isDragged) {
            while (true) {
                kotlinx.coroutines.delay(4000)
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f), // 2:1 aspect ratio based on contact banner (1774x887)
            shape = bannerShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page % 2) {
                    0 -> {
                        val resumeImageRes = remember(isRtl, normalizedTheme, isActuallyDark) {
                            when {
                                normalizedTheme == "FOREST" -> if (isRtl) R.drawable.resume_banner_ar_light_forest else R.drawable.resume_banner_en_light_forest
                                normalizedTheme == "LAVENDER" || normalizedTheme == "PLAIN_LIGHT" -> if (isRtl) R.drawable.resume_banner_ar_light_lavender else R.drawable.resume_banner_en_light_lavender
                                isActuallyDark -> if (isRtl) R.drawable.resume_banner_ar_dark_midnight else R.drawable.resume_banner_en_dark_midnight
                                else -> if (isRtl) R.drawable.resume_banner_ar_light_dawn else R.drawable.resume_banner_en_light_dawn
                            }
                        }
                        ResumeBannerImageSlide(
                            imageRes = resumeImageRes,
                            shape = bannerShape,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = targetQuiz != null) {
                                    targetQuiz?.let(onQuizClick)
                                }
                        )
                    }
                    1 -> {
                        val contactImageRes = remember(isRtl, normalizedTheme, isActuallyDark) {
                            when {
                                normalizedTheme == "FOREST" -> if (isRtl) R.drawable.contact_banner_ar_light_forest_path else R.drawable.contact_banner_en_light_forest_path
                                normalizedTheme == "LAVENDER" || normalizedTheme == "PLAIN_LIGHT" -> if (isRtl) R.drawable.contact_banner_ar_light_lavender_valley else R.drawable.contact_banner_en_light_lavender_valley
                                isActuallyDark -> if (isRtl) R.drawable.contact_banner_ar_dark_moonlit_valley else R.drawable.contact_banner_en_dark_moonlit_valley
                                else -> if (isRtl) R.drawable.contact_banner_ar_light_sunrise_valley else R.drawable.contact_banner_en_light_sunrise_valley
                            }
                        }
                        ContactBannerImageSlide(
                            imageRes = contactImageRes,
                            shape = bannerShape,
                            onContactClick = { onLinkClick("https://linktr.ee/MKSpace") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Pager Indicators
        Row(
            modifier = Modifier.height(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { iteration ->
                val color = if (currentRealPage.value == iteration) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun ResumeBannerImageSlide(
    imageRes: Int,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clip(shape)) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop, // Crop to fit 2:1 aspect ratio
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ContactBannerImageSlide(
    imageRes: Int,
    shape: Shape,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier
        .clip(shape)
        .clickable(onClick = onContactClick)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = "Contact Us",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BannerMiniArtwork(
    action: BannerAction,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    colors.primaryContainer.copy(alpha = 0.46f),
                    colors.surface.copy(alpha = 0.70f),
                    colors.secondary.copy(alpha = 0.12f)
                )
            )
        )

        // Soft arch frame.
        val archLeft = w * 0.12f
        val archTop = h * 0.08f
        val archWidth = w * 0.76f
        val archHeight = h * 0.84f
        drawRoundRect(
            color = colors.primary.copy(alpha = 0.16f),
            topLeft = Offset(archLeft, archTop),
            size = Size(archWidth, archHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(archWidth * 0.48f, archWidth * 0.48f)
        )
        drawRoundRect(
            color = colors.surface.copy(alpha = 0.70f),
            topLeft = Offset(archLeft + w * 0.045f, archTop + h * 0.045f),
            size = Size(archWidth - w * 0.09f, archHeight - h * 0.09f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(archWidth * 0.40f, archWidth * 0.40f)
        )

        val sunColor = when (action) {
            BannerAction.Promotion -> colors.tertiary
            BannerAction.Feedback -> colors.secondary
            else -> colors.primary
        }
        drawCircle(
            color = sunColor.copy(alpha = 0.52f),
            radius = w * 0.13f,
            center = Offset(w * 0.66f, h * 0.27f)
        )

        val far = Path().apply {
            moveTo(w * 0.10f, h * 0.58f)
            cubicTo(w * 0.26f, h * 0.40f, w * 0.42f, h * 0.59f, w * 0.56f, h * 0.45f)
            cubicTo(w * 0.73f, h * 0.30f, w * 0.84f, h * 0.58f, w * 0.96f, h * 0.42f)
            lineTo(w * 0.96f, h)
            lineTo(w * 0.10f, h)
            close()
        }
        drawPath(far, colors.secondary.copy(alpha = 0.20f))

        val near = Path().apply {
            moveTo(w * 0.08f, h * 0.76f)
            cubicTo(w * 0.24f, h * 0.60f, w * 0.39f, h * 0.78f, w * 0.58f, h * 0.58f)
            cubicTo(w * 0.73f, h * 0.42f, w * 0.86f, h * 0.79f, w, h * 0.60f)
            lineTo(w, h)
            lineTo(w * 0.08f, h)
            close()
        }
        drawPath(near, colors.onSurface.copy(alpha = 0.10f))

        when (action) {
            BannerAction.Brand -> {
                drawLine(colors.primary.copy(alpha = 0.48f), Offset(w * 0.18f, h * 0.88f), Offset(w * 0.18f, h * 0.63f), 3.dp.toPx())
                repeat(5) { i ->
                    val y = h * (0.83f - i * 0.04f)
                    drawLine(colors.primary.copy(alpha = 0.44f), Offset(w * 0.18f, y), Offset(w * (0.10f + i * 0.01f), y - h * 0.035f), 2.dp.toPx())
                    drawLine(colors.primary.copy(alpha = 0.36f), Offset(w * 0.18f, y), Offset(w * (0.27f - i * 0.01f), y - h * 0.035f), 2.dp.toPx())
                }
            }
            BannerAction.Conversion -> {
                repeat(3) { i ->
                    drawRoundRect(
                        color = colors.primary.copy(alpha = 0.48f - i * 0.08f),
                        topLeft = Offset(w * (0.56f + i * 0.035f), h * (0.73f - i * 0.05f)),
                        size = Size(w * 0.30f, h * 0.075f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx(), 9.dp.toPx())
                    )
                }
            }
            BannerAction.Feedback -> {
                drawCircle(colors.primary.copy(alpha = 0.75f), radius = 5.dp.toPx(), center = Offset(w * 0.22f, h * 0.24f))
                drawCircle(colors.secondary.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = Offset(w * 0.34f, h * 0.30f))
                drawCircle(colors.tertiary.copy(alpha = 0.45f), radius = 6.dp.toPx(), center = Offset(w * 0.49f, h * 0.24f))
            }
            BannerAction.Promotion -> {
                repeat(4) { i ->
                    val x = w * (0.18f + i * 0.10f)
                    drawRoundRect(
                        color = colors.primary.copy(alpha = 0.34f + i * 0.11f),
                        topLeft = Offset(x, h * (0.58f - i * 0.07f)),
                        size = Size(w * 0.06f, h * (0.20f + i * 0.07f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryPreviewCard(
    category: CategoryWithMetadata,
    onCategorySelected: (CategoryWithMetadata) -> Unit,
    onCategoryLongClick: (CategoryWithMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalMksDesignTokens.current
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val tint = category.color?.let { Color(it) } ?: colors.primary
    val displayIcon = category.emoji

    Surface(
        modifier = modifier
            .widthIn(min = 132.dp)
            .combinedClickable(
                onClick = { onCategorySelected(category) },
                onLongClick = {
                    if (!tokens.isPlain) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCategoryLongClick(category)
                }
            ),
        shape = RoundedCornerShape(tokens.cardRadius),
        color = colors.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.10f)),
        shadowElevation = if (category.isPinned) tokens.cardElevation else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(tokens.chipRadius),
                color = tint.copy(alpha = 0.13f),
                contentColor = tint
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!displayIcon.isNullOrBlank()) {
                        Text(displayIcon, style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = category.questionCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
            }
            if (category.isPinned) {
                Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(15.dp), tint = tint)
            }
        }
    }
}

@Composable
private fun ColorScheme.onContainer(containerColor: Color): Color {
    return when (containerColor) {
        primaryContainer -> onPrimaryContainer
        secondaryContainer -> onSecondaryContainer
        tertiaryContainer -> onTertiaryContainer
        else -> onSurface
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItem(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewMode: String = "LIST",
    showCover: Boolean = true
) {
    val tokens = LocalMksDesignTokens.current
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(tokens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.11f)),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        if (viewMode == "GRID") {
            Column(modifier = Modifier.padding(13.dp)) {
                BookVisual(
                    title = book.title,
                    coverImage = book.coverImage,
                    isPinned = book.isPinned,
                    showCover = showCover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.05f)
                )
                Spacer(Modifier.height(12.dp))
                BookTextBlock(book = book, modifier = Modifier.fillMaxWidth(), isGrid = true)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    QuestionCountPill(count = book.questionCount)
                    IconButton(onClick = onLongClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = colors.onSurfaceVariant)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BookVisual(
                    title = book.title,
                    coverImage = book.coverImage,
                    isPinned = book.isPinned,
                    showCover = showCover,
                    modifier = Modifier.size(88.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    BookTextBlock(book = book, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    QuestionCountPill(count = book.questionCount)
                }
                IconButton(onClick = onLongClick, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BookTextBlock(book: BookEntity, modifier: Modifier = Modifier, isGrid: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colors.onSurface
        )
        if (isGrid || book.description.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = book.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                minLines = if (isGrid) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun QuestionCountPill(count: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = colors.primary.copy(alpha = 0.10f),
        contentColor = colors.primary
    ) {
        Text(
            text = stringResource(R.string.questions_count, count),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun BookVisual(
    title: String,
    coverImage: String?,
    isPinned: Boolean,
    showCover: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalMksDesignTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (tokens.useGradients) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                colors.primaryContainer.copy(alpha = 0.50f),
                                colors.surface.copy(alpha = 0.88f),
                                colors.secondaryContainer.copy(alpha = 0.26f)
                            )
                        )
                    )
                } else {
                    Modifier.background(colors.primaryContainer.copy(alpha = 0.2f))
                }
            )
            .border(1.dp, colors.outline.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (showCover && !coverImage.isNullOrBlank()) {
            AsyncImage(
                model = coverImage,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        } else {
            val brush = if (tokens.useGradients) {
                Brush.linearGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = 0.40f),
                        colors.secondaryContainer.copy(alpha = 0.30f),
                        colors.tertiaryContainer.copy(alpha = 0.20f)
                    )
                )
            } else {
                Brush.linearGradient(listOf(colors.primaryContainer.copy(alpha = 0.15f), colors.primaryContainer.copy(alpha = 0.15f)))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        if (isPinned) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
                shape = RoundedCornerShape(9.dp),
                color = colors.surface.copy(alpha = 0.88f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = "Pinned",
                    modifier = Modifier
                        .size(19.dp)
                        .padding(3.dp),
                    tint = colors.primary
                )
            }
        }
    }
}

@Composable
fun FieldChip(
    field: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Text(
            text = field,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuizItem(
    quiz: QuizEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewMode: String = "LIST",
    showCover: Boolean = true
) {
    val tokens = LocalMksDesignTokens.current
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(tokens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        if (viewMode == "GRID") {
            Column(modifier = Modifier.padding(12.dp)) {
                QuizVisual(
                    quiz = quiz,
                    showCover = showCover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
                )
                Spacer(Modifier.height(12.dp))
                QuizTextBlock(quiz = quiz, modifier = Modifier.fillMaxWidth(), isGrid = true)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onLongClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuizVisual(
                    quiz = quiz,
                    showCover = showCover,
                    modifier = Modifier.size(68.dp)
                )
                QuizTextBlock(quiz = quiz, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.70f))
                IconButton(onClick = onLongClick, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QuizVisual(
    quiz: QuizEntity,
    showCover: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.secondaryContainer.copy(alpha = 0.44f)),
        contentAlignment = Alignment.Center
    ) {
        if (showCover && !quiz.coverImage.isNullOrBlank()) {
            AsyncImage(
                model = quiz.coverImage,
                contentDescription = quiz.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                clipToBounds = true
            )
        } else {
            val defaultIcon = when(quiz.iconName) {
                "flashcard" -> Icons.Rounded.Style
                "slideshow" -> Icons.Rounded.Slideshow
                "note" -> Icons.AutoMirrored.Rounded.Article
                "prompt" -> Icons.Rounded.AutoAwesome
                else -> Icons.AutoMirrored.Rounded.FactCheck
            }
            Icon(
                imageVector = defaultIcon,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(34.dp)
            )
        }
        if (quiz.isPinned) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape = CircleShape,
                color = colors.surface.copy(alpha = 0.86f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier
                        .size(17.dp)
                        .padding(3.dp),
                    tint = colors.secondary
                )
            }
        }
    }
}

@Composable
private fun QuizTextBlock(quiz: QuizEntity, modifier: Modifier = Modifier, isGrid: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = quiz.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isGrid || quiz.description.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = quiz.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                minLines = if (isGrid) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = colors.secondary.copy(alpha = 0.09f),
                shape = RoundedCornerShape(12.dp),
                contentColor = colors.secondary
            ) {
                Text(
                    text = stringResource(R.string.qs_count, quiz.questionCount),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
            quiz.category?.takeIf { it.isNotBlank() }?.let { category ->
                Surface(
                    color = colors.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    contentColor = colors.onSurfaceVariant
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EntityInfoSection(
    title: String,
    info: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        info.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookOptionsSheet(
    book: BookEntity,
    onDismiss: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDashboardClick: () -> Unit
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!book.coverImage.isNullOrBlank()) {
                        AsyncImage(
                            model = book.coverImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = book.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.questions_count, book.questionCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onPinClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (book.isPinned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (book.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.pin_label))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Knowledge Dashboard") },
                supportingContent = { Text("View all study assets and progress") },
                leadingContent = { Icon(Icons.Rounded.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onDashboardClick(); onDismiss() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.show_info_stats)) },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                trailingContent = {
                    Switch(checked = showInfo, onCheckedChange = { showInfo = it })
                },
                modifier = Modifier.clickable { showInfo = !showInfo }
            )

            AnimatedVisibility(visible = showInfo) {
                EntityInfoSection(
                    title = stringResource(R.string.metadata_stats_title),
                    info = listOf(
                        stringResource(R.string.internal_id_label) to book.id.toString(),
                        stringResource(R.string.external_id_label) to book.externalId.take(8) + "...",
                        stringResource(R.string.created_label) to dateFormat.format(Date(book.createdAt)),
                        stringResource(R.string.updated_label) to dateFormat.format(Date(book.updatedAt)),
                        stringResource(R.string.last_studied_label) to if (book.lastStudiedAt > 0) dateFormat.format(Date(book.lastStudiedAt)) else stringResource(R.string.never_label),
                        stringResource(R.string.total_questions_label) to book.questionCount.toString(),
                        stringResource(R.string.answered_label) to "${book.answeredCount} (${(book.completionPercentage * 100).toInt()}%)",
                        stringResource(R.string.accuracy_label) to "${(book.accuracyPercentage * 100).toInt()}%",
                        stringResource(R.string.avg_attempts_label) to "%.1f".format(if (book.questionCount > 0) book.totalAttempts.toFloat() / book.questionCount else 0f)
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(if (book.isPinned) R.string.unpin_from_top else R.string.pin_to_top)) },
                leadingContent = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                modifier = Modifier.clickable { onPinClick(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit)) },
                leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                modifier = Modifier.clickable { onEditClick(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_label)) },
                leadingContent = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                modifier = Modifier.clickable { onExportClick(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_label)) },
                leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable { onImportClick(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete)) },
                leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDeleteClick(); onDismiss() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizOptionsSheet(
    quiz: QuizEntity,
    onDismiss: () -> Unit,
    onBrowseQuestionsClick: (() -> Unit)? = null,
    onScanClick: (() -> Unit)? = null,
    onPinClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null,
    onImportClick: (() -> Unit)? = null,
    onDeleteClick: () -> Unit
) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!quiz.coverImage.isNullOrBlank()) {
                        AsyncImage(
                            model = quiz.coverImage,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = quiz.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.questions_count, quiz.questionCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onPinClick != null) {
                    IconButton(
                        onClick = onPinClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (quiz.isPinned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (quiz.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.pin_label))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.show_info_stats)) },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                trailingContent = {
                    Switch(checked = showInfo, onCheckedChange = { showInfo = it })
                },
                modifier = Modifier.clickable { showInfo = !showInfo }
            )

            AnimatedVisibility(visible = showInfo) {
                EntityInfoSection(
                    title = stringResource(R.string.metadata_stats_title),
                    info = listOf(
                        stringResource(R.string.internal_id_label) to quiz.id.toString(),
                        stringResource(R.string.external_id_label) to quiz.externalId.take(8) + "...",
                        stringResource(R.string.book_id_label) to quiz.bookId.toString(),
                        stringResource(R.string.category_label) to (quiz.category ?: stringResource(R.string.none_label)),
                        stringResource(R.string.created_label) to dateFormat.format(Date(quiz.createdAt)),
                        stringResource(R.string.updated_label) to dateFormat.format(Date(quiz.updatedAt)),
                        stringResource(R.string.last_studied_label) to if (quiz.lastStudiedAt > 0) dateFormat.format(Date(quiz.lastStudiedAt)) else stringResource(R.string.never_label),
                        stringResource(R.string.total_questions_label) to quiz.questionCount.toString(),
                        stringResource(R.string.answered_label) to "${quiz.answeredCount} (${(quiz.completionPercentage * 100).toInt()}%)",
                        stringResource(R.string.accuracy_label) to "${(quiz.accuracyPercentage * 100).toInt()}%",
                        stringResource(R.string.avg_attempts_label) to "%.1f".format(if (quiz.questionCount > 0) quiz.totalAttempts.toFloat() / quiz.questionCount else 0f)
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            onBrowseQuestionsClick?.let { onClick ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.browse_questions)) },
                    leadingContent = { Icon(Icons.Rounded.Search, null) },
                    modifier = Modifier.clickable { onClick(); onDismiss() }
                )
            }
            onScanClick?.let { onClick ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.scanner)) },
                    leadingContent = { Icon(Icons.Rounded.QrCodeScanner, null) },
                    modifier = Modifier.clickable { onClick(); onDismiss() }
                )
            }
            if (onBrowseQuestionsClick != null || onScanClick != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            onPinClick?.let { onClick ->
                ListItem(
                    headlineContent = { Text(stringResource(if (quiz.isPinned) R.string.unpin_from_top else R.string.pin_to_top)) },
                    leadingContent = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                    modifier = Modifier.clickable { onClick(); onDismiss() }
                )
            }
            onEditClick?.let { onClick ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.edit)) },
                    leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    modifier = Modifier.clickable { onClick(); onDismiss() }
                )
            }
            onExportClick?.let { onClick ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.export_label)) },
                    leadingContent = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                    modifier = Modifier.clickable { onClick(); onDismiss() }
                )
            }
            onImportClick?.let { onClick ->
                ListItem(
                    headlineContent = { Text(stringResource(R.string.import_label)) },
                    leadingContent = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                    modifier = Modifier.clickable { onClick(); onDismiss() }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete)) },
                leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDeleteClick(); onDismiss() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookItemPreview() {
    MKSTheme {
        BookItem(
            book = BookEntity(
                id = 1,
                externalId = "book_1",
                title = "Sample Book",
                description = "This is a sample book description."
            ),
            onClick = {},
            onLongClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuizItemPreview() {
    MKSTheme {
        QuizItem(
            quiz = QuizEntity(
                id = 1,
                externalId = "quiz_1",
                bookId = 1,
                title = "Sample Quiz",
                description = "This is a sample quiz description."
            ),
            onClick = {},
            onLongClick = {}
        )
    }
}
