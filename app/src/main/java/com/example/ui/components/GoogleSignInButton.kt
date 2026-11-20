package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.appBlackButtonBorder
import com.example.ui.theme.appBlackButtonColors

/**
 * Real multi-color Google "G" mark drawn crisply in Canvas.
 */
@Composable
fun RealGoogleGIcon(
    modifier: Modifier = Modifier.size(24.dp)
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp)
            .semantics { contentDescription = "Google" }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.22f
            val inset = stroke * 0.5f
            val arcSize = Size(w - stroke, h - stroke)
            val topLeft = Offset(inset, inset)

            // Google Blue (top-right down to mid-right)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -45f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Google Green (bottom-right to left)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 45f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Google Yellow (left curve)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 140f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Google Red (top curve)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 210f,
                sweepAngle = 108f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Horizontal blue crossbar completing the "G"
            val midY = h * 0.5f
            val barPath = Path().apply {
                moveTo(w * 0.50f, midY - stroke * 0.48f)
                lineTo(w * 0.96f, midY - stroke * 0.48f)
                lineTo(w * 0.96f, midY + stroke * 0.50f)
                lineTo(w * 0.50f, midY + stroke * 0.50f)
                close()
            }
            drawPath(path = barPath, color = Color(0xFF4285F4))
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = appBlackButtonColors(),
        border = appBlackButtonBorder() ?: BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                RealGoogleGIcon(modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isLoading) "Signing in..." else "Continue with Google",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}
