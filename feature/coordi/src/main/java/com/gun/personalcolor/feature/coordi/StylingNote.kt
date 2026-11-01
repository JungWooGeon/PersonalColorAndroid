package com.gun.personalcolor.feature.coordi

/** 틴티's one-liner about today's look. Short, natural, picks a line by [seed]. */
internal fun stylingNote(slots: List<OutfitSlot>, seed: Int): String {
    fun name(s: OutfitSlot) = if (s.fromCloset) s.color.name else s.item
    val main = slots.firstOrNull { it.label == "상의" || it.label == "원피스" }
    // Only a colored piece "brings out" the face; a grey or ivory one doesn't.
    val glow = main?.takeIf { OutfitEngine.lab(it.color.hex).chroma >= 15 }
    val base = slots.firstOrNull { it.label == "하의" } ?: slots.firstOrNull { it.label == "아우터" }
    val outer = slots.firstOrNull { it.label == "아우터" }
    val acc = slots.firstOrNull { it.label == "소품" }
    val point = acc?.takeIf { it.kind != "umbrella" } ?: slots.firstOrNull { it.label == "가방" || it.label == "포인트" }
    val lines = if (acc?.kind == "umbrella") buildList {
        // Weather-neutral: the umbrella can mean rain later on a sunny morning, or snow.
        add("${acc.color.name} 우산까지 챙기면 깔끔해요")
        if (glow != null) add("${glow.color.name}${josa(glow.color.name, "이", "가")} 얼굴을 환하게 살려 줘요")
    } else buildList {
        if (glow != null) add("${glow.color.name}${josa(glow.color.name, "이", "가")} 얼굴 가까이에서 혈색을 살려 줘요")
        if (base != null && point != null) add("${base.color.name}${josa(base.color.name, "으로", "로")} 차분하게 받치고 ${name(point)}${josa(name(point), "으로", "로")} 마무리했어요")
        if (outer != null && main != null && outer.color.name != main.color.name) add("겉은 ${outer.color.name}, 안은 ${main.color.name}. 톤을 맞춰 단정하게 입어요")
        if (acc != null) add("${name(acc)} 하나면 따뜻하고 귀여워요")
    }
    return lines.ifEmpty { listOf("오늘 컬러로 가볍게 맞춰 봤어요") }[Math.floorMod(seed, lines.size.coerceAtLeast(1))]
}

/** Korean particle by the final consonant: josa("카멜", "으로", "로") = "로" (ㄹ takes 로 too). */
internal fun josa(word: String, withFinal: String, withoutFinal: String): String {
    val c = word.trimEnd { !it.isLetterOrDigit() }.lastOrNull() ?: return withoutFinal
    if (c !in '가'..'힣') return withoutFinal
    val jong = (c - '가') % 28
    return if (jong == 0 || (withFinal == "으로" && jong == 8)) withoutFinal else withFinal
}
