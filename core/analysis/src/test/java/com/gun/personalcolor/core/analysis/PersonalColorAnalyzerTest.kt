package com.gun.personalcolor.core.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class PersonalColorAnalyzerTest {

    // ---------- ColorMath ----------

    @Test
    fun knownLabValues() {
        val white = ColorMath.rgbToLab(Rgb(255, 255, 255))
        assertEquals(100.0, white.l, 0.01)
        assertEquals(0.0, white.a, 0.01)
        assertEquals(0.0, white.b, 0.01)
        val red = ColorMath.rgbToLab(Rgb(255, 0, 0))
        assertEquals(53.24, red.l, 0.05)
        assertEquals(80.09, red.a, 0.05)
        assertEquals(67.20, red.b, 0.05)
        assertEquals(0.0, ColorMath.rgbToLab(Rgb(0, 0, 0)).l, 0.01)
    }

    @Test
    fun rgbLabRoundTrip() {
        for (r in 0..255 step 15) for (g in 0..255 step 15) for (b in 0..255 step 15) {
            val c = Rgb(r, g, b)
            assertEquals(c, ColorMath.labToRgb(ColorMath.rgbToLab(c)))
        }
        assertEquals(Rgb(255, 255, 255), ColorMath.labToRgb(Lab(120.0, 0.0, 0.0))) // clamped
    }

    // ---------- ToneClassifier ----------

    @Test
    fun classifyIsADistribution() {
        val grid = listOf(-1.0, -0.5, 0.0, 0.5, 1.0)
        for (w in grid) for (v in grid) for (c in grid) {
            val s = ToneClassifier.classify(Axes(w, v, c))
            assertEquals(12, s.size)
            assertEquals(ToneClassifier.TONE_IDS.toSet(), s.map { it.toneId }.toSet())
            assertEquals(1.0, s.sumOf { it.probability }, 1e-9)
            assertTrue(s.zipWithNext().all { (a, b) -> a.probability >= b.probability })
            assertTrue(s.first().probability < 0.8)
        }
    }

    @Test
    fun prototypesClassifyToThemselvesWithoutBeingOverconfident() {
        val protos = mapOf(
            "spring_light" to Axes(0.5, 0.9, 0.3), "spring_true" to Axes(1.0, 0.3, 0.5),
            "spring_bright" to Axes(0.5, 0.2, 1.0), "summer_light" to Axes(-0.5, 0.9, 0.0),
            "summer_true" to Axes(-1.0, 0.3, -0.2), "summer_mute" to Axes(-0.4, 0.0, -0.9),
            "autumn_mute" to Axes(0.4, 0.0, -0.9), "autumn_true" to Axes(1.0, -0.4, -0.3),
            "autumn_deep" to Axes(0.5, -0.9, 0.0), "winter_bright" to Axes(-0.5, 0.0, 1.0),
            "winter_true" to Axes(-1.0, -0.2, 0.5), "winter_deep" to Axes(-0.5, -0.9, 0.3),
        )
        assertEquals(protos.keys.toList(), ToneClassifier.TONE_IDS)
        for ((id, axes) in protos) {
            val top = ToneClassifier.classify(axes).first()
            assertEquals(id, top.toneId)
            assertTrue("$id top=${top.probability}", top.probability in 0.3..0.7)
        }
    }


    // ---------- Analyzer on synthetic faces (colors from tools/model/train.py SPEC means) ----------

    private fun polar(l: Double, h: Double, c: Double) = Lab(l, c * cos(Math.toRadians(h)), c * sin(Math.toRadians(h)))

    private fun pixels(lab: Lab, n: Int, noise: Double, rnd: Random) = List(n) {
        ColorMath.labToRgb(Lab(lab.l + rnd.nextGaussian() * noise, lab.a + rnd.nextGaussian() * noise, lab.b + rnd.nextGaussian() * noise))
    }

    /** Synthetic face with realistic junk mixed in: speculars + shadows on skin, lashes + vessels in sclera. */
    private fun face(skin: Lab, hair: Lab, iris: Lab, lips: Lab, seed: Long = 1, sclera: Lab = Lab(82.0, 1.5, 6.0)): FaceSamples {
        val rnd = Random(seed)
        fun region() = pixels(skin, 80, 1.5, rnd) + pixels(Lab(97.0, 0.0, 3.0), 8, 1.0, rnd) +
            pixels(Lab(skin.l - 30, skin.a, skin.b), 8, 2.0, rnd)
        val regions = mapOf("cheekL" to region(), "cheekR" to region(), "forehead" to region(), "chin" to region())
        val hairPatches = List(4) { pixels(hair, 30, 2.5, rnd) }
        return FaceSamples(
            skin = regions.values.flatten(),
            sclera = pixels(sclera, 60, 1.5, rnd) + pixels(Lab(15.0, 1.0, 2.0), 15, 2.0, rnd) +
                pixels(Lab(65.0, 30.0, 15.0), 8, 2.0, rnd),
            iris = pixels(iris, 40, 3.0, rnd) + pixels(Lab(95.0, 0.0, 0.0), 4, 1.0, rnd),
            hair = hairPatches.flatten(),
            lips = pixels(lips, 60, 2.5, rnd),
            skinRegions = regions,
            hairPatches = hairPatches,
        )
    }

    private val springSkin = Lab(65.5, 9.6, 18.0)
    private val springFace = face(skin = springSkin, hair = Lab(19.0, 4.0, 8.5), iris = Lab(25.0, 6.0, 13.0), lips = polar(54.0, 42.0, 32.0))
    private val summerFace = face(skin = Lab(67.0, 9.6, 13.2), hair = Lab(20.0, 2.0, 3.5), iris = Lab(25.0, 5.0, 9.0), lips = polar(55.0, 28.0, 29.0))
    private val winterFace = face(skin = Lab(62.5, 11.4, 13.8), hair = Lab(12.0, 1.0, 1.5), iris = Lab(18.0, 4.0, 7.0), lips = polar(50.0, 25.0, 31.0))
    private val autumnFace = face(skin = Lab(62.5, 10.4, 17.6), hair = Lab(17.0, 4.0, 7.5), iris = Lab(22.0, 6.0, 12.0), lips = polar(50.0, 43.0, 30.0))
    private val faces = mapOf("spring" to springFace, "summer" to summerFace, "autumn" to autumnFace, "winter" to winterFace)

    private fun success(s: FaceSamples) = PersonalColorAnalyzer.analyze(s) as AnalysisResult.Success
    private fun season(r: AnalysisResult.Success): String = r.seasonScores.keys.first()

    @Test
    fun prototypicalFacesGetTheirSeason() {
        for ((expected, f) in faces) {
            val r = success(f)
            assertEquals(expected, season(r))
            assertEquals(1.0, r.scores.sumOf { it.probability }, 1e-9)
            assertEquals(1.0, r.seasonScores.values.sum(), 1e-9)
            assertTrue(r.confidence in 0.0..1.0)
            assertEquals(WhiteBalanceSource.SCLERA, r.features.whiteBalance)
        }
    }

    @Test
    fun explanationAxesPointTheRightWay() {
        assertTrue(success(springFace).axes.warmth > 0)
        assertTrue(success(autumnFace).axes.warmth > 0)
        assertTrue(success(summerFace).axes.warmth < 0)
        assertTrue(success(winterFace).axes.warmth < 0)
        assertTrue(success(winterFace).axes.value < success(summerFace).axes.value)
        assertTrue(success(winterFace).features.contrast > 40)
        assertNotNull(success(winterFace).features.hair)
    }

    @Test
    fun deterministic() {
        for (f in faces.values) {
            val (a, b) = success(f) to success(f)
            assertEquals(a.scores, b.scores)
            assertEquals(a.axes, b.axes)
            assertEquals(a.confidence, b.confidence, 0.0)
        }
    }

    @Test
    fun kotlinModelMatchesPython() {
        val x = ToneModelWeights.PARITY_X
        val n = x.size / 3
        for (row in 0 until 3) {
            val p = ToneModel.probabilities(x.copyOfRange(row * n, row * n + n))
            for (k in 0 until 12) assertEquals(ToneModelWeights.PARITY_P[row * 12 + k], p[k], 1e-4)
        }
    }

    @Test
    fun robustSkinIgnoresSpecularsAndShadows() {
        val skin = success(springFace).features.skin
        val truth = springSkin
        assertEquals(truth.l, skin.l, 1.5)
        assertEquals(truth.a, skin.a, 1.5)
        assertEquals(truth.b, skin.b, 1.5)
    }

    @Test
    fun shadowedRegionIsSkippedAndLowersQuality() {
        val rnd = Random(9)
        val skin = springSkin
        val regions = springFace.skinRegions + ("cheekR" to pixels(Lab(skin.l - 20, skin.a - 2, skin.b - 4), 80, 1.5, rnd))
        val r = success(springFace.copy(skinRegions = regions, skin = regions.values.flatten()))
        assertEquals(skin.l, r.features.skin.l, 1.5)
        assertTrue(r.features.lightingAsymmetry > 15)
        assertTrue(r.features.quality < success(springFace).features.quality)
        assertEquals("spring", season(r))
    }

    // ---------- White balance ----------

    /** Multiplies sRGB channels, like a lighting cast (8-bit, clipped). */
    private fun FaceSamples.cast(kr: Double, kg: Double, kb: Double): FaceSamples {
        fun List<Rgb>.c() = map {
            Rgb((it.r * kr).roundToInt().coerceIn(0, 255), (it.g * kg).roundToInt().coerceIn(0, 255), (it.b * kb).roundToInt().coerceIn(0, 255))
        }
        return FaceSamples(skin.c(), sclera.c(), iris.c(), hair.c(), lips.c(), whiteRef.c(),
            skinRegions.mapValues { it.value.c() }, hairPatches.map { it.c() })
    }

    /** Planckian illuminant at [kelvin], [residual] of it left after the camera's own AWB, applied in linear light. */
    private fun FaceSamples.illuminant(kelvin: Double, residual: Double): FaceSamples {
        val t = kelvin
        val x = if (t <= 4000) -0.2661239e9 / (t * t * t) - 0.2343589e6 / (t * t) + 0.8776956e3 / t + 0.179910
        else -3.0258469e9 / (t * t * t) + 2.1070379e6 / (t * t) + 0.2226347e3 / t + 0.240390
        val y = if (t <= 4000) -0.9549476 * x * x * x - 1.37418593 * x * x + 2.09137015 * x - 0.16748867
        else 3.0817580 * x * x * x - 5.87338670 * x * x + 3.75112997 * x - 0.37001483
        val xyz = doubleArrayOf(x / y, 1.0, (1 - x - y) / y)
        val r = 3.2404542 * xyz[0] - 1.5371385 * xyz[1] - 0.4985314 * xyz[2]
        val g = -0.9692660 * xyz[0] + 1.8760108 * xyz[1] + 0.0415560 * xyz[2]
        val b = 0.0556434 * xyz[0] - 0.2040259 * xyz[1] + 1.0572252 * xyz[2]
        val k = doubleArrayOf(r / g, 1.0, b / g).map { Math.pow(it, residual) }
        fun ch(v: Int, m: Double): Int {
            val lin = ColorMath.toLinear(v) * m
            return ColorMath.labToRgb(ColorMath.linearToLab(lin, lin, lin)).r // linear → 8-bit sRGB via a grey round trip
        }
        fun List<Rgb>.c() = map { Rgb(ch(it.r, k[0]), ch(it.g, k[1]), ch(it.b, k[2])) }
        return FaceSamples(skin.c(), sclera.c(), iris.c(), hair.c(), lips.c(), whiteRef.c(),
            skinRegions.mapValues { it.value.c() }, hairPatches.map { it.c() })
    }

    @Test
    fun whiteBalanceUndoesColorCast() {
        for (f in faces.values) {
            val base = success(f)
            for ((kr, kb) in listOf(1.0 to 1.10, 1.04 to 0.90)) { // blue, yellow cast
                val cast = success(f.cast(kr, 1.0, kb))
                assertEquals(season(base), season(cast))
                assertEquals(base.axes.warmth, cast.axes.warmth, 0.3) // sRGB-space multiply ≠ a linear cast
                assertTrue(cast.features.whiteBalanceGains.any { abs(it - 1) > 0.05 })
            }
        }
    }

    @Test
    fun seasonSurvivesSimulatedIlluminantsWithWhiteBalance() {
        for ((expected, f) in faces) for (kelvin in listOf(4000.0, 5000.0, 6500.0, 7000.0)) {
            val r = success(f.illuminant(kelvin, residual = 0.4))
            assertEquals("$expected @ ${kelvin}K", expected, season(r))
        }
    }

    @Test
    fun withoutWhiteBalanceWarmLightFlipsCoolFaces() {
        // The same warm (4000 K, half-corrected) light with no eye whites to balance against: the cool face reads warm.
        val noRef = summerFace.illuminant(4000.0, residual = 0.5).copy(sclera = emptyList())
        val r = success(noRef)
        assertEquals(WhiteBalanceSource.NONE, r.features.whiteBalance)
        assertTrue(r.seasonScores.getValue("summer") < success(summerFace).seasonScores.getValue("summer"))
    }

    @Test
    fun whitePaperBeatsScleraAndFixesStrongCasts() {
        val rnd = Random(5)
        val paper = pixels(Lab(78.0, 0.0, 0.0), 80, 0.8, rnd) + pixels(Lab(40.0, 10.0, 15.0), 20, 2.0, rnd) // shaded paper + fingers
        // Strong warm cast that sclera alone can't be trusted for, plus a yellowish sclera.
        val f = face(skin = Lab(67.0, 9.6, 13.2), hair = Lab(20.0, 2.0, 3.5), iris = Lab(25.0, 5.0, 9.0),
            lips = polar(55.0, 28.0, 29.0), sclera = Lab(80.0, 2.0, 12.0)).copy(whiteRef = paper)
        val base = success(f)
        assertEquals(WhiteBalanceSource.WHITE_REF, base.features.whiteBalance)
        assertEquals("summer", season(base))
        val warm = success(f.illuminant(3000.0, residual = 0.6))
        assertEquals(WhiteBalanceSource.WHITE_REF, warm.features.whiteBalance)
        assertEquals("summer", season(warm))
        assertEquals(base.features.skin.hue, warm.features.skin.hue, 3.0)
    }

    @Test
    fun blownOutPaperFallsBackToSclera() {
        val paper = List(60) { Rgb(255, 255, 255) }
        assertEquals(WhiteBalanceSource.SCLERA, success(springFace.copy(whiteRef = paper)).features.whiteBalance)
    }

    @Test
    fun noScleraMeansNoWhiteBalance() {
        val r = success(springFace.copy(sclera = emptyList()))
        assertTrue(r.features.whiteBalanceGains.contentEquals(doubleArrayOf(1.0, 1.0, 1.0)))
        assertEquals(WhiteBalanceSource.NONE, r.features.whiteBalance)
        assertTrue(r.confidence < success(springFace).confidence)
    }

    // ---------- Quality gates / reliability ----------

    @Test
    fun qualityGates() {
        assertEquals(AnalysisResult.Rejected(QualityIssue.NO_SKIN), PersonalColorAnalyzer.analyze(springFace.copy(skin = emptyList())))
        assertEquals(AnalysisResult.Rejected(QualityIssue.NO_SKIN), PersonalColorAnalyzer.analyze(FaceSamples(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())))
        val rnd = Random(2)
        val dark = pixels(Lab(10.0, 5.0, 8.0), 200, 1.5, rnd)
        assertEquals(AnalysisResult.Rejected(QualityIssue.TOO_DARK), PersonalColorAnalyzer.analyze(springFace.copy(skin = dark, skinRegions = emptyMap())))
        val bright = pixels(Lab(97.0, 3.0, 5.0), 200, 1.0, rnd)
        assertEquals(AnalysisResult.Rejected(QualityIssue.TOO_BRIGHT), PersonalColorAnalyzer.analyze(springFace.copy(skin = bright, skinRegions = emptyMap())))
        assertEquals(AnalysisResult.Rejected(QualityIssue.STRONG_COLOR_CAST), PersonalColorAnalyzer.analyze(springFace.cast(1.0, 1.0, 0.6)))
    }

    @Test
    fun unreliableHairIsIgnored() {
        val rnd = Random(3)
        val skin = springSkin
        // "Hair" patches that are really forehead skin.
        val forehead = List(3) { pixels(Lab(skin.l - 2, skin.a, skin.b), 30, 2.0, rnd) }
        assertNull(success(springFace.copy(hair = forehead.flatten(), hairPatches = forehead)).features.hair)
        // Wildly inconsistent patches (background clutter).
        val clutter = List(3) { List(30) { Rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(80)) } }
        assertNull(success(springFace.copy(hair = clutter.flatten(), hairPatches = clutter)).features.hair)
        // One good patch among junk is enough.
        val mixed = forehead.take(2) + listOf(pixels(Lab(22.0, 3.0, 6.0), 30, 2.0, rnd))
        assertEquals(22.0, success(springFace.copy(hair = mixed.flatten(), hairPatches = mixed)).features.hair!!.l, 2.0)
    }

    @Test
    fun lipstickIsFlaggedAndDoesNotFlipTheSeason() {
        val rnd = Random(4)
        val red = pixels(polar(45.0, 20.0, 58.0), 60, 2.0, rnd)
        val r = success(summerFace.copy(lips = red))
        assertTrue(r.features.lipMakeup)
        assertEquals("summer", season(r))
    }

    // ---------- Multi-photo + logging ----------

    @Test
    fun combineAveragesProbabilities() {
        val a = success(springFace)
        val b = success(face(skin = Lab(66.0, 9.4, 17.6), hair = Lab(20.0, 4.0, 8.0), iris = Lab(26.0, 6.0, 12.0), lips = polar(54.0, 40.0, 31.0), seed = 2))
        val c = PersonalColorAnalyzer.combine(listOf(a, b))
        assertEquals(1.0, c.scores.sumOf { it.probability }, 1e-9)
        assertEquals("spring", season(c))
        assertEquals(a, PersonalColorAnalyzer.combine(listOf(a)))
    }

    @Test
    fun measurementVectorIsAnonymousNumbers() {
        val v = success(winterFace).features.measurementVector()
        for (k in listOf("skin_l", "skin_a", "skin_b", "skin_h", "skin_c", "hair_l", "iris_l", "contrast", "lip_h", "wb_r", "wb_src", "quality")) {
            assertTrue(k, k in v)
        }
        assertTrue(v.values.all { it.isFinite() })
    }
}
