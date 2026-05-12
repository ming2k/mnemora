package com.hihusky.mnemora.ui.screens.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.ui.components.MnemoraCard
import com.hihusky.mnemora.ui.components.MnemoraEmptyState
import com.hihusky.mnemora.ui.components.MnemoraMetricCard
import com.hihusky.mnemora.ui.components.MnemoraProgressLine
import com.hihusky.mnemora.ui.components.MnemoraStatusBadge
import com.hihusky.mnemora.ui.components.topappbar.MnemoraCollapsibleTopAppBar
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.statusContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onResumeSession: (bookId: Int, mode: String, sessionId: Long?) -> Unit,
    viewModel: RecordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scrollFraction by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            when {
                firstIndex > 0 -> 1f
                else -> (firstOffset / 120f).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        topBar = {
            MnemoraCollapsibleTopAppBar(title = "Records", scrollFraction = scrollFraction)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.sessions.isEmpty() -> {
                    EmptyRecordsState()
                }

                else -> {
                    val grouped = groupSessionsByDate(uiState.sessions)
                    val total = uiState.sessions.size
                    val completed = uiState.sessions.count { it.isCompleted }
                    val inProgress = uiState.sessions.count { it.isActive }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Stats header
                        item {
                            StatsRow(
                                total = total,
                                completed = completed,
                                inProgress = inProgress
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        grouped.forEach { (label, sessions) ->
                            item {
                                SectionHeader(label)
                            }
                            items(sessions, key = { it.id }) { session ->
                                SessionCard(
                                    session = session,
                                    bookName = uiState.bookMap[session.bookId]?.displayName ?: "Unknown",
                                    onClick = {
                                        onResumeSession(
                                            session.bookId,
                                            session.mode,
                                            if (session.isActive || session.isCompleted) session.id else null
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(total: Int, completed: Int, inProgress: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MnemoraMetricCard(
            value = total.toString(),
            label = "Sessions",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        MnemoraMetricCard(
            value = completed.toString(),
            label = "Completed",
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        MnemoraMetricCard(
            value = inProgress.toString(),
            label = "In Progress",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SessionCard(
    session: StudySessionEntity,
    bookName: String,
    onClick: () -> Unit
) {
    val dateText = formatRelativeTime(session.startTime)
    val isCompleted = session.isCompleted
    val isActive = session.isActive

    val statusColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val statusIcon = when {
        isActive -> Icons.Default.PlayArrow
        isCompleted -> Icons.Default.CheckCircle
        else -> Icons.Default.Schedule
    }
    val statusLabel = when {
        isActive -> "In progress"
        isCompleted -> "Completed"
        else -> "Abandoned"
    }

    val progressPercent = if (session.totalQuestions > 0) {
        session.currentIndex * 100 / session.totalQuestions
    } else 0

    MnemoraCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(MnemoraSize.AvatarSmall)
                        .clip(CircleShape)
                        .background(statusColor.statusContainer()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(MnemoraSpacing.Medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "$dateText · ${session.mode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MnemoraStatusBadge(text = statusLabel, color = statusColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            MnemoraProgressLine(progress = progressPercent / 100f, color = statusColor)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${session.currentIndex} / ${session.totalQuestions} questions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

@Composable
private fun EmptyRecordsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MnemoraEmptyState(
            icon = Icons.Outlined.History,
            title = "No records yet",
            message = "Start studying to see your history here"
        )
    }
}

private fun groupSessionsByDate(
    sessions: List<StudySessionEntity>
): List<Pair<String, List<StudySessionEntity>>> {
    val now = Calendar.getInstance()
    val today = now.toDateKey()
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now.toDateKey()
    now.add(Calendar.DAY_OF_YEAR, -6)
    val thisWeekStart = now.toDateKey()

    val groups = mutableMapOf<String, MutableList<StudySessionEntity>>()

    sessions.forEach { session ->
        val sessionDate = Calendar.getInstance().apply {
            timeInMillis = session.startTime
        }.toDateKey()

        val label = when {
            sessionDate == today -> "Today"
            sessionDate == yesterday -> "Yesterday"
            sessionDate >= thisWeekStart -> "This week"
            else -> {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(session.startTime))
            }
        }
        groups.getOrPut(label) { mutableListOf() }.add(session)
    }

    val order = listOf("Today", "Yesterday", "This week")
    val sorted = mutableListOf<Pair<String, List<StudySessionEntity>>>()
    order.forEach { key ->
        groups[key]?.let { sorted.add(key to it) }
    }
    groups.keys.filter { it !in order }.sortedDescending().forEach { key ->
        sorted.add(key to groups[key]!!)
    }
    return sorted
}

private fun Calendar.toDateKey(): Int {
    return get(Calendar.YEAR) * 10000 + get(Calendar.MONTH) * 100 + get(Calendar.DAY_OF_MONTH)
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
