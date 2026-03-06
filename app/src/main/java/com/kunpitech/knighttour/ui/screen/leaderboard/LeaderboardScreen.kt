package com.kunpitech.knighttour.ui.screen.leaderboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.knighttour.ui.theme.AbyssBlack
import com.kunpitech.knighttour.ui.theme.BloodRedBright
import com.kunpitech.knighttour.ui.theme.BorderDefault
import com.kunpitech.knighttour.ui.theme.BorderSubtle
import com.kunpitech.knighttour.ui.theme.BorderWidth
import com.kunpitech.knighttour.ui.theme.CrownGold
import com.kunpitech.knighttour.ui.theme.GoldDim
import com.kunpitech.knighttour.ui.theme.KnightGold
import com.kunpitech.knighttour.ui.theme.KnightTourShapes
import com.kunpitech.knighttour.ui.theme.KnightTourTheme
import com.kunpitech.knighttour.ui.theme.OnlineTeal
import com.kunpitech.knighttour.ui.theme.RedDim
import com.kunpitech.knighttour.ui.theme.RoundedSmall
import com.kunpitech.knighttour.ui.theme.SurfaceDark
import com.kunpitech.knighttour.ui.theme.SurfaceElevated
import com.kunpitech.knighttour.ui.theme.SurfaceHighest
import com.kunpitech.knighttour.ui.theme.TextPrimary
import com.kunpitech.knighttour.ui.theme.TextSecondary
import com.kunpitech.knighttour.ui.theme.TextTertiary
import com.kunpitech.knighttour.ui.theme.VictoryGreen
import com.kunpitech.knighttour.ui.theme.WarningAmber
import com.kunpitech.knighttour.ui.theme.knightType
import kotlinx.coroutines.delay

// ===============================================================
//  KNIGHT TOUR -- LEADERBOARD SCREEN
//
//  Layout:
//    TopBar: back + title + refresh
//    TabRow: GLOBAL / FRIENDS / MY STATS
//    [GLOBAL/FRIENDS] Podium (top 3 special display)
//                     Ranked list (4..N, staggered fade-in)
//                     Your rank sticky footer
//    [MY STATS]       Your full game history
// ===============================================================

