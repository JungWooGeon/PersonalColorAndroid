> 참고: 앱에서 Firebase를 제거해 지금은 측정값 수집 경로가 없다. 아래 재학습 절차는 나중에 정답 데이터를 다시 모을 때를 위한 기록이다.

# Tone model

- `RESEARCH.md` holds the literature notes and the per-tone spec.
- `train.py` simulates synthetic faces, trains the model, evaluates it, and exports `core/analysis/.../ToneModelWeights.kt`.
- `ToneModel.kt` holds the hand-written inference code and feature vector. Keep it in sync with `features()` in train.py.

```
python3 tools/model/train.py              # numpy only, ~10 s; prints metrics and rewrites ToneModelWeights.kt
./gradlew :core:analysis:test             # includes a Python↔Kotlin parity test on 3 vectors
```

When you change the spec, features or analyzer constants, keep these mirrors in sync:
- `SCLERA_TARGET`, `PAPER_TARGET`, gain clamps and reject ratios
- reliability rules
- `FILL`
- rule-axis constants

## Collection → retraining loop (opt-in, anonymous)

1. **Collect.** The app calls `Features.measurementVector()` and sends an event only when the user has opted in. Send the numbers only, never pixels or images. Suggested event: Firebase Analytics `pc_measure` with:
   - all vector keys (`skin_l, skin_a, skin_b, skin_h, skin_c, hair_l/a/b, iris_l/a/b, lip_l, lip_h, lip_c, contrast, wb_r/g/b, wb_src, asym, lip_makeup, quality`), rounded to 0.1. Analytics has a 25-parameter limit, so split the vector or drop `*_a/*_b` of hair/iris if needed.
   - `label`: the tone id the user reports from a **professional in-person diagnosis**. Self-quiz results and "I think I'm…" answers must not be used.
   - `label_src`: `expert`.
   - `model`: `ToneModelWeights.VERSION`.
2. **Export.** Link Firebase to BigQuery (free tier is enough) and run:
   ```sql
   SELECT (SELECT value.string_value FROM UNNEST(event_params) WHERE key='label') AS tone,
          (SELECT value.double_value FROM UNNEST(event_params) WHERE key='skin_l') AS skin_l,
          -- …one line per vector key…
   FROM `project.analytics_XXXX.events_*` WHERE event_name = 'pc_measure'
     AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key='label_src') = 'expert'
   ```
   Save the result as `labeled.csv`. Required columns: `tone` plus the vector keys; empty cells mean the feature is missing.
3. **Retrain.**
   ```
   python3 tools/model/train.py --real labeled.csv --real-weight 5
   ```
   Real rows are added to the synthetic set with a higher weight, so real data gradually takes over: when real rows × weight ≫ synthetic rows, it dominates.
   - **Before shipping:** hold out 20 % of the real rows (split by user, not by photo). Report season, warm/cool and 12-tone top-2 accuracy on that holdout. That number is the first honest real-world accuracy.
   - **With ~1–2k real rows:** also refit the spec means/SDs (§5) from them, so the synthetic part matches phones instead of spectrophotometers.
4. **Ship.** Commit the regenerated `ToneModelWeights.kt` (the VERSION string changes), then run the unit tests.

Privacy:
- Get explicit opt-in.
- Send numbers only: there's nothing in them to re-identify a face.
- Don't log a user id together with the vector. Firebase's app-instance id is enough for de-duplication and the by-user split.
