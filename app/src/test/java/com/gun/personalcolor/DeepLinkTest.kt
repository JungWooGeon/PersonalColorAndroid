package com.gun.personalcolor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {
    private val tones = setOf("spring_light", "winter_deep")

    private fun parse(
        openExtra: String? = null, scheme: String? = null, host: String? = null, path: String? = null, tone: String? = null,
    ) = parseDeepLink(openExtra, scheme, host, path, tone) { it in tones }

    private fun web(path: String) = parse(scheme = "https", host = "personalcolorotest.netlify.app", path = path)
    private fun kakao(host: String = "kakaolink", tone: String?) = parse(scheme = "kakao123abc", host = host, tone = tone)

    @Test fun coordiExtraWinsOverData() =
        assertEquals(DeepLink.Coordi, parse(openExtra = "coordi", scheme = "https", path = "/r/spring_light"))

    @Test fun otherExtraIsIgnored() = assertNull(parse(openExtra = "home"))

    @Test fun noIntentData() = assertNull(parse())

    @Test fun webLink() {
        assertEquals(DeepLink.ToneLink("spring_light"), web("/r/spring_light"))
        assertEquals(DeepLink.ToneLink("spring_light"), web("/r/spring_light/"))
        assertEquals(DeepLink.ToneLink("winter_deep"), parse(scheme = "http", host = "personalcolorotest.netlify.app", path = "/r/winter_deep"))
    }

    @Test fun webLinkInvalid() {
        assertNull(web("/r/unknown_tone"))
        assertNull(web("/r/"))
        assertNull(web("/x/spring_light"))
        assertNull(web(""))
        assertNull(parse(scheme = "https", path = null))
    }

    @Test fun kakaoLink() = assertEquals(DeepLink.ToneLink("winter_deep"), kakao(tone = "winter_deep"))

    @Test fun kakaoLinkInvalid() {
        assertNull(kakao(tone = "nope"))
        assertNull(kakao(tone = null))
        assertNull(kakao(host = "elsewhere", tone = "winter_deep"))
    }

    @Test fun webSchemeIgnoresToneParam() =
        assertNull(parse(scheme = "https", host = "kakaolink", path = "/", tone = "spring_light"))

    @Test fun unknownSchemeWithoutKakaoHost() = assertNull(parse(scheme = "ftp", host = "example.com", tone = "spring_light"))

    @Test fun webLinkFromAnotherSiteIsIgnored() {
        assertNull(parseDeepLink(null, "https", "evil.example.com", "/r/autumn_mute", null) { true })
    }
}