@Composable
fun LeaderboardScreen(
    uiState      : LeaderboardUiState,
    onEvent      : (LeaderboardEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Enter animations
    val headerAlpha = remember { Animatable(0f) }
    val tabsAlpha   = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(400, easing = EaseOut))
        delay(60)
        tabsAlpha.animateTo(1f, tween(350, easing = EaseOut))
        delay(80)
        contentAlpha.animateTo(1f, tween(400, easing = EaseOut))
    }

    // Infinite glow pulse
    val inf = rememberInfiniteTransition(label = "lb_inf")
    val pulse by inf.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "lbPulse",
    )

    val activeEntries = when (uiState.selectedTab) {
        LeaderboardTab.GLOBAL   -> uiState.globalEntries
        LeaderboardTab.FRIENDS  -> uiState.friendEntries
        LeaderboardTab.MY_STATS -> uiState.myStatsEntries
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssBlack)
            .systemBarsPadding(),
    ) {

        // Background
        LbBackground(Modifier.fillMaxSize(), pulse)

        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar
            LbTopBar(
                onBack    = onNavigateBack,
                onRefresh = { onEvent(LeaderboardEvent.Refresh) },
                modifier  = Modifier
                    .fillMaxWidth()
                    .alpha(headerAlpha.value)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // Tab Row
            LbTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelect = { onEvent(LeaderboardEvent.TabSelected(it)) },
                modifier    = Modifier
                    .fillMaxWidth()
                    .alpha(tabsAlpha.value)
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Content
            if (uiState.isLoading) {
                LoadingState(
                    modifier = Modifier.fillMaxWidth().weight(1f).alpha(contentAlpha.value),
                    pulse    = pulse,
                )
            } else if (uiState.error != null) {
                ErrorState(
                    message  = uiState.error,
                    onRetry  = { onEvent(LeaderboardEvent.Refresh) },
                    modifier = Modifier.fillMaxWidth().weight(1f).alpha(contentAlpha.value),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .alpha(contentAlpha.value),
                ) {
                    when (uiState.selectedTab) {
                        LeaderboardTab.MY_STATS -> RankedTab(
                            entries    = activeEntries,
                            showPodium = false,
                            modifier   = Modifier.fillMaxSize(),
                        )
                        else -> {
                            when {
                                uiState.selectedTab == LeaderboardTab.FRIENDS &&
                                        activeEntries.isEmpty() -> {
                                    FriendsEmptyState(modifier = Modifier.fillMaxSize())
                                }
                                uiState.selectedTab == LeaderboardTab.MY_STATS &&
                                        activeEntries.isEmpty() -> {
                                    MyStatsEmptyState(modifier = Modifier.fillMaxSize())
                                }
                                else -> {
                                    RankedTab(
                                        entries    = activeEntries,
                                        showPodium = uiState.selectedTab == LeaderboardTab.GLOBAL,
                                        modifier   = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }

                    // Your rank sticky footer (global + friends only)
                    if (uiState.selectedTab != LeaderboardTab.MY_STATS &&
                        uiState.currentUserRank > 0) {
                        YourRankFooter(
                            rank     = uiState.currentUserRank,
                            score    = uiState.currentUserScore,
                            pulse    = pulse,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

// ===============================================================
//  BACKGROUND
// ===============================================================

@Composable
private fun LbBackground(modifier: Modifier, pulse: Float) {
    Canvas(modifier = modifier) {
        val gc = KnightGold.copy(alpha = 0.024f)
        val g  = 38.dp.toPx()
        var x = 0f; while (x <= size.width)  { drawLine(gc, Offset(x,0f), Offset(x,size.height), 1f); x += g }
        var y = 0f; while (y <= size.height) { drawLine(gc, Offset(0f,y), Offset(size.width,y), 1f);   y += g }

        // Top trophy glow
        drawCircle(
            brush  = Brush.radialGradient(
                listOf(CrownGold.copy(alpha = 0.07f * pulse), Color.Transparent),
                center = Offset(size.width / 2f, 0f),
                radius = size.width * 0.8f,
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width / 2f, 0f),
        )
    }
}

// ===============================================================
//  TOP BAR
// ===============================================================

@Composable
private fun LbTopBar(
    onBack    : () -> Unit,
    onRefresh : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LbIconBtn(icon = "\u2039", onClick = onBack)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "\u265A  LEADERBOARD",
                style = MaterialTheme.knightType.ScreenHeader.copy(letterSpacing = 3.sp),
                color = CrownGold,
            )
            Text(
                text  = "GLOBAL RANKINGS",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                color = TextTertiary,
            )
        }

        LbIconBtn(icon = "\u21BA", onClick = onRefresh)
    }
}

// ===============================================================
//  TAB ROW
// ===============================================================

@Composable
private fun LbTabRow(
    selectedTab : LeaderboardTab,
    onTabSelect : (LeaderboardTab) -> Unit,
    modifier    : Modifier = Modifier,
) {
    Row(
        modifier              = modifier
            .background(SurfaceDark, KnightTourShapes.small)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.small)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LeaderboardTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val tabBg by animateFloatAsState(
                targetValue   = if (isSelected) 1f else 0f,
                animationSpec = tween(220),
                label         = "tabBg_${tab.name}",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .background(
                        if (isSelected) GoldDim else Color.Transparent,
                        KnightTourShapes.extraSmall,
                    )
                    .then(
                        if (isSelected)
                            Modifier.border(
                                BorderWidth.thin,
                                KnightGold.copy(alpha = 0.4f),
                                KnightTourShapes.extraSmall,
                            )
                        else Modifier
                    )
                    .clickable { onTabSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = tab.label,
                    style = MaterialTheme.knightType.TabLabel,
                    color = if (isSelected) CrownGold else TextTertiary,
                )
            }
        }
    }
}

// ===============================================================
//  RANKED TAB  (Global / Friends)
// ===============================================================

@Composable
private fun RankedTab(
    entries    : List<LeaderboardEntry>,
    showPodium : Boolean,
    modifier   : Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state       = listState,
        modifier    = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Podium — top 3 only for GLOBAL tab with enough entries
        if (showPodium && entries.size >= 3) {
            item {
                PodiumSection(
                    first    = entries[0],
                    second   = entries[1],
                    third    = entries[2],
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
            }
        }

        // Remaining entries (4+ for global, all for friends)
        val startIdx = if (showPodium && entries.size >= 3) 3 else 0
        itemsIndexed(entries.drop(startIdx)) { idx, entry ->
            RankRow(
                entry    = entry,
                delayMs  = (idx * 40).coerceAtMost(400),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Bottom padding so footer doesn't overlap last row
        item { Spacer(Modifier.height(72.dp)) }
    }
}

// ===============================================================
//  PODIUM (top 3)
// ===============================================================

@Composable
private fun PodiumSection(
    first    : LeaderboardEntry,
    second   : LeaderboardEntry,
    third    : LeaderboardEntry,
    modifier : Modifier = Modifier,
) {
    // Entrance scale
    val scale1 = remember { Animatable(0f) }
    val scale2 = remember { Animatable(0f) }
    val scale3 = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(200)
        scale2.animateTo(1f, tween(380, easing = EaseOutBack))
        delay(60)
        scale1.animateTo(1f, tween(420, easing = EaseOutBack))
        delay(40)
        scale3.animateTo(1f, tween(360, easing = EaseOutBack))
    }

    // Gold sparkle pulse
    val inf = rememberInfiniteTransition(label = "podium")
    val sparkle by inf.animateFloat(
        initialValue  = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sparkle",
    )

    Box(
        modifier          = modifier
            .background(
                Brush.verticalGradient(listOf(GoldDim.copy(alpha = 0.4f), Color.Transparent)),
                KnightTourShapes.medium,
            )
            .border(BorderWidth.thin, KnightGold.copy(alpha = 0.2f), KnightTourShapes.medium)
            .padding(top = 20.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
        contentAlignment  = Alignment.BottomCenter,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom,
        ) {
            // 2nd place
            PodiumPillar(
                entry     = second,
                position  = 2,
                pillarH   = 64.dp,
                scale     = scale2.value,
                sparkle   = sparkle,
                modifier  = Modifier.weight(1f),
            )
            // 1st place — tallest
            PodiumPillar(
                entry     = first,
                position  = 1,
                pillarH   = 96.dp,
                scale     = scale1.value,
                sparkle   = sparkle,
                modifier  = Modifier.weight(1f),
            )
            // 3rd place
            PodiumPillar(
                entry     = third,
                position  = 3,
                pillarH   = 44.dp,
                scale     = scale3.value,
                sparkle   = sparkle,
                modifier  = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PodiumPillar(
    entry    : LeaderboardEntry,
    position : Int,
    pillarH  : Dp,
    scale    : Float,
    sparkle  : Float,
    modifier : Modifier = Modifier,
) {
    val (medalColor, medalIcon) = when (position) {
        1    -> CrownGold    to "\u265A"   // king
        2    -> Color(0xFFB0BEC5) to "\u265D"   // bishop silver
        else -> Color(0xFFCD7F32) to "\u265E"   // knight bronze
    }

    Column(
        modifier            = modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Avatar circle with glow
        Box(
            modifier         = Modifier.size(if (position == 1) 52.dp else 42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f; val cy = size.height / 2f
                if (position == 1) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            listOf(medalColor.copy(alpha = 0.30f * sparkle), Color.Transparent),
                            center = Offset(cx, cy), radius = cx,
                        ),
                        radius = cx, center = Offset(cx, cy),
                    )
                }
                drawCircle(
                    color  = medalColor.copy(alpha = if (position == 1) 0.55f else 0.30f),
                    radius = cx - 2.dp.toPx(), center = Offset(cx, cy),
                    style  = Stroke(if (position == 1) 1.5.dp.toPx() else 1.dp.toPx()),
                )
            }
            Text(medalIcon, fontSize = if (position == 1) 24.sp else 20.sp, color = medalColor)
        }

        Spacer(Modifier.height(4.dp))

        // Name
        Text(
            text      = entry.playerName.take(10),
            style     = MaterialTheme.knightType.StatLabel.copy(
                fontSize = if (position == 1) 11.sp else 10.sp,
                letterSpacing = 1.sp,
            ),
            color     = if (entry.isCurrentUser) KnightGold else TextPrimary,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        // Score
        Text(
            text      = entry.score.toString(),
            style     = MaterialTheme.knightType.StatDisplay.copy(
                fontSize = if (position == 1) 16.sp else 13.sp,
            ),
            color     = medalColor,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

        // Pillar base
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(pillarH)
                .background(
                    Brush.verticalGradient(
                        listOf(medalColor.copy(alpha = 0.18f), medalColor.copy(alpha = 0.06f)),
                    ),
                    KnightTourShapes.extraSmall,
                )
                .border(
                    BorderWidth.thin,
                    medalColor.copy(alpha = 0.30f),
                    KnightTourShapes.extraSmall,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "#$position",
                style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 20.sp),
                color = medalColor.copy(alpha = 0.5f),
            )
        }
    }
}

// ===============================================================
//  RANK ROW  (entries 4+)
// ===============================================================

@Composable
private fun RankRow(
    entry    : LeaderboardEntry,
    delayMs  : Int,
    modifier : Modifier = Modifier,
) {
    val rowAlpha = remember { Animatable(0f) }
    val rowOff   = remember { Animatable(16f) }
    LaunchedEffect(entry.rank) {
        delay(delayMs.toLong())
        rowAlpha.animateTo(1f, tween(300, easing = EaseOut))
        rowOff.animateTo(0f,   tween(300, easing = EaseOutCubic))
    }

    val isUser = entry.isCurrentUser
    val rowBg  = if (isUser) GoldDim else Color.Transparent
    val border = if (isUser)
        Modifier.border(BorderWidth.thin, KnightGold.copy(alpha = 0.4f), KnightTourShapes.extraSmall)
    else Modifier

    Row(
        modifier = modifier
            .alpha(rowAlpha.value)
            .padding(top = rowOff.value.coerceAtLeast(0f).dp)
            .background(rowBg, KnightTourShapes.extraSmall)
            .then(border)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Rank number
        Box(
            modifier         = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "#${entry.rank}",
                style = MaterialTheme.knightType.StatLabel.copy(fontSize = 11.sp),
                color = if (isUser) KnightGold else TextTertiary,
            )
        }

        // Name + rank label
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text     = entry.playerName,
                style    = MaterialTheme.knightType.BodyPrimary.copy(fontSize = 14.sp),
                color    = if (isUser) CrownGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RankPip(entry.rankLabel)
                Text(
                    text  = entry.boardLabel,
                    style = MaterialTheme.knightType.Caption,
                    color = TextTertiary,
                )
            }
        }

        // Time
        Text(
            text  = entry.timeStr,
            style = MaterialTheme.knightType.StatLabel.copy(fontSize = 11.sp),
            color = TextTertiary,
        )

        // Score
        Text(
            text  = entry.score.toString(),
            style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 16.sp),
            color = if (isUser) CrownGold else TextPrimary,
        )
    }

    // Thin separator
    if (!isUser) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(1.dp)
                .background(BorderSubtle),
        )
    }
}

@Composable
private fun RankPip(rankLabel: String) {
    val color = when (rankLabel) {
        "GRANDMASTER" -> CrownGold
        "MASTER"      -> WarningAmber
        "KNIGHT"      -> OnlineTeal
        "SQUIRE"      -> TextSecondary
        else          -> TextTertiary
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedSmall)
            .border(BorderWidth.thin, color.copy(alpha = 0.3f), RoundedSmall)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text  = rankLabel,
            style = MaterialTheme.knightType.BadgeText,
            color = color,
        )
    }
}

