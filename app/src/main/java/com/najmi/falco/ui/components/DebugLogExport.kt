package com.najmi.falco.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.najmi.falco.data.local.DebugLogBuffer
import com.najmi.falco.ui.theme.FalcoTypography
import com.najmi.falco.ui.theme.FalcoZeroShape
import com.najmi.falco.ui.theme.LocalFalcoPalette

@Composable
fun DebugLogExportButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFalcoPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.chip)
            .border(1.dp, palette.divider, FalcoZeroShape)
            .clickable {
                exportDebugLog(context)
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "[EXPORT]",
                style = FalcoTypography.labelSmall,
                color = palette.textMuted
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Export Debug Log",
                    style = FalcoTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.textBody
                )
                Text(
                    "Share session data as text",
                    style = FalcoTypography.labelSmall,
                    color = palette.textGhost
                )
            }
        }
    }
}

private fun exportDebugLog(context: Context) {
    val logText = DebugLogBuffer.formatForExport()
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Falco Debug Log")
        putExtra(Intent.EXTRA_TEXT, logText)
    }
    
    context.startActivity(Intent.createChooser(intent, "Export Debug Log"))
}
