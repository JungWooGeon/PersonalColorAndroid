# Personal-color colorimetry: research notes → quantitative spec

The synthetic training data (`train.py`) comes from this spec. Only §1–§4 are sourced. Each per-tone number in §5 is
**interpolated** from those anchors, because nobody has published 12-tone colorimetry.

## 1. Skin L\*a\*b\* by personal-color type (Korean women, cheek)

| Study | N / method | Finding |
|---|---|---|
| Lee & Park 2012, 복식문화연구 20(2) — [pdf](https://koreascience.kr/article/JAKO201214338812050.pdf) | 136 students, 3 experts with drapes | Warm cheek L 63.5±1.7, a 11.8±1.7, b **17.1±1.6**. Cool cheek L 64.8±2.1, a 10.9±1.6, b **15.0±1.2**. An LDA on `0.406·a* + 0.662·b*` gets **81.6 %** warm/cool. |
| Choi & Bae 2024, Asian J. Beauty Cosmetol. 22(2) — [link](https://www.e-ajbc.org/journal/view.php?number=1507) | 40 (10/season), colorimeter, not cleansed | Spring L 67.6, a 8.9, b 14.8. Summer 67.9 / 9.2 / 11.7. Autumn 62.1 / 10.2 / 16.5. Winter 61.4 / 12.5 / 12.5. **L\* separates light from deep seasons (Δ≈6). b\* separates warm from cool (Δ≈3.5).** |
| Kim, Oh & Lee 2018, JKSCT 42(1) — [pdf](https://koreascience.or.kr/article/JAKO201810237886055.pdf) | **993** women aged 20–30s | Bare cheek L 65.2±2.9, a 9.1±1.7, b 17.9±2.3. Eight types are octants of (Value, b\*, saturation) around V₀=65.2, b₀=18.5, S₀=0.33. |
| Cha & Kim 2011, 대한미용학회지 7(4) — [KCI](https://www.kci.go.kr/kciportal/ci/sereArticleSearch/ciSereArtiView.kci?sereArticleSearchBean.artiId=ART001610421) | 200 women, CM-700d, 4 drape systems | Seasons are separated mainly by L\* and b\*. Results are inconsistent between systems: winter had the highest b\* in one of them. |
| Biomed. Dermatol. 2017 — [link](https://link.springer.com/article/10.1186/s41702-017-0002-7) | 543 women, CM-2600d | L terciles 60.7 / 63.9 / 66.7 (SD ≈2.2). b\* doesn't vary with L. |
| Xiao et al. 2017, Skin Res Technol 23 — [eprint](https://eprints.whiterose.ac.uk/id/eprint/100718/) | Chinese, CM-2600d | Cheek L 58.9, a 11.4, b 14.2. L is about 6 lower than the Korean studies (instrument mode and sex mix). Ethnic groups differ mainly in b\*. |

Takeaways:
- **b\* (yellowness) is the most robust warm/cool cue.** The h° gap between warm and cool is 1.3° in one study and 10° in another.
- **Skin L\*** drives light vs deep.
- **Chroma / clear vs muted** has the weakest evidence.
- Within-group SDs (L≈2, b≈1.5) are about the same size as the gaps between groups, so the groups overlap heavily.

## 2. Hair, iris, lips (population anchors only — no per-type data found)
- **Hair**, Korean men in their 20s: L 15.4±4.0, a 1.4, b 1.3. Measured photographically ([J. Korean Soc. Body & Beauty Art 2012](https://scholar.kyobobook.co.kr/article/detail/4010023433936)). There's a warm-brown cluster at L 18.8, b 6.9.
- **Iris**, Koreans: L 21.6±6.0, a 22, b 29, h≈50°. Taken from slit-lamp photos ([Jang et al. 2018, HIR 24(3)](https://synapse.koreamed.org/articles/1099763)).
- **Lips**: vermilion L 53.3±3.4, a 11.2, b 8.8 by contact CM-700d, which blanches the lip ([record](https://pesquisa.bvsalud.org/gim/resource/ar/wpr-698623)). Lip a\*/b\* correlate with cheek a\*/b\*.
- **Contrast**: the Fujii & von Alten 8-type system adds high/low skin–hair contrast. No numeric threshold has been published.

## 3. Consultant decision rules (theory)
- **Warm / cool** = yellow vs pink-blue undertone. Measured, this means skin b\* (and h°), golden vs ash hair, and coral vs bluish-pink lips.
- **Light / deep** = skin L\* plus how dark the hair and eyes are.
- **Clear (bright) / muted** = skin chroma plus contrast between skin and hair/eyes.
- **12 tones** = a dominant characteristic plus a secondary one. Light Spring = light, then warm. True Spring = warm, then clear. Bright Spring = clear, then warm. Summer Mute = muted, then cool. Autumn Deep = deep, then warm. And so on.
- `ToneClassifier` prototypes already follow this rule: the dominant axis sits at ±1 and the secondary at ±0.5. They are unchanged so the self-quiz behaves the same.
- Korean practice often uses the PCCS-tone-based 8 types (Kim 2018).

## 4. Prevalence and agreement (realistic ceilings)
- **Korean prevalence leans cool.**
  - Cool is 60–77 % in Lee & Park 2012, Shin 2002, Moon 2005 and Kim 2018.
  - Summer is the most common season and spring/autumn the rarest. One survey, Hong 2005, found the opposite.
  - No 12-tone frequencies exist. The prior in `train.py` is a guess consistent with these surveys.
- **Agreement between drape systems** on the same women: Korea vs Japan 64.5 %, UK vs Germany 83 % (Cha 2011). No kappa between consultants has been published.
- **Automated systems trained on real expert labels:**
  - Deep Armocromia (ECCV-W 2024): 55 % on 4 seasons, 32 % on 12 tones.
  - Hierarchical ConvNeXt ([JICCE 2026](https://www.jicce.org/journal/view.html?uid=1349&vmd=Full)): 70 % on 4 seasons, about 48 % on 12 tones end to end.
  - Hand-crafted Lab features with nearest centroid ([Leiden thesis 2026](https://theses.liacs.nl/pdf/2025-2026-DinhNNatascha.pdf)): 31 % on 12 tones, 66 % on undertone.
  - Colorinsight (Korean celebrity photos): 56 % on 4 seasons with ResNet18.
- ⇒ **Published real-world ceilings: warm/cool ≈80 %, 4 seasons ≈55–70 %, 12 tones ≈30–50 %.** Claims of 90 % or more are unverified marketing.

## 5. Quantitative spec used by `train.py` (Korean adults, cheek, spectro-like D65)
Within-tone SDs: skin L 1.8, a 1.4, b 1.3; hair L 3.5, hair b 2.5; iris L 4.5; lip h 6°, lip C 4, lip L 3. These are the sourced population SDs, shrunk a little.

| Tone | skin L | a | b | h° | C\* | hair L | hair b | iris L | skin−hair ΔL | lip h° |
|---|---|---|---|---|---|---|---|---|---|---|
| spring_light | 67.5 | 9.0 | 16.0 | 60.6 | 18.4 | 22 | 8.0 | 27 | 45.5 | 40 |
| spring_true | 65.5 | 9.6 | 18.0 | 61.9 | 20.4 | 19 | 8.5 | 25 | 46.5 | 42 |
| spring_bright | 66.5 | 10.6 | 17.6 | 58.9 | 20.5 | 15 | 6.0 | 22 | 51.5 | 38 |
| summer_light | 68.0 | 8.8 | 13.2 | 56.3 | 15.9 | 21 | 3.5 | 26 | 47.0 | 30 |
| summer_true | 66.0 | 10.4 | 13.4 | 52.2 | 17.0 | 18 | 3.0 | 23 | 48.0 | 27 |
| summer_mute | 63.5 | 9.0 | 13.8 | 56.9 | 16.5 | 19 | 4.0 | 24 | 44.5 | 32 |
| autumn_mute | 63.5 | 9.8 | 16.4 | 59.1 | 19.1 | 19 | 7.0 | 23 | 44.5 | 40 |
| autumn_true | 62.5 | 10.4 | 17.6 | 59.4 | 20.4 | 17 | 7.5 | 22 | 45.5 | 43 |
| autumn_deep | 60.5 | 10.8 | 17.8 | 58.8 | 20.8 | 13 | 5.0 | 18 | 47.5 | 40 |
| winter_bright | 64.5 | 11.0 | 14.2 | 52.2 | 18.0 | 12 | 2.0 | 18 | 52.5 | 25 |
| winter_true | 62.5 | 11.4 | 13.8 | 50.4 | 17.9 | 13 | 1.5 | 19 | 49.5 | 25 |
| winter_deep | 60.5 | 11.6 | 14.6 | 51.5 | 18.6 | 11 | 2.0 | 16 | 49.5 | 28 |

Sourced anchors:
- Population cheek L≈65, b 15.5–17.9.
- Warm − cool Δb\* = +2.1…+3.6.
- Light − deep seasons ΔL\* ≈ +6.
- Hair L≈15, iris L≈22, bare-lip h≈38°.

Everything else is interpolation following the §3 rules. Phone photos differ from spectrophotometers: tone curves, saturation boost, auto-exposure. The simulator adds those as noise, but **a per-camera bias can only be fixed with real labeled data** (README.md).

The rule axes (`PersonalColorAnalyzer.ruleAxes`, UI only) are centred on the spec means (h 56°, b−a 5, L 64.5, darkest feature L 16, C 18.5, contrast 47). Each scale is roughly the gap between tone groups.

## 6. Measurement notes
- **White reference.** Office paper has optical brighteners, so it measures slightly blue (≈L94, a 0.5, b −3). We balance paper to that target instead of to neutral. Assuming neutral paper cost about 2 points of season accuracy in simulation.
- **Sclera** is naturally yellowish and varies between people (assumed b 6±2.5). That variation is the main residual error of sclera-based white balance.
- **Face auto-exposure** pulls skin L\* toward a fixed target, which partly erases the light/deep signal. Hair/iris contrast survives AE, which is why it's a feature.
- **Foundation** pulls skin toward shade No.21–23 (L≈66, a 9.5, b 16.5). Choi & Bae: spring and summer chose No.21, autumn and winter No.23. Foundation is the biggest real-world confound for undertone.