// ===============================================================
//  YOUR RANK STICKY FOOTER
// ===============================================================

@Composable
private fun YourRankFooter(
    rank     : Int,
    score    : Int,
    pulse    : Float,
    modifier : Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                Brush.horizontalGradient(listOf(GoldDim, SurfaceDark, GoldDim)),
                KnightTourShapes.medium,
            )
            .border(
                BorderWidth.default,
                KnightGold.copy(alpha = 0.4f * pulse),
                KnightTourShapes.medium,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("\u265E", fontSize = 20.sp, color = KnightGold)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text  = "YOUR RANK",
                    style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 3.sp),
                    color = TextTertiary,
                )
                Text(
                    text  = "#$rank",
                    style = MaterialTheme.knightType.ScreenHeader.copy(fontSize = 18.sp),
                    color = CrownGold,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text  = "BEST SCORE",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 3.sp),
                color = TextTertiary,
            )
            Text(
                text  = score.toString(),
                style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 20.sp),
                color = CrownGold,
            )
        }
    }
}

// ===============================================================
//  PERSONAL BESTS TAB
// ===============================================================

@Composable
private fun PersonalBestsTab(
    entries  : List<LeaderboardEntry>,
    modifier : Modifier = Modifier,
) {
    LazyColumn(
        modifier            = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text  = "YOUR BEST SCORES BY BOARD",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        itemsIndexed(entries) { idx, entry ->
            PersonalBestCard(entry = entry, delayMs = idx * 80)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PersonalBestCard(entry: LeaderboardEntry, delayMs: Int) {
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.94f) }
    LaunchedEffect(entry.rank) {
        delay(delayMs.toLong())
        cardAlpha.animateTo(1f, tween(320, easing = EaseOut))
        cardScale.animateTo(1f, tween(360, easing = EaseOutBack))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha.value)
            .scale(cardScale.value)
            .background(SurfaceDark, KnightTourShapes.medium)
            .border(BorderWidth.thin, BorderDefault, KnightTourShapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GoldDim, KnightTourShapes.extraSmall)
                    .border(BorderWidth.thin, KnightGold.copy(alpha = 0.35f), KnightTourShapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(entry.boardLabel, style = MaterialTheme.knightType.DifficultyChip, color = KnightGold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text  = "BOARD  ${entry.boardLabel}",
                    style = MaterialTheme.knightType.CardTitle.copy(fontSize = 13.sp),
                    color = TextPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = entry.timeStr,
                        style = MaterialTheme.knightType.BodySecondary,
                        color = TextSecondary,
                    )
                    RankPip(entry.rankLabel)
                }
            }
        }

        Text(
            text  = entry.score.toString(),
            style = MaterialTheme.knightType.StatDisplay.copy(fontSize = 22.sp),
            color = CrownGold,
        )
    }
}

