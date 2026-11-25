package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.domain.RiskTier
import com.example.ui.theme.LocalMontanteColors
import kotlin.math.max

@Composable
fun TeamCrest(
    name: String,
    logoUrl: String?,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(semantic.cardElevated)
            .border(1.dp, semantic.borderSubtle, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Logo $name",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size - 8.dp)
                    .clip(CircleShape),
                error = {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProbabilityMeterBar(
    probability: Double,
    riskTier: RiskTier,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    val targetFraction = probability.toFloat().coerceIn(0.05f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
        label = "prob_bar"
    )
    val barColor = when (riskTier) {
        RiskTier.SAFE_MONTANTE -> semantic.safeEmerald
        RiskTier.BALANCED -> semantic.boostAmber
        RiskTier.VALUE_HIGH -> semantic.riskCrimson
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(barColor.copy(alpha = 0.75f), barColor)
                    )
                )
        )
    }
}

@Composable
fun RiskTierPill(
    tier: RiskTier,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    val (bg, fg) = when (tier) {
        RiskTier.SAFE_MONTANTE -> semantic.safeContainer to semantic.safeEmerald
        RiskTier.BALANCED -> semantic.boostContainer to semantic.boostAmber
        RiskTier.VALUE_HIGH -> semantic.riskContainer to semantic.riskCrimson
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(99.dp),
        modifier = modifier
    ) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MontanteScoreBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    val color = when {
        score >= 78 -> semantic.safeEmerald
        score >= 62 -> semantic.boostAmber
        else -> semantic.riskCrimson
    }
    val container = when {
        score >= 78 -> semantic.safeContainer
        score >= 62 -> semantic.boostContainer
        else -> semantic.riskContainer
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
            Text(
                text = "INDICE",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun FormStreakRow(
    form: String,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    val chars = form.takeLast(5).padStart(5, '-')
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        chars.forEach { ch ->
            val (label, bg, fg) = when (ch.uppercaseChar()) {
                'W' -> Triple("V", semantic.safeContainer, semantic.safeEmerald)
                'D' -> Triple("N", semantic.boostContainer, semantic.boostAmber)
                'L' -> Triple("D", semantic.riskContainer, semantic.riskCrimson)
                else -> Triple("-", MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
            }
        }
    }
}

@Composable
fun BankrollCurveChart(
    points: List<Double>,
    targetAmount: Double,
    modifier: Modifier = Modifier
) {
    val semantic = LocalMontanteColors.current
    val lineColor = semantic.safeEmerald
    val targetColor = semantic.boostAmber
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val safePoints = if (points.size >= 2) points else listOf(points.firstOrNull() ?: 0.0, points.firstOrNull() ?: 0.0)
    val maxVal = max(targetAmount, (safePoints.maxOrNull() ?: 10.0) * 1.1).coerceAtLeast(10.0)
    val minVal = 0.0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
    ) {
        val w = size.width
        val h = size.height
        val padH = 12.dp.toPx()
        val padV = 14.dp.toPx()
        val chartW = (w - padH * 2).coerceAtLeast(1f)
        val chartH = (h - padV * 2).coerceAtLeast(1f)

        // Subtle horizontal grid lines
        for (i in 0..2) {
            val y = padV + chartH * (i / 2f)
            drawLine(
                color = gridColor.copy(alpha = 0.6f),
                start = Offset(padH, y),
                end = Offset(w - padH, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Target line
        val targetNorm = ((targetAmount - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
        val targetY = padV + chartH * (1f - targetNorm)
        drawLine(
            color = targetColor.copy(alpha = 0.85f),
            start = Offset(padH, targetY),
            end = Offset(w - padH, targetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )

        val coords = safePoints.mapIndexed { index, value ->
            val x = if (safePoints.size == 1) padH else padH + chartW * (index.toFloat() / (safePoints.size - 1))
            val norm = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
            val y = padV + chartH * (1f - norm)
            Offset(x, y)
        }

        val linePath = Path().apply {
            coords.forEachIndexed { idx, pt ->
                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
        }
        val fillPath = Path().apply {
            coords.forEachIndexed { idx, pt ->
                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
            lineTo(coords.last().x, padV + chartH)
            lineTo(coords.first().x, padV + chartH)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.34f), Color.Transparent),
                startY = padV,
                endY = padV + chartH
            )
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        coords.forEach { pt ->
            drawCircle(color = PitchDotBorder, radius = 5.dp.toPx(), center = pt)
            drawCircle(color = lineColor, radius = 3.2.dp.toPx(), center = pt)
        }
    }
}

private val PitchDotBorder = Color(0xFF0A1016)
