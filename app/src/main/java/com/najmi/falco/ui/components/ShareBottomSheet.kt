package com.najmi.falco.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.ui.theme.LocalFalcoPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    verdict: Verdict,
    onDismiss: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SHARE VERDICT",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = palette.textGhost,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            ShareOption(
                label = "Share as Text",
                description = "Copy verdict summary to clipboard",
                onClick = {
                    shareAsText(context, verdict)
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ShareOption(
                label = "Share as Image",
                description = "Generate and share verdict card",
                onClick = {
                    // Image sharing would require bitmap generation
                    shareAsText(context, verdict)
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ShareOption(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    val palette = LocalFalcoPalette.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(palette.chip)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary
        )
        Text(
            text = description,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = palette.textMuted
        )
    }
}

private fun shareAsText(context: Context, verdict: Verdict) {
    val text = buildString {
        appendLine("FALCO Verdict")
        appendLine("=============")
        appendLine()
        appendLine("Claim: ${verdict.summary.take(100)}...")
        appendLine()
        appendLine("Verdict: ${verdict.lean.name}")
        appendLine("Confidence: ${(verdict.confidence * 100).toInt()}%")
        appendLine()
        appendLine("Summary: ${verdict.summary}")
        if (verdict.temporalWarning != null) {
            appendLine()
            appendLine(verdict.temporalWarning)
        }
    }
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Verdict"))
}