// ===============================================================
//  LOADING / ERROR STATES
// ===============================================================

@Composable
private fun FriendsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp),
        ) {
            Text("🌐", fontSize = 40.sp)
            Text(
                text      = "NO OPPONENTS YET",
                style     = MaterialTheme.knightType.ScreenHeader.copy(fontSize = 16.sp),
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = "Play an online game against a friend — their scores will appear here after the match.",
                style     = MaterialTheme.knightType.BodySecondary,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MyStatsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp),
        ) {
            Text("♞", fontSize = 40.sp)
            Text(
                text      = "NO GAMES YET",
                style     = MaterialTheme.knightType.ScreenHeader.copy(fontSize = 16.sp),
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = "Complete a game to see your stats here.",
                style     = MaterialTheme.knightType.BodySecondary,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier, pulse: Float) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "\u265E",
                fontSize = 40.sp,
                color    = KnightGold.copy(alpha = pulse),
            )
            Text(
                text  = "LOADING RANKINGS...",
                style = MaterialTheme.knightType.Eyebrow.copy(letterSpacing = 4.sp),
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("\u26A0", fontSize = 36.sp, color = BloodRedBright)
            Text(
                text  = message,
                style = MaterialTheme.knightType.BodySecondary,
                color = TextTertiary,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .background(GoldDim, KnightTourShapes.small)
                    .border(BorderWidth.thin, KnightGold.copy(alpha = 0.4f), KnightTourShapes.small)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text("RETRY", style = MaterialTheme.knightType.ButtonSecondary, color = KnightGold)
            }
        }
    }
}

