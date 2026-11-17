package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Official Google Identity pill Sign-In button matching the clean white pill appearance
 * with a crisp 4-color Google "G" mark and "Sign in with Google" label.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val googleButtonBg = Color(0xFFFFFFFF)
    val googleButtonBorder = Color(0xFFDADCE0)
    val googleButtonText = Color(0xFF3C4043)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, googleButtonBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = googleButtonBg,
            contentColor = googleButtonText,
            disabledContainerColor = googleButtonBg.copy(alpha = 0.85f),
            disabledContentColor = googleButtonText.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
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
                    color = Color(0xFF4285F4),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                GoogleGLogo(modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isLoading) "Signing in..." else "Sign in with Google",
                color = googleButtonText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.15.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    val bluePath = remember {
        PathParser().parsePathString(
            "M22.56,12.25c0,-0.78 -0.07,-1.53 -0.2,-2.25H12v4.26h5.92c-0.26,1.37 -1.04,2.53 -2.21,3.31v2.77h3.57c2.08,-1.92 3.28,-4.74 3.28,-8.09z"
        ).toPath()
    }
    val greenPath = remember {
        PathParser().parsePathString(
            "M12,23c2.97,0 5.46,-0.98 7.28,-2.66l-3.57,-2.77c-0.98,0.66 -2.23,1.06 -3.71,1.06c-2.86,0 -5.29,-1.93 -6.16,-4.53H2.18v2.84C3.99,20.53 7.7,23 12,23z"
        ).toPath()
    }
    val yellowPath = remember {
        PathParser().parsePathString(
            "M5.84,14.09c-0.22,-0.66 -0.35,-1.36 -0.35,-2.09s0.13,-1.43 0.35,-2.09V7.07H2.18C1.43,8.55 1,10.22 1,12s0.43,3.45 1.18,4.93l2.85,-2.22l0.81,-0.62z"
        ).toPath()
    }
    val redPath = remember {
        PathParser().parsePathString(
            "M12,5.38c1.62,0 3.06,0.56 4.21,1.64l3.15,-3.15C17.45,2.09 14.97,1 12,1C7.7,1 3.99,3.47 2.18,7.07l3.66,2.84c0.87,-2.6 3.3,-4.53 6.16,-4.53z"
        ).toPath()
    }

    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        scale(scaleX = sx, scaleY = sy, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path = bluePath, color = Color(0xFF4285F4))
            drawPath(path = greenPath, color = Color(0xFF34A853))
            drawPath(path = yellowPath, color = Color(0xFFFBBC05))
            drawPath(path = redPath, color = Color(0xFFEA4335))
        }
    }
}
