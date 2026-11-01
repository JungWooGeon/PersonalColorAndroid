package com.gun.personalcolor.core.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmeTest {
    @Test fun appleEmulatorWithSme2() = assertTrue(hasSme(
        "processor\t: 0\nFeatures\t: fp asimd aes sb bf16 smei16i64 ebf16 sme2 sme2p1 smef16f16\nCPU implementer\t: 0x61\n"))

    @Test fun note20WithoutSme() = assertFalse(hasSme(
        "processor\t: 0\nFeatures\t: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp\n"))

    @Test fun smeOnlyCountsOnFeaturesLine() = assertFalse(hasSme("Hardware\t: sme-board\nFeatures\t: fp asimd\n"))
}