// ===============================================================
//  SMALL HELPERS
// ===============================================================

@Composable
private fun LbIconBtn(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(SurfaceElevated, KnightTourShapes.extraSmall)
            .border(BorderWidth.thin, BorderSubtle, KnightTourShapes.extraSmall)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 20.sp, color = TextSecondary)
    }
}

// ===============================================================
//  ROUTE ENTRY POINT
// ===============================================================

@Composable
fun LeaderboardRoute(
    onNavigateBack : () -> Unit,
    viewModel      : LeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LeaderboardScreen(
        uiState       = uiState,
        onEvent       = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

// ===============================================================
//  PREVIEWS
// ===============================================================

private fun stubEntries() = listOf(
    LeaderboardEntry(1,  "Devil_King",  15820, "10x10", "1:12", "GRANDMASTER"),
    LeaderboardEntry(2,  "NightRider",  14390, "8x8",   "0:58", "GRANDMASTER"),
    LeaderboardEntry(3,  "ChessMaster", 13210, "8x8",   "1:04", "GRANDMASTER"),
    LeaderboardEntry(4,  "KnightFall",  11750, "8x8",   "1:22", "MASTER"),
    LeaderboardEntry(5,  "ShadowRook",  10980, "6x6",   "0:44", "MASTER"),
    LeaderboardEntry(6,  "QueenSlayer", 9870,  "8x8",   "1:38", "MASTER"),
    LeaderboardEntry(7,  "L_Mover",     9120,  "6x6",   "0:52", "MASTER"),
    LeaderboardEntry(8,  "BoardWalker", 8450,  "6x6",   "1:01", "KNIGHT"),
    LeaderboardEntry(14, "You",         4948,  "6x6",   "1:34", "KNIGHT", isCurrentUser = true),
)

@Preview(name = "Leaderboard -- Global", showBackground = true, backgroundColor = 0xFF050508, widthDp = 360, heightDp = 800)
@Composable
private fun PreviewGlobal() {
    KnightTourTheme {
        LeaderboardScreen(
            uiState = LeaderboardUiState(
                selectedTab      = LeaderboardTab.GLOBAL,
                globalEntries    = stubEntries(),
                currentUserRank  = 14,
                currentUserScore = 4948,
                isLoading        = false,
            ),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@Preview(name = "Leaderboard -- Personal Bests", showBackground = true, backgroundColor = 0xFF050508, widthDp = 360, heightDp = 800)
@Composable
private fun PreviewPersonal() {
    KnightTourTheme {
        LeaderboardScreen(
            uiState = LeaderboardUiState(
                selectedTab   = LeaderboardTab.MY_STATS,
                myStatsEntries = listOf(
                    LeaderboardEntry(1, "You", 4948, "6x6",   "1:34", "KNIGHT",  isCurrentUser = true),
                    LeaderboardEntry(2, "You", 3820, "5x5",   "1:12", "SQUIRE",  isCurrentUser = true),
                    LeaderboardEntry(3, "You", 2110, "8x8",   "2:58", "NOVICE",  isCurrentUser = true),
                ),
                isLoading = false,
            ),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}