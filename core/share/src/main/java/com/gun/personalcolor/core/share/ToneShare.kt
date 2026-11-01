package com.gun.personalcolor.core.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.gun.personalcolor.core.model.Tone
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import java.io.File

/** KakaoTalk feed share (rich card that opens the app) with a plain Android share-sheet fallback. */
object ToneShare {
    const val WEB_BASE = "https://tintycolor.com"

    private var kakaoReady = false

    /** Call once from Application.onCreate. A blank key leaves Kakao off (share-sheet fallback only). */
    fun init(context: Context, kakaoNativeAppKey: String) {
        if (kakaoNativeAppKey.isBlank()) return
        kakaoReady = runCatching { KakaoSdk.init(context, kakaoNativeAppKey) }.isSuccess
    }

    fun pageUrl(toneId: String) = "$WEB_BASE/r/$toneId"

    fun share(activity: Activity, tone: Tone, imageFile: File? = null) {
        if (kakaoReady && ShareClient.instance.isKakaoTalkSharingAvailable(activity)) shareKakao(activity, tone, imageFile)
        else shareSheet(activity, tone, imageFile)
    }

    private fun shareKakao(activity: Activity, tone: Tone, imageFile: File?) {
        val app = Link(androidExecutionParams = mapOf("tone" to tone.id))
        val web = Link(webUrl = pageUrl(tone.id), mobileWebUrl = pageUrl(tone.id))
        val template = FeedTemplate(
            content = Content(
                title = "나의 퍼스널컬러는 ${tone.nameKo}!",
                description = tone.summary,
                imageUrl = "$WEB_BASE/r/og/${tone.id}.png",
                imageWidth = 1200, imageHeight = 630,
                link = Link(webUrl = pageUrl(tone.id), mobileWebUrl = pageUrl(tone.id), androidExecutionParams = mapOf("tone" to tone.id)),
            ),
            buttons = listOf(Button("나도 진단하기", app), Button("결과 보기", web)),
        )
        ShareClient.instance.shareDefault(activity, template) { result, error ->
            if (result != null) runCatching { activity.startActivity(result.intent) }.onSuccess { return@shareDefault }
            Log.w("ToneShare", "Kakao share failed", error)
            shareSheet(activity, tone, imageFile)
        }
    }

    private fun shareSheet(activity: Activity, tone: Tone, imageFile: File?) {
        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "나의 퍼스널컬러는 ${tone.nameKo}! 너의 컬러도 알려줘 👉 ${pageUrl(tone.id)}")
            if (imageFile != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity, "${activity.packageName}.files", imageFile))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(send, "결과 공유"))
    }
}
