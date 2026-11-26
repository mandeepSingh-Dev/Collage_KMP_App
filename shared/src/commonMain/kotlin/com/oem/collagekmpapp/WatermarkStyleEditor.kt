package com.oem.collagekmpapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// commonMain — shared by Settings and the Export screen so both show the same controls + preview.

/**
 * Pick what to stamp (App Name / Signature / Date), edit the signature or date format, and see a live preview.
 *
 * @param onSaveAsDefault when non-null a "Save as default" link is shown (Export screen quick edits).
 */
@Composable
fun WatermarkStyleEditor(
    type: WatermarkType,
    text: String,
    datePattern: String,
    onTypeChange: (WatermarkType) -> Unit,
    onTextChange: (String) -> Unit,
    onDatePatternChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSaveAsDefault: (() -> Unit)? = null
) {
    val stamp = watermarkStampText(type, text, datePattern)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Stamp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextSecondary)
        NeuSegmented(
            options = WatermarkType.entries.map { it.label },
            selected = type.label,
            onSelect = { label -> WatermarkType.entries.firstOrNull { it.label == label }?.let(onTypeChange) },
            modifier = Modifier.fillMaxWidth()
        )

        when (type) {
            WatermarkType.APP_NAME -> Text(
                text = "${type.emoji}  Stamps \"$APP_WATERMARK_TEXT\" on your exports",
                fontSize = 12.sp,
                color = NeuColors.TextMuted
            )

            WatermarkType.SIGNATURE -> OutlinedTextField(
                value = text,
                onValueChange = { onTextChange(it.replace('\n', ' ').take(WATERMARK_MAX_LENGTH)) },
                singleLine = true,
                placeholder = { Text("@mybrand or By Alex") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeuColors.TextMuted) },
                supportingText = { Text("${text.length}/$WATERMARK_MAX_LENGTH", fontSize = 11.sp) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuColors.Accent,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            WatermarkType.DATE -> {
                // label shown = today's date in that pattern, e.g. "2026.11.26"
                val samples = remember { WATERMARK_DATE_PATTERNS.associateBy { currentDateStamp(it) } }
                Text("Date Format", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextSecondary)
                NeuSegmented(
                    options = samples.keys.toList(),
                    selected = samples.entries.firstOrNull { it.value == datePattern }?.key ?: samples.keys.first(),
                    onSelect = { label -> samples[label]?.let(onDatePatternChange) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        WatermarkStampPreview(stamp)

        if (onSaveAsDefault != null) {
            TextButton(onClick = onSaveAsDefault, modifier = Modifier.align(Alignment.End)) {
                Text("Save as default", fontWeight = FontWeight.Bold, color = NeuColors.Accent, fontSize = 13.sp)
            }
        }
    }
}

/** Small "photo" strip showing exactly how the stamp will look (white, semi-transparent, soft shadow). */
@Composable
fun WatermarkStampPreview(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF475569))))
    ) {
        Text(
            "Preview",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
        )
        Text(
            text = text,
            style = watermarkTextStyle(14.sp),
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
        )
    }
}

/**
 * Draws the stamp over an export preview at the same relative size/position as the exporter
 * (text = 3.2 % of width, margin = 3 % of width, bottom-right).
 */
@Composable
fun BoxScope.WatermarkOverlay(text: String) {
    if (text.isBlank()) return
    BoxWithConstraints(Modifier.matchParentSize()) {
        val fontSize = with(LocalDensity.current) { (maxWidth * 0.032f).toSp() }
        val margin = maxWidth * 0.03f
        Text(
            text = text,
            style = watermarkTextStyle(if (fontSize.value < 7f) 7.sp else fontSize),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = margin, bottom = margin)
        )
    }
}

/** Matches the exporter's Paint: white @ alpha 215, bold, soft dark shadow. */
private fun watermarkTextStyle(size: TextUnit) = TextStyle(
    color = Color.White.copy(alpha = 215f / 255f),
    fontSize = size,
    fontWeight = FontWeight.Bold,
    shadow = Shadow(color = Color(0x99000000), offset = Offset(0f, 2f), blurRadius = 6f)
)