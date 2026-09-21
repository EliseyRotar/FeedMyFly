package com.example.ui.reels

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

data class ReelItem(
    val titleRes: Int,
    val descRes: Int,
    val author: String,
    val likes: String,
    val audioTrack: String,
    val gradientColors: List<Color>
)

@Composable
fun ReelsView(
    onSwipe: () -> Unit,
    dopamineLevel: Float,
    modifier: Modifier = Modifier
) {
    val reels = listOf(
        ReelItem(
            titleRes = R.string.reel_1_title,
            descRes = R.string.reel_1_desc,
            author = "@fruit_sommelier",
            likes = "142K",
            audioTrack = "Fermentation Beats • 120 BPM",
            gradientColors = listOf(Color(0xFFF39C12), Color(0xFFD35400), Color(0xFF78281F))
        ),
        ReelItem(
            titleRes = R.string.reel_2_title,
            descRes = R.string.reel_2_desc,
            author = "@bulb_bonker",
            likes = "89K",
            audioTrack = "Tungsten Glow • Lo-Fi",
            gradientColors = listOf(Color(0xFFF1C40F), Color(0xFFE67E22), Color(0xFF2C3E50))
        ),
        ReelItem(
            titleRes = R.string.reel_3_title,
            descRes = R.string.reel_3_desc,
            author = "@glass_barrier_enthusiast",
            likes = "215K",
            audioTrack = "Invisible Wall • Mystery Ambience",
            gradientColors = listOf(Color(0xFF3498DB), Color(0xFF2980B9), Color(0xFF1B4F72))
        ),
        ReelItem(
            titleRes = R.string.reel_4_title,
            descRes = R.string.reel_4_desc,
            author = "@sugar_crush_fly",
            likes = "320K",
            audioTrack = "Sweet Sensations • 100 Hz",
            gradientColors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF4A148C))
        ),
        ReelItem(
            titleRes = R.string.reel_5_title,
            descRes = R.string.reel_5_desc,
            author = "@sensilla_care",
            likes = "98K",
            audioTrack = "Grooming ASMR • Clean Wings",
            gradientColors = listOf(Color(0xFF1ABC9C), Color(0xFF16A085), Color(0xFF0E6251))
        ),
        ReelItem(
            titleRes = R.string.reel_6_title,
            descRes = R.string.reel_6_desc,
            author = "@wingbeat_220",
            likes = "410K",
            audioTrack = "Drosophila Flight Frequency 220Hz",
            gradientColors = listOf(Color(0xFF673AB7), Color(0xFF512DA8), Color(0xFF311B92))
        )
    )

    val pagerState = rememberPagerState(pageCount = { reels.size })

    // Inject 1.0 into all SENSORY_REEL neurons for 10 ticks on every swipe
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            onSwipe()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_view_root")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("reels_vertical_pager")
        ) { page ->
            val reel = reels[page]
            ReelCard(reel = reel)
        }

        // Top banner explaining PAM stimulation
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            color = Color(0xCC000000),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reels_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.reels_swipe_hint),
                        color = Color(0xFFFFCC80),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Live dopamine indicator
                Surface(
                    color = Color(0x33FFB300),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PAM: ${(dopamineLevel * 100).toInt()}%",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelCard(reel: ReelItem) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(reel.gradientColors))
    ) {
        // Decorative geometric shapes simulating video graphics
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .background(Color(0x1AFFFFFF), CircleShape)
        )

        // Right side interaction column (TikTok / Reels style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionIcon(icon = Icons.Default.Favorite, label = reel.likes, tint = Color(0xFFFF4081))
            ActionIcon(icon = Icons.Default.ThumbUp, label = "Reward", tint = Color(0xFFFFD54F))
            ActionIcon(icon = Icons.Default.Share, label = "Share", tint = Color.White)
        }

        // Bottom left reel title & author
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.8f)
                .padding(start = 16.dp, bottom = 40.dp)
        ) {
            Text(
                text = reel.author,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(reel.titleRes),
                color = Color(0xFFECEFF1),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(reel.descRes),
                color = Color(0xFFCFD8DC),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = reel.audioTrack,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0x33000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}
