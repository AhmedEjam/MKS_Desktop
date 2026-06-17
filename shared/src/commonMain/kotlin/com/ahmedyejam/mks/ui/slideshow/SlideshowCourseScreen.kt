package com.ahmedyejam.mks.ui.slideshow

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.ui.components.LoadingErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSlideshowCourseScreen(
    viewModel: SlideshowCourseViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.course?.title ?: "Slideshow") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCourse(state.course?.id ?: 0L) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            
            LoadingErrorState(state.isLoading, state.error, { state.course?.id?.let { viewModel.loadCourse(it) } })

            if (state.course != null) {
                SlidePresentationContent(state, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidePresentationContent(
    state: SlideshowCourseUiState,
    viewModel: SlideshowCourseViewModel
) {
    if (state.slides.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No slides in this course.")
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { state.slides.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.loadCourse(state.course?.id ?: 0L) // Simplified sync
    }

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            pageSpacing = 16.dp
        ) { page ->
            SlidePageContent(state.slides[page])
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.previousSlide() },
                enabled = state.currentIndex > 0
            ) {
                Icon(Icons.Default.ChevronLeft, null)
                Text("Previous")
            }
            
            Text("Slide ${state.currentIndex + 1} of ${state.slides.size}", style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = { viewModel.nextSlide() },
                enabled = state.currentIndex < state.slides.size - 1
            ) {
                Text("Next")
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
}

@Composable
private fun SlidePageContent(slide: CourseSlideEntity) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(slide.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        if (!slide.imagePath.isNullOrBlank()) {
            AsyncImage(
                model = slide.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Text(slide.body, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp)
        
        if (!slide.speakerNotes.isNullOrBlank()) {
            HorizontalDivider()
            Text("Notes:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(slide.speakerNotes!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
