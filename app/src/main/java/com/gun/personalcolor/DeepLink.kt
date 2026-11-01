package com.gun.personalcolor

/** Where an incoming intent should land. */
internal sealed interface DeepLink {
    data class ToneLink(val id: String) : DeepLink
    data object Coordi : DeepLink
}

/**
 * kakao{KEY}://kakaolink?tone=<id>, https://…/r/<id>, or the "open=coordi" extra (notification).
 * Web links are only accepted from the app's own site (also filtered by the manifest's App Links intent filter).
 * [path] is the decoded URI path; empty segments are dropped like Uri.getPathSegments().
 */
internal fun parseDeepLink(
    openExtra: String?, scheme: String?, host: String?, path: String?, toneParam: String?, isValidTone: (String) -> Boolean,
): DeepLink? {
    if (openExtra == "coordi") return DeepLink.Coordi
    if (scheme == null) return null
    val id = when (scheme) {
        "https", "http" -> if (host != SITE_HOST) null else path.orEmpty().split('/').filter { it.isNotEmpty() }.takeIf { it.size >= 2 && it[0] == "r" }?.get(1)
        else -> if (host == "kakaolink") toneParam else null
    }
    return id?.takeIf(isValidTone)?.let { DeepLink.ToneLink(it) }
}


internal const val SITE_HOST = "personalcolorotest.netlify.app"
