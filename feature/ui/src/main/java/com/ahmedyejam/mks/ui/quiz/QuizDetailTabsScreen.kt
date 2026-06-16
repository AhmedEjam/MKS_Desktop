package com.ahmedyejam.mks.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.ui.session.SessionManagementScreen
import com.ahmedyejam.mks.ui.session.SessionViewModel
import kotlinx.coroutines.launch

enum class QuizTab(val title: String, val icon: ImageVector) {
    SESSIONS("Sessions", Icons.Rounded.History),
    BROWSE("Browse", Icons.AutoMirrored.Rounded.List)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailTabsScreen(
    quizId: Long,
    quizTitle: String?,
    sessionViewModel: SessionViewModel,
    questionsViewModel: QuizQuestionsViewModel,
    dataStoreManager: DataStoreManager,
    onSessionSelected: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val tabs = remember { QuizTab.entries.toTypedArray() }
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = { Text(quizTitle ?: "Quiz Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, null, Modifier.size(20.dp)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1
        ) { page ->
            when (tabs[page]) {
                QuizTab.SESSIONS -> {
                    SessionManagementScreen(
                        quizId = quizId,
                        viewModel = sessionViewModel,
                        dataStoreManager = dataStoreManager,
                        isEmbedded = true,
                        onSessionSelected = onSessionSelected,
                        onNavigateBack = onBack
                    )
                }
                QuizTab.BROWSE -> {
                    QuizQuestionsScreen(
                        viewModel = questionsViewModel,
                        isEmbedded = true,
                        onBack = onBack
                    )
                }
            }
        }
    }
}
