package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrandSignatureFooter(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Editorial Emotional Hook (Swiggy / Zomato style bold stacked statement)
        Text(
            text = "Crafted for\nyour memories.",
            style = TextStyle(
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.0).sp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF94A3B8), // Crisp slate top
                        Color(0xFFCBD5E1), // Soft fading bottom
                    )
                )
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Subtle Divider Line
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(Color(0xFFCBD5E1))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Brand Wordmark + Made in India Signature Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "COLLAGE STUDIO",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.2.sp,
                color = Color(0xFF94A3B8)
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )

            Text(
                text = "Made with ♥ in India",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
