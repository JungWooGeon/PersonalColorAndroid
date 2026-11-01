package com.gun.personalcolor.feature.tones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.temperatureKo

@Composable
internal fun FavoriteButton(favorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (favorite) "즐겨찾기 해제" else "즐겨찾기 추가",
            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Two equal buttons side by side; stacked when there's not enough room for single-line labels. */
@Composable
internal fun ButtonPair(
    modifier: Modifier = Modifier,
    stackBelow: Dp = 300.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < stackBelow) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.weight(1f)); second(Modifier.weight(1f))
            }
        }
    }
}

/** Compact tappable row: avatar, name, season · temperature, chevron. */
@Composable
internal fun ToneRow(tone: Tone, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Surface(onClick = onClick, color = Color.Transparent, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ToneAvatar(tone, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tone.nameKo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tone.seasonKo} · ${tone.temperatureKo} · ${tone.nameEn}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) trailing()
            else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Tiny fake tones for @Preview only. */
internal fun previewTones(): List<Tone> {
    fun t(id: String, season: String, name: String, metal: String, vararg hex: String): Tone {
        val c = hex.mapIndexed { i, h -> NamedColor("컬러 ${i + 1}", h) }
        return Tone(id, season, name, id, listOf("화사한", "맑은", "산뜻한"), "밝고 맑은 색이 잘 어울리는 타입이에요.", "피부가 밝고 투명해요.",
            c, c.reversed(), Makeup("", c, "", c, "", c, ""), Hair(c, ""), Fashion("", emptyList()), Accessory(metal, ""), "")
    }
    return listOf(
        t("spring_light", "spring", "봄 라이트", "골드", "#FFCBA4", "#F8A98E", "#F7B2B7", "#FFE5A0", "#B5E3D8", "#FFF4E0"),
        t("spring_true", "spring", "봄 웜", "골드", "#FF8C61", "#FFB347", "#F4C430", "#7FB77E", "#40B5AD", "#E97451"),
        t("summer_light", "summer", "여름 라이트", "실버", "#F4C2D7", "#C8A2C8", "#AFCBEA", "#B8E2DC", "#E6E6FA", "#FFE4EC"),
        t("winter_deep", "winter", "겨울 딥", "실버", "#000000", "#800020", "#0F52BA", "#046307", "#FFFFFF", "#4B0082"),
    )
}
