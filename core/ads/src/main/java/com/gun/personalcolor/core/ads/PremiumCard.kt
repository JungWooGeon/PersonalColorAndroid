package com.gun.personalcolor.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.designsystem.DropShape
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.aurora
import kotlinx.coroutines.launch

/**
 * "프리미엄" one-time purchase (no ads + unlimited daily outfit + premium tools) + restore, shared by 설정 and 마이.
 * [compactWhenOwned]: once bought, show a quiet "프리미엄 이용 중" line instead of the big card. Hidden in previews.
 */
@Composable
fun PremiumSection(snackbar: SnackbarHostState, modifier: Modifier = Modifier, compactWhenOwned: Boolean = false) {
    val billing = LocalAdManager.current?.billing ?: return
    val activity = LocalActivity.current
    val adFree by billing.adFree.collectAsStateWithLifecycle()
    val price by billing.price.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    if (adFree && compactWhenOwned) {
        Text("프리미엄 이용 중", modifier.padding(horizontal = 4.dp), style = Muted)
        return
    }
    PremiumCard(
        adFree, price, restoring, modifier,
        onPurchase = {
            val started = activity != null && billing.purchase(activity)
            if (!started) scope.launch { snackbar.showSnackbar("지금은 구매할 수 없어요. 잠시 후 다시 시도해 주세요.") }
        },
        onRestore = {
            if (restoring) return@PremiumCard
            restoring = true
            billing.restore { r ->
                val msg = when (r) {
                    RestoreResult.RESTORED -> "프리미엄을 다시 적용했어요"
                    RestoreResult.NOTHING -> "복원할 구매 내역이 없어요"
                    RestoreResult.FAILED -> "Google Play에 연결하지 못했어요. 잠시 후 다시 시도해 주세요."
                }
                scope.launch { restoring = false; snackbar.showSnackbar(msg) }
            }
        },
    )
}

/** Soft aurora card with the drop corner: benefits + one-time purchase, or 틴티 cheering once it's bought. */
@Composable
private fun PremiumCard(adFree: Boolean, price: String?, restoring: Boolean, modifier: Modifier, onPurchase: () -> Unit, onRestore: () -> Unit) {
    Column(modifier) {
        PcCard(Modifier.fillMaxWidth(), shape = DropShape, elevation = 2) {
            Box(Modifier.aurora(PcTheme.colors)) {
                if (adFree) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Tinty(Modifier.size(88.dp), expression = TintyExpression.Cheer, animated = false, prop = TintyProp.Crown)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Eyebrow("프리미엄")
                            Text("프리미엄 이용 중", style = MaterialTheme.typography.titleLarge)
                            Text("광고 없이 모든 기능을 쓰고 있어요", style = Muted)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Eyebrow("프리미엄")
                                Text("광고 없이 더 편하게", style = MaterialTheme.typography.titleLarge)
                            }
                            Tinty(Modifier.size(84.dp), expression = TintyExpression.Happy, animated = false, prop = TintyProp.Crown)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("광고 없이 보기", "오늘의 코디 무제한", "립 체험·리포트 바로 보기").forEach { Benefit(it) }
                        }
                        Text((price?.let { "$it · " } ?: "") + "한 번만 결제하면 계속 써요", style = Muted)
                        PcButton("프리미엄 시작하기", onPurchase, Modifier.fillMaxWidth())
                        TextButton(onRestore, Modifier.align(Alignment.CenterHorizontally), enabled = !restoring) {
                            if (restoring) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("확인 중…")
                            } else Text("구매 복원")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Benefit(text: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = PcTheme.colors.best)
    Spacer(Modifier.width(8.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Preview
@Composable
private fun PremiumPreview() = PcTheme {
    Column { PremiumCard(false, "₩3,900", false, Modifier, {}, {}); PremiumCard(true, null, false, Modifier, {}, {}) }
}
