package com.gun.personalcolor.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * The app's dialog: a white r-xl card with 틴티 leaning in from the top (its expression and prop say what kind of
 * question this is), centered title and line, one full-width primary button and a quiet secondary one.
 * [destructive] paints the primary action in the error color (delete / reset).
 */
@Composable
fun PcDialog(
    title: String,
    message: String?,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = "취소",
    expression: TintyExpression = TintyExpression.Default,
    prop: TintyProp = TintyProp.None,
    destructive: Boolean = false,
) = Dialog(onDismissRequest = onDismiss) {
    Box(Modifier.widthIn(max = 360.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        PcCard(Modifier.fillMaxWidth().padding(top = 52.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLowest, elevation = 2) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = Muted, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(22.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (destructive) PcDangerButton(confirmText, onConfirm, Modifier.fillMaxWidth())
                    else PcButton(confirmText, onConfirm, Modifier.fillMaxWidth())
                    if (dismissText != null) TextButton(onDismiss, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        Tinty(Modifier.size(112.dp).offset(y = 0.dp).tintyEntrance(), expression = expression, prop = prop, animated = false)
    }
}
