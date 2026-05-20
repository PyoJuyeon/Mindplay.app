package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Badge
import com.example.data.LeaderboardEntry
import com.example.data.MindfulLog
import com.example.data.StreakState
import com.example.ui.audio.AudioSynth
import com.example.ui.theme.*
import com.example.viewmodel.MindPlayViewModel
import com.example.viewmodel.StoryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class MindPlayTab(val title: String, val icon: @Composable () -> Unit) {
    Dashboard("Dashboard", { Icon(Icons.Default.Home, contentDescription = "Dashboard Tab") }),
    Entertainment("Play", { Icon(Icons.Default.PlayArrow, contentDescription = "Play Games Tab") }),
    Mindfulness("Mindful", { Icon(Icons.Default.Favorite, contentDescription = "Mindfulness Tab") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindPlayApp(viewModel: MindPlayViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(MindPlayTab.Dashboard) }

    // Collect DB states
    val streakState by viewModel.streakState.collectAsStateWithLifecycle()
    val mindfulLogs by viewModel.mindfulLogs.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    var activeGame by remember { mutableStateOf<String?>(null) } // "TRIVIA", "EMOJI", "PITCH_SYNTH", ...

    // Atmospheric dynamic background with glowing high-contrast radial matrices
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // Top-left soft Indigo glowing matrix
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(x = -size.width * 0.1f, y = -size.height * 0.1f),
                        radius = size.maxDimension * 0.55f
                    ),
                    center = Offset(x = -size.width * 0.1f, y = -size.height * 0.1f),
                    radius = size.maxDimension * 0.55f
                )
                // Bottom-right soft Emerald glowing matrix
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(x = size.width * 1.1f, y = size.height * 1.1f),
                        radius = size.maxDimension * 0.55f
                    ),
                    center = Offset(x = size.width * 1.1f, y = size.height * 1.1f),
                    radius = size.maxDimension * 0.55f
                )
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // Let the atmospheric depth show through
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0F1115).copy(alpha = 0.88f),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    MindPlayTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab; activeGame = null },
                            icon = tab.icon,
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.4.sp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else OnCosmosMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.White.copy(alpha = 0.05f),
                                unselectedTextColor = OnCosmosMuted,
                                unselectedIconColor = OnCosmosMuted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = if (activeGame != null) "GAME" else currentTab.name,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    }
                ) { state ->
                    when (state) {
                        "Dashboard" -> DashboardScreen(
                            streakState = streakState,
                            badges = badges,
                            leaderboard = leaderboard,
                            recentLogs = mindfulLogs,
                            onNavigateToPlay = { currentTab = MindPlayTab.Entertainment },
                            onNavigateToMind = { currentTab = MindPlayTab.Mindfulness }
                        )
                        "Entertainment" -> EntertainmentPanel(
                            onSelectGame = { activeGame = it }
                        )
                        "Mindfulness" -> MindfulnessPanel(
                            onSelectGame = { activeGame = it }
                        )
                        "GAME" -> {
                            GameContainer(
                                gameType = activeGame!!,
                                viewModel = viewModel,
                                onClose = { activeGame = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    streakState: StreakState?,
    badges: List<Badge>,
    leaderboard: List<LeaderboardEntry>,
    recentLogs: List<MindfulLog>,
    onNavigateToPlay: () -> Unit,
    onNavigateToMind: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App title header themed
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HELLO, TRAVELER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnCosmosMuted,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MindPlay",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
                
                // Glowing golden flame streak badge
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔥", fontSize = 16.sp)
                    Text(
                        text = "${streakState?.currentStreak ?: 0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Daily Balance card (The "Balance Gauge" element with the 2rem rounded corners & high contrast slider)
        item {
            val playPoints = streakState?.entertainmentPoints ?: 0
            val mindPoints = streakState?.mindfulnessPoints ?: 0
            val total = playPoints + mindPoints
            val ratio = if (total == 0) 0.5f else (mindPoints.toFloat() / total.toFloat())
            val playPercent = ((1f - ratio) * 100).toInt()
            val mindPercent = (ratio * 100).toInt()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.15f), // Indigo source overlay
                                Color(0xFF7C3AED).copy(alpha = 0.15f)  // Purple source overlay
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Balance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC) // Indigo light
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$playPercent% Play / $mindPercent% Zen",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE0E7FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Immersive dual-colored slider bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f - ratio)
                                    .background(Color(0xFF6366F1)) // Beautiful Indigo Accent
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(ratio)
                                    .background(Color(0xFF10B981)) // Beautiful Emerald Accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val description = when {
                        total == 0 -> "\"One quick trivia, then a breath. Let's start the balance!\""
                        ratio in 0.4f..0.6f -> "\"Perfection. Zen equilibrium is completely synchronized. Carry this harmony!\""
                        ratio > 0.6f -> "\"Highly mindful. Lean into play challenges to stimulate your dynamic focus.\""
                        else -> "\"Extremely active. Take a slow mindful stretch or breathing log to center yourself.\""
                    }
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = OnCosmosSecondary,
                        lineHeight = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // Streak Card & Total XP Card in glassmorphic style
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CONSISTENCY", fontSize = 10.sp, color = OnCosmosMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${streakState?.currentStreak ?: 0} Days",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B) // Amber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if ((streakState?.currentStreak ?: 0) > 0) "Streak Active! 🔥" else "Commit today! 🕊️",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL POWER", fontSize = 10.sp, color = OnCosmosMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${streakState?.totalPoints ?: 0} XP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6366F1) // Indigo
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Active Growth", fontSize = 11.sp, color = OnCosmosMuted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // For You Quick Actions section (Interactive grid matching the theme HTML exactly)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FOR YOU",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnCosmosMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trivia Burst
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.1f)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .clickable { onNavigateToPlay() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧩", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Trivia Burst",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "2 min challenge",
                                fontSize = 10.sp,
                                color = OnCosmosMuted
                            )
                        }
                    }

                    // Bubble Pop
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.1f)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .clickable { onNavigateToMind() }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🫧", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Bubble Pop",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Mindful breathing",
                                fontSize = 10.sp,
                                color = OnCosmosMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Story Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .clickable { onNavigateToPlay() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFF43F5E).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖", fontSize = 20.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Interactive: The Quiet Sea",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Quick creative story",
                                fontSize = 11.sp,
                                color = OnCosmosMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("→", color = OnCosmosMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Aura Achievements & Badges
        item {
            Column {
                Text(
                    text = "AURA ACHIEVEMENTS & BADGES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnCosmosMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val defaultBadges = listOf(
                    Badge("STREAK_3", "Streak Starter", "Maintain a 3-day consistency streak.", "Mindful"),
                    Badge("POINTS_100", "Point Champion", "Accumulate over 100 total points.", "Entertainment"),
                    Badge("ZEN_MASTER", "Zen Master", "Earn 50 mindfulness balance points.", "Mindful"),
                    Badge("CHALLENGE_PRO", "Mind Challenger", "Earn 50 dynamic challenge points.", "Entertainment")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    defaultBadges.forEach { b ->
                        val unlocked = badges.any { it.badgeId == b.badgeId }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (unlocked) Color(0xFF6366F1).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (unlocked) Color(0xFF6366F1).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (unlocked) Color(0xFF6366F1).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Default.Star else Icons.Default.Lock,
                                        contentDescription = "Badge Icon",
                                        tint = if (unlocked) Color(0xFFF59E0B) else OnCosmosMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = b.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    color = if (unlocked) Color.White else OnCosmosMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Friendly Leaderboard
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Friendly Leaderboard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Ranks",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    leaderboard.take(5).forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (entry.isUser) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.isUser) Color.White else OnCosmosSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (entry.isUser) "You (MindPlayer)" else entry.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (entry.isUser) FontWeight.Bold else FontWeight.Normal,
                                    color = if (entry.isUser) Color(0xFF6366F1) else OnCosmosSecondary
                                )
                            }
                            Text(
                                text = "${entry.score} XP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (index < leaderboard.take(5).size - 1) {
                            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 0.8.dp)
                        }
                    }
                }
            }
        }

        // Recent Mindful logs
        if (recentLogs.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT MINDFUL LOGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnCosmosMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(recentLogs.take(3)) { log ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.prompt,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5B4FC) // Indigo accent
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = log.mood,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399) // Emerald mood
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = log.userResponse,
                            fontSize = 13.sp,
                            color = OnCosmosSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntertainmentPanel(
    onSelectGame: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "🎮 Mind Entertainment",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Quick challenges & creative discovery (1-3 minutes)",
                fontSize = 11.sp,
                color = OnCosmosMuted
            )
        }

        // Game selector cards
        val list = listOf(
            Triple("Dynamic Trivia Burst", "Test your spatial & cosmic knowledge under a 10s countdown timer!", "TRIVIA"),
            Triple("Zen Emoji Puzzles", "Match words with beautiful pictograms to test lateral speed.", "EMOJI"),
            Triple("Auditory Pitch Guessing", "Synthesize dynamic tones & guesses to align acoustic senses.", "PITCH_SYNTH"),
            Triple("AI Welcoming Stories", "Co-write calming stories with interactive pathways & choices.", "STORY")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(list) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .clickable { onSelectGame(game.third) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (game.third == "STORY") Icons.Default.Star else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = game.first,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = game.second,
                                fontSize = 11.sp,
                                color = OnCosmosMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MindfulnessPanel(
    onSelectGame: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "🧘 Calm Mindfulness",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Relaxing mini-games & deep grounding routines",
                fontSize = 11.sp,
                color = OnCosmosMuted
            )
        }

        val list = listOf(
            Triple("Breathing Bubble Pop", "Follow rhythmic expanding bubble, tap on the perfect peak hold.", "BREATHING"),
            Triple("Color Aura Spotting", "Slow focus exercise to spot subtle shifts of cosmic pastels.", "COLOR_SPOT"),
            Triple("Gratitude Spin Wheel", "Rotate the gratitude dial to log feelings & receive positive feedback.", "GRATITUDE_WHEEL")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(list) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .clickable { onSelectGame(game.third) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = game.first,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = game.second,
                                fontSize = 11.sp,
                                color = OnCosmosMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameContainer(
    gameType: String,
    viewModel: MindPlayViewModel,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Game container header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close Game")
            }
            Text(
                text = when (gameType) {
                    "TRIVIA" -> "Trivia Burst"
                    "EMOJI" -> "Emoji Game"
                    "PITCH_SYNTH" -> "Auditory Guess"
                    "STORY" -> "Co-Story Weaver"
                    "BREATHING" -> "Breathing Engine"
                    "COLOR_SPOT" -> "Aura Spotting"
                    "GRATITUDE_WHEEL" -> "Gratitude dial"
                    else -> "Challenge"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(modifier = Modifier.size(48.dp)) // Equalizer spacer
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (gameType) {
                "TRIVIA" -> TriviaGame(viewModel, onClose)
                "EMOJI" -> EmojiGame(viewModel, onClose)
                "PITCH_SYNTH" -> PitchSynthGame(viewModel, onClose)
                "STORY" -> StoryGame(viewModel, onClose)
                "BREATHING" -> BreathingGame(viewModel, onClose)
                "COLOR_SPOT" -> ColorSpotGame(viewModel, onClose)
                "GRATITUDE_WHEEL" -> GratitudeWheelGame(viewModel, onClose)
            }
        }
    }
}

// ---------------- Game Implementations ----------------

@Composable
fun TriviaGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var clickedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var timerSeconds by remember { mutableStateOf(10) }
    var isDone by remember { mutableStateOf(false) }

    val questions = listOf(
        TriviaQ("What sound frequency is closest to the natural resonance of deep pine woods?", listOf("432 Hz", "528 Hz", "150 Hz", "12000 Hz"), 2),
        TriviaQ("Which country invented 'Shinrin-yoku' (Forest Bathing)?", listOf("Korea", "Finland", "Japan", "Norway"), 2),
        TriviaQ("What color wavelength is considered most relaxing to human optical receptors?", listOf("Pure Red", "Deep Cobalt Blue", "Teal-Green", "Violet"), 2),
        TriviaQ("In mindfulness design, what does the 'Anchor' refer to?", listOf("Deep sleep", "A fixed sensory detail like physical breath", "Heavy food", "A weighted stone"), 1)
    )

    // Dynamic timer countdown
    LaunchedEffect(currentQuestionIndex, isDone) {
        if (!isDone) {
            timerSeconds = 10
            while (timerSeconds > 0 && clickedOptionIndex == null) {
                delay(1000)
                timerSeconds--
            }
            if (clickedOptionIndex == null) {
                // Timeout, auto-fail this question
                clickedOptionIndex = -1
                delay(1500)
                if (currentQuestionIndex + 1 < questions.size) {
                    currentQuestionIndex++
                    clickedOptionIndex = null
                } else {
                    isDone = true
                    viewModel.addPoints(score, isMindful = false)
                    viewModel.addLeaderboardEntry("You (Trivia)", score)
                }
            }
        }
    }

    if (isDone) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Trivia Burst Completed!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You answered ${score / 10} of ${questions.size} accurately.", fontSize = 14.sp)
            Text("Score: +$score Points for Balance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("close_trivia_btn")
            ) {
                Text("Collect XP & Return", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        val q = questions[currentQuestionIndex]
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer & Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Challenge Q ${currentQuestionIndex + 1}/${questions.size}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "⌛ ${timerSeconds}s",
                    fontSize = 14.sp,
                    color = if (timerSeconds < 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = timerSeconds.toFloat() / 10f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = q.text,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                )
            }

            // Options list
            q.options.forEachIndexed { optIndex, optText ->
                val isAnswered = clickedOptionIndex != null
                val bgColor = when {
                    !isAnswered -> MaterialTheme.colorScheme.surface
                    optIndex == q.correctIndex -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    optIndex == clickedOptionIndex -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderHighlight = when {
                    !isAnswered -> Color.Transparent
                    optIndex == q.correctIndex -> MaterialTheme.colorScheme.primary
                    optIndex == clickedOptionIndex -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isAnswered) {
                            clickedOptionIndex = optIndex
                            coroutineScope.launch {
                                if (optIndex == q.correctIndex) {
                                    score += 10
                                    AudioSynth.playTone(783.99, 150) // G5 chord chime
                                    delay(50)
                                    AudioSynth.playTone(1046.50, 250) // C6 high bell
                                } else {
                                    AudioSynth.playTone(220.0, 300) // Lower focus tone
                                }
                                delay(1200)
                                if (currentQuestionIndex + 1 < questions.size) {
                                    currentQuestionIndex++
                                    clickedOptionIndex = null
                                } else {
                                    isDone = true
                                    viewModel.addPoints(score, isMindful = false)
                                    viewModel.addLeaderboardEntry("You (Trivia)", score)
                                }
                            }
                        }
                        .border(1.5.dp, borderHighlight, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Text(
                        text = optText,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

data class TriviaQ(val text: String, val options: List<String>, val correctIndex: Int)

@Composable
fun EmojiGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isDone by remember { mutableStateOf(false) }

    val puzzles = listOf(
        EmojiPuzzle("🐝🍃🤷", listOf("Be Free Now", "Honey Wind Whisper", "No Worry Bee", "Honey Bee"), "Be Free Now"),
        EmojiPuzzle("⛈️🌈🌤️", listOf("Rain on roof", "After Storm Calm", "Wet Meadow", "Cloud Rainbow"), "After Storm Calm"),
        EmojiPuzzle("🧘‍♀️🌌💬", listOf("Lotus space", "Meditation Chat", "Cosmic Quiet Mind", "Night Speech"), "Cosmic Quiet Mind")
    )

    if (isDone) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Emoji Puzzles Complete!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total score: +$score Points for Balance", fontSize = 16.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Claim Rewards")
            }
        }
    } else {
        val current = puzzles[step]
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Guess the Mindful Phrase:", fontSize = 14.sp, color = OnCosmosMuted)
            
            // Emoji Display Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(current.emojis, fontSize = 48.sp, letterSpacing = 8.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            current.options.forEach { opt ->
                val chosen = selectedOption == opt
                val isCorrect = opt == current.answer
                val cardBg = when {
                    selectedOption == null -> MaterialTheme.colorScheme.surface
                    chosen && isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    chosen && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    opt == current.answer -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = selectedOption == null) {
                            selectedOption = opt
                            if (isCorrect) {
                                score += 15
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (selectedOption != null && isCorrect) {
                            Icon(Icons.Default.Check, contentDescription = "Correct", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (selectedOption != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (step + 1 < puzzles.size) {
                            step++
                            selectedOption = null
                        } else {
                            isDone = true
                            viewModel.addPoints(score, isMindful = false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Continue to Next Word")
                }
            }
        }
    }
}

data class EmojiPuzzle(val emojis: String, val options: List<String>, val answer: String)

@Composable
fun PitchSynthGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var targetFrequency by remember { mutableStateOf(523.25) } // C5 defaults
    var targetLabel by remember { mutableStateOf("Low Soft Hum") }
    var selectedVal by remember { mutableStateOf<String?>(null) }
    var answeredCorrectly by remember { mutableStateOf(false) }

    // Generate random pitch
    val notes = listOf(
        Pair(261.63, "Deep Earth Ground Note (C4)"),
        Pair(440.0, "Classic Ambient Focus Pitch (A4)"),
        Pair(783.99, "Bright Clear Morning Wave (G5)"),
        Pair(1046.50, "Whistling Zenith Cloud Chime (C6)")
    )

    fun pickNewNote() {
        val item = notes.random()
        targetFrequency = item.first
        targetLabel = item.second
        selectedVal = null
        answeredCorrectly = false
        coroutineScope.launch {
            delay(300)
            AudioSynth.playTone(targetFrequency, 800)
        }
    }

    LaunchedEffect(Unit) {
        pickNewNote()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Close your eyes & listen carefully to the pitch being synthesized. Can you guess its frequency style?",
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    AudioSynth.playTone(targetFrequency, 900)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Play note")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Replay Pure Tone")
        }

        Spacer(modifier = Modifier.height(16.dp))

        notes.forEach { note ->
            val correct = note.second == targetLabel
            val selected = selectedVal == note.second
            val cardBg = when {
                selectedVal == null -> MaterialTheme.colorScheme.surface
                correct -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                selected -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectedVal == null) {
                        selectedVal = note.second
                        if (correct) {
                            answeredCorrectly = true
                            viewModel.addPoints(15, isMindful = false)
                            coroutineScope.launch {
                                delay(200)
                                AudioSynth.playTone(note.first, 200)
                                delay(200)
                                AudioSynth.playTone(note.first * 1.25, 200)
                                delay(200)
                                AudioSynth.playTone(note.first * 1.5, 400)
                            }
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Text(
                    text = note.second,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (selectedVal != null) {
            Text(
                text = if (answeredCorrectly) "Acoustically Aligned! +15 Points" else "A tiny bit off. Replay the note to memorize the frequency!",
                color = if (answeredCorrectly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { pickNewNote() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("New Sound Challenge", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun StoryGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    val state by viewModel.storyState.collectAsStateWithLifecycle()
    val isKeyEmpty = remember { false } // client checking
    var showAILetter by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val s = state) {
            is StoryState.Idle -> {
                Text(
                    "Co-Write Cozy Stories with Gemini AI! Or select local paths to discover beautiful, eye-safe landscapes.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                val landscapes = listOf("Serene Island", "Cyberpunk Lantern Garden")
                landscapes.forEach { l ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.startStoryChallenge(l)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Begin adventure: $l",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            is StoryState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Weaving beautiful sentences from the sky...", fontSize = 12.sp, color = OnCosmosMuted)
                }
            }
            is StoryState.Active -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = s.storyText,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select your path with presence:", fontSize = 12.sp, color = OnCosmosMuted)

                    s.choices.forEachIndexed { i, choice ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.makeStoryChoice(i, isOffline = s.choices.first().startsWith("Follow") || s.choices.first().startsWith("Sit"))
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = choice,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is StoryState.Conclusion -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = s.storyText,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Thank you for exploring! You earned points for active creativity. Your mind is quiet and aligned.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.resetStory() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("New Story", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            Text("Complete & Return")
                        }
                    }
                }
            }
            is StoryState.Error -> {
                Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.resetStory() }) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun BreathingGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var cycle by remember { mutableStateOf("Inhale...") } // "Inhale", "Hold", "Exhale"
    var cyclePoints by remember { mutableStateOf(0) }
    var bubbleScale by remember { mutableStateOf(1f) }
    var userTappedHold by remember { mutableStateOf(false) }

    // Coroutine loop controlling the breathing cycle
    LaunchedEffect(Unit) {
        while (true) {
            // INHALE (4 seconds)
            cycle = "Inhale slowly..."
            userTappedHold = false
            animate(
                initialValue = 1f,
                targetValue = 2.4f,
                animationSpec = tween(4000, easing = LinearOutSlowInEasing)
            ) { value, _ ->
                bubbleScale = value
            }

            // HOLD (4 seconds)
            cycle = "Hold at the peak..."
            delay(4000)

            // EXHALE (4 seconds)
            cycle = "Exhale thoroughly..."
            animate(
                initialValue = 2.4f,
                targetValue = 1f,
                animationSpec = tween(4000, easing = FastOutLinearInEasing)
            ) { value, _ ->
                bubbleScale = value
            }

            // Quiet rest
            cycle = "Rest your lungs..."
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Match your respiration with the expanding bubble. Press 'Anchor Peak Flow' only when holding breath at maximum size!",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                cycle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Beautiful Pulsing Breath Sphere on Canvas with linear blur arcs
        Box(
            modifier = Modifier
                .size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val baseRadius = 45.dp.toPx()
                val currentRadius = baseRadius * bubbleScale
                
                // Outer cyan aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonTeal.copy(alpha = 0.45f),
                            SkyCyan.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    radius = currentRadius * 1.25f,
                    center = center
                )

                // Main breathing bubble
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            NeonTeal,
                            CosmosDarkSurfaceVariant
                        )
                    ),
                    radius = currentRadius,
                    center = center
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (cycle.startsWith("Hold")) "PEAK" else "",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (cycle.startsWith("Hold") && !userTappedHold) {
                        userTappedHold = true
                        cyclePoints += 15
                        viewModel.addPoints(15, isMindful = true)
                        coroutineScope.launch {
                            AudioSynth.playTone(1046.50, 400) // Synthesized high harmonic Bell
                        }
                    } else if (!userTappedHold) {
                        // Bad timing feedback
                        coroutineScope.launch {
                            AudioSynth.playTone(329.63, 200) // E4 flat
                        }
                    }
                },
                enabled = cycle.startsWith("Hold") && !userTappedHold,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("tap_peak_button")
            ) {
                Text(
                    text = if (userTappedHold) "Connected! 🌟" else "Anchor Peak Flow (+15 Mind)",
                    fontWeight = FontWeight.Bold
                )
            }

            Text("Session Mind Points: +$cyclePoints", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnCosmosSecondary)

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("Complete & Save", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ColorSpotGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var level by remember { mutableStateOf(1) }
    var correctCellIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var baseColor by remember { mutableStateOf(Color(0xFF0D9488)) }
    var oddColor by remember { mutableStateOf(Color(0xFF0F766E)) }

    fun generateGrid() {
        // Choose calming theme base: Teals, Blues, Sages, Lavenders
        val r = (10..50).random()
        val g = (140..190).random()
        val b = (140..190).random()
        baseColor = Color(r, g, b)

        // Make the odd one slightly shift depending on level
        val shift = maxOf(4, 30 - level * 3)
        oddColor = Color(
            minOf(255, r + shift),
            minOf(255, g + shift),
            minOf(255, b + shift)
        )
        correctCellIndex = (0..11).random()
    }

    LaunchedEffect(level) {
        generateGrid()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Find the calming, luminous portal (subtly lighter hue). Keeps visual presence focused.", fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Level $level • Score: $score", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        // 4x3 Calming Color matrix
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val isCorrect = index == correctCellIndex
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCorrect) oddColor else baseColor)
                                .clickable {
                                    if (isCorrect) {
                                        score += 10
                                        viewModel.addPoints(10, isMindful = true)
                                        coroutineScope.launch {
                                            // Play incremental scales corresponding to levels
                                            val freq = 523.25 * (1f + level * 0.125f)
                                            AudioSynth.playTone(freq, 150)
                                        }
                                        level++
                                    } else {
                                        coroutineScope.launch {
                                            AudioSynth.playTone(261.63, 200)
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { level++; generateGrid() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Skip Aura", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Finished & Calm")
            }
        }
    }
}

@Composable
fun GratitudeWheelGame(viewModel: MindPlayViewModel, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var selectedPrompt by remember { mutableStateOf<String?>(null) }
    var gratitudeText by remember { mutableStateOf("") }
    var spinnerAngle by remember { mutableStateOf(0f) }

    val promptOptions = listOf(
        "A peaceful sensory sound you noticed recently...",
        "Who is a kindred soul you feel grateful for today...",
        "A comforting, simple food that warmed you...",
        "A quiet space or room you felt safe inside...",
        "A sudden, happy memory that flashed back..."
    )

    // Spin mechanism animation
    fun spinDial() {
        if (isSpinning) return
        isSpinning = true
        selectedPrompt = null
        gratitudeText = ""
        
        val randomOffset = (720..1440).random().toFloat()
        coroutineScope.launch {
            animate(
                initialValue = spinnerAngle,
                targetValue = spinnerAngle + randomOffset,
                animationSpec = tween(2800, easing = FastOutSlowInEasing)
            ) { value, _ ->
                spinnerAngle = value
            }
            
            // Choose prompt based on final normalized angle degrees
            val index = (spinnerAngle.toInt() / 72) % promptOptions.size
            selectedPrompt = promptOptions[index]
            isSpinning = false
            AudioSynth.playTone(880.0, 300) // Clear chime
        }
    }

    val feedback by viewModel.aiReflectionFeedback.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedPrompt == null) {
            Text("Spin the Gratitude dial to receive an active reflection prompt:", fontSize = 13.sp, textAlign = TextAlign.Center)

            // Canvas decorative Wheel
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val radius = size.minDimension / 2
                    val colors = listOf(NeonTeal, SkyCyan, SunsetAmber, CoralPink, NeonTeal.copy(alpha = 0.5f))

                    // Draw Wedges rotated by our state physics angle
                    for (i in 0 until 5) {
                        drawArc(
                            brush = Brush.linearGradient(listOf(colors[i], colors[i].copy(alpha = 0.6f))),
                            startAngle = spinnerAngle + (i * 72f),
                            sweepAngle = 72f,
                            useCenter = true,
                            size = Size(radius * 2, radius * 2),
                            alpha = 0.85f
                        )
                    }

                    // Draw inner brass axle
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = center)
                    drawCircle(SunsetAmber, radius = 5.dp.toPx(), center = center)
                }

                // Dial cursor overlay
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                        .size(24.dp)
                )
            }

            Button(
                onClick = { spinDial() },
                enabled = !isSpinning,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSpinning) "Spinning dial..." else "Spin Gratitude dial")
            }
        } else {
            // Prompts Panel & Response writing
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Prompt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Reflection prompt:", fontSize = 11.sp, color = OnCosmosMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(selectedPrompt!!, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (feedback == null) {
                    OutlinedTextField(
                        value = gratitudeText,
                        onValueChange = { gratitudeText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("gratitude_input"),
                        placeholder = { Text("Log your sincere feelings or sensory details here...", fontSize = 13.sp) },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var chosenMood by remember { mutableStateOf("Calm") }
                        
                        Text("Mood Check-in: ", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        
                        listOf("Calm", "Anxious", "Excited", "Tired").forEach { mood ->
                            val active = chosenMood == mood
                            Button(
                                onClick = { chosenMood = mood },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(mood, fontSize = 11.sp, color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (gratitudeText.isNotEmpty()) {
                                viewModel.processReflection(selectedPrompt!!, gratitudeText, "Calm")
                            }
                        },
                        enabled = gratitudeText.isNotEmpty() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_reflection_btn")
                    ) {
                        Text(if (isLoading) "Consulting Companion..." else "Integrate Reflection (+15 XP)")
                    }
                } else {
                    // Feedback completed
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quiet Companionship Note:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feedback!!,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedPrompt = null
                                gratitudeText = ""
                                viewModel.clearReflectionState()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("New Prompt", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = { viewModel.clearReflectionState(); onClose() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Complete & Return")
                        }
                    }
                }
            }
        }
    }
}
