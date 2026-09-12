package com.fiki.ytdownloader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiki.ytdownloader.ui.theme.GlassSurface
import com.fiki.ytdownloader.ui.theme.NeonCyan
import com.fiki.ytdownloader.ui.theme.NeonPurple
import com.fiki.ytdownloader.ui.theme.TextMuted
import com.fiki.ytdownloader.ui.theme.TextPrimary

/**
 * Reusable Frosted Glass Card with iOS squircle shape and specular glass bevel border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (borderColor != null) {
        Brush.verticalGradient(
            listOf(
                borderColor.copy(alpha = 0.5f),
                borderColor.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.2.dp, borderBrush)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Floating iOS Island Dock Navigation Bar
 */
@Composable
fun FloatingGlassDock(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    items: List<Pair<ImageVector, String>>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = NeonCyan.copy(alpha = 0.15f))
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0F1626).copy(alpha = 0.85f))
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedIndex == index
                val interactionSource = remember { MutableInteractionSource() }

                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    label = "DockBg"
                )

                val animatedIconColor by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else TextMuted,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    label = "DockIcon"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(animatedBgColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = animatedIconColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (isSelected) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS-style Segmented Capsule Control
 */
@Composable
fun IOSSegmentedControl(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    options: List<Triple<String, String, ImageVector>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF0A0F1A).copy(alpha = 0.70f))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.03f))
                ),
                RoundedCornerShape(26.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label, icon) ->
            val isSelected = selectedTab == key
            val isVideo = key == "video"

            val bgBrush = if (isSelected) {
                if (isVideo) {
                    Brush.horizontalGradient(
                        listOf(NeonCyan.copy(alpha = 0.28f), NeonCyan.copy(alpha = 0.12f))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(NeonPurple.copy(alpha = 0.32f), NeonPurple.copy(alpha = 0.15f))
                    )
                }
            } else {
                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            }

            val borderStroke = if (isSelected) {
                BorderStroke(
                    1.dp,
                    if (isVideo) NeonCyan.copy(alpha = 0.5f) else NeonPurple.copy(alpha = 0.5f)
                )
            } else null

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(22.dp)) else Modifier)
                    .background(bgBrush)
                    .clickable { onTabSelected(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) (if (isVideo) NeonCyan else NeonPurple) else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
