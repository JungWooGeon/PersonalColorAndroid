package com.gun.personalcolor.view.screen

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.gun.personalcolor.state.AdState
import com.gun.personalcolor.state.MainState
import com.gun.personalcolor.view.composable.WebView

@Composable
fun AiTestScreen() {
    val context = LocalContext.current
    var adState by remember { mutableStateOf<AdState>(AdState.Idle) }
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }
    var isRewardEarned by remember { mutableStateOf(false) }
    var setupAdCallbacks by remember { mutableStateOf<((RewardedAd) -> Unit)?>(null) }
    var onLoadAd by remember { mutableStateOf<(() -> Unit)?>(null) }

    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserCallback = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // 파일 선택 콜백에 URI 배열을 전달
                fileChooserCallback.value?.onReceiveValue(arrayOf(it))
            } ?: fileChooserCallback.value?.onReceiveValue(null)
            fileChooserCallback.value = null
        }
    )

    LaunchedEffect(Unit) {
        setupAdCallbacks = { ad ->
            ad.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    rewardedAd = null

                    if (isRewardEarned) {
                        // 보상을 받았을 때만 사진 선택기 실행
                        fileChooserCallback.value = filePathCallback
                        fileChooserLauncher.launch("image/*")
                        isRewardEarned = false
                        onLoadAd?.let { it() }
                    } else {
                        adState = AdState.Idle
                        onLoadAd?.let { it() }
                        Toast.makeText(context, "광고 시청을 취소하였습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    rewardedAd = null
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
        }

        onLoadAd = {
            Log.d(TAG, "Load Ads")
            if (!isAdLoading) {
                isAdLoading = true
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    "ca-app-pub-3940256099942544/5224354917",
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isAdLoading = false
                            adState = AdState.Error(adError.message)
                        }

                        override fun onAdLoaded(ad: RewardedAd) {
                            isAdLoading = false
                            rewardedAd = ad

                            setupAdCallbacks?.let { it(ad) }
                            Log.e(TAG, rewardedAd.toString())
                            adState = AdState.Loaded
                        }
                    }
                )
            }
        }

        onLoadAd?.let { it() }
    }

    AiTestScreen(
        adState = adState,
        onShowFileChooser = { callback ->
            filePathCallback = callback
            rewardedAd?.let { ad ->
                ad.show(context as Activity) { rewardItem ->
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                    Log.d(TAG, "User earned the reward. amount: $rewardAmount, rewardType: $rewardType")
                    isRewardEarned = true
                }
            } ?: run {
                Log.d(TAG, "The rewarded ad wasn't ready yet.")
                Toast.makeText(context, "광고를 불러오는 중입니다. 잠시 후 시도해주세요.", Toast.LENGTH_SHORT).show()
                onLoadAd?.let { it() }
            }
        }
    )
}

@Composable
fun AiTestScreen(
    adState: AdState,
    onShowFileChooser: ((ValueCallback<Array<Uri>>) -> Unit)? = null
) {
    when (adState) {
        is AdState.Loaded -> {
            WebView(
                url = MainState.AiTest.screenRoute,
                onShowFileChooser = onShowFileChooser
            )
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private const val TAG = "AiTestScreen"