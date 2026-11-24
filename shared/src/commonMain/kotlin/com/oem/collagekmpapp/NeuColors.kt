package com.oem.collagekmpapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Neumorphism toolkit (pure Compose Multiplatform – no platform shadow APIs).
 *
 *  - dual shadow: light highlight toward the top-left, soft dark shadow toward the bottom-right
 *  - smooth rounded corners
 *  - tactile press: the element sinks (shadows shrink, surface turns concave, tiny scale-down)
 */
object NeuColors {
    val Surface = Color(0xFFEDF2F7)
    val Light = Color(0xFFFFFFFF)
    val Dark = Color(0xFF9FB0C7)
    val Accent = Color(0xFF2563EB)
    val AccentSoft = Color(0xFFDCE8FF)
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val TextMuted = Color(0xFF94A3B8)
}

private const val SHADOW_LAYERS = 7

private fun androidx.compose.ui.graphics.drawscope.DrawScope.neuOutlineRect(
    shape: Shape, grow: Float, dx: Float, dy: Float
): RoundRect {
    val outline = shape.createOutline(size, layoutDirection, this)
    val base = when (outline) {
        is Outline.Rounded -> outline.roundRect
        is Outline.Rectangle -> RoundRect(outline.rect)
        else -> RoundRect(0f, 0f, size.width, size.height, CornerRadius.Zero)
    }
    return RoundRect(
        left = base.left - grow + dx,
        top = base.top - grow + dy,
        right = base.right + grow + dx,
        bottom = base.bottom + grow + dy,
        topLeftCornerRadius = CornerRadius(base.topLeftCornerRadius.x + grow, base.topLeftCornerRadius.y + grow),
        topRightCornerRadius = CornerRadius(base.topRightCornerRadius.x + grow, base.topRightCornerRadius.y + grow),
        bottomRightCornerRadius = CornerRadius(base.bottomRightCornerRadius.x + grow, base.bottomRightCornerRadius.y + grow),
        bottomLeftCornerRadius = CornerRadius(base.bottomLeftCornerRadius.x + grow, base.bottomLeftCornerRadius.y + grow)
    )
}

/**
 * Raised neumorphic surface. [elevation] is the distance of the dual shadow.
 * [pressed] shrinks the shadows and makes the face concave (used by buttons).
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    pressed: Boolean = false,
    color: Color = NeuColors.Surface,
    lightColor: Color = NeuColors.Light,
    darkColor: Color = NeuColors.Dark
): Modifier = this
    .drawBehind {
        val e = elevation.toPx()
        if (e > 0.5f) {
            val blur = e * 1.6f
            for (i in SHADOW_LAYERS downTo 1) {
                val t = i / SHADOW_LAYERS.toFloat()           // 1 = outermost
                val grow = blur * t * 0.55f
                val fade = (1f - t) * 0.9f + 0.1f
                // dark: bottom-right
                val dark = neuOutlineRect(shape, grow, e * 0.55f, e * 0.55f)
                drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(dark) }, darkColor.copy(alpha = 0.075f * fade))
                // light: top-left
                val light = neuOutlineRect(shape, grow, -e * 0.55f, -e * 0.55f)
                drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(light) }, lightColor.copy(alpha = 0.22f * fade))
            }
        }
        // face
        val face = neuOutlineRect(shape, 0f, 0f, 0f)
        val path = androidx.compose.ui.graphics.Path().apply { addRoundRect(face) }
        if (pressed) {
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(darkColor.copy(alpha = 0.35f).compositeOver(color), color, lightColor.copy(alpha = 0.45f).compositeOver(color)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        } else {
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(lightColor.copy(alpha = 0.55f).compositeOver(color), color, darkColor.copy(alpha = 0.12f).compositeOver(color)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        }
    }

private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha
    return Color(
        red = this.red * a + background.red * (1f - a),
        green = this.green * a + background.green * (1f - a),
        blue = this.blue * a + background.blue * (1f - a),
        alpha = 1f
    )
}

/** Sunken (inset) surface – tracks, text wells, segmented-control backgrounds. */
fun Modifier.neumorphicInset(
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = NeuColors.Surface
): Modifier = this.drawBehind {
    val face = neuOutlineRect(shape, 0f, 0f, 0f)
    val path = androidx.compose.ui.graphics.Path().apply { addRoundRect(face) }
    drawPath(
        path,
        Brush.linearGradient(
            listOf(NeuColors.Dark.copy(alpha = 0.38f).compositeOver(color), color, NeuColors.Light.copy(alpha = 0.6f).compositeOver(color)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
}

/** Card: raised neumorphic container. */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .neumorphic(shape = shape, elevation = elevation)
            .clip(shape)
            .padding(contentPadding),
        content = content
    )
}

/** Tactile button. [accent] != null gives a coloured (primary) button. */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(if (pressed) 2.dp else 8.dp, tween(110))
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    val faceColor = accent ?: NeuColors.Surface
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.55f }
            .neumorphic(
                shape = shape,
                elevation = elevation,
                pressed = pressed,
                color = faceColor,
                lightColor = if (accent != null) Color.White.copy(alpha = 0.55f) else NeuColors.Light,
                darkColor = if (accent != null) accent.copy(alpha = 0.55f).compositeOver(NeuColors.Dark) else NeuColors.Dark
            )
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Small raised round icon badge (used for list rows / icon buttons). */
@Composable
fun NeuIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = NeuColors.Accent,
    size: Dp = 40.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .neumorphic(shape = CircleShape, elevation = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Round, tactile icon button for header bars. */
@Composable
fun NeuIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NeuColors.TextPrimary,
    contentDescription: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(if (pressed) 1.dp else 5.dp, tween(100))
    Box(
        modifier = modifier
            .size(42.dp)
            .neumorphic(shape = CircleShape, elevation = elevation, pressed = pressed)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Neumorphic header bar: raised, rounded bottom corners, optional back button and trailing actions. */
@Composable
fun NeuTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)           // room for the shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(shape = shape, elevation = 7.dp)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) NeuIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, contentDescription = "Back")
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NeuColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}

/** Neumorphic switch: sunken track, raised thumb that slides, track tints when on. */
@Composable
fun NeuSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = NeuColors.Accent
) {
    val trackW = 56.dp
    val trackH = 30.dp
    val thumb = 24.dp
    val pad = 3.dp
    val offset by animateDpAsState(if (checked) trackW - thumb - pad else pad, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    val tint by animateColorAsState(if (checked) accent.copy(alpha = 0.28f) else Color.Transparent, tween(200))
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(trackW, trackH)
            .neumorphicInset(shape = CircleShape)
            .background(tint, CircleShape)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset, y = (trackH - thumb) / 2)
                .size(thumb)
                .neumorphic(shape = CircleShape, elevation = 3.dp, color = if (checked) accent else NeuColors.Surface,
                    lightColor = if (checked) Color.White.copy(alpha = 0.5f) else NeuColors.Light)
        )
    }
}

/** Segmented toggle (e.g. JPG / PNG / WebP): sunken track with a raised, animated selected pill. */
@Composable
fun NeuSegmented(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .neumorphicInset(shape = RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            val txt by animateColorAsState(if (isSel) NeuColors.Accent else NeuColors.TextSecondary, tween(180))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(if (isSel) Modifier.neumorphic(shape = RoundedCornerShape(16.dp), elevation = 4.dp) else Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(opt) },
                contentAlignment = Alignment.Center
            ) {
                Text(opt, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = txt)
            }
        }
    }
}