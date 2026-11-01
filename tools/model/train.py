#!/usr/bin/env python3
"""
Trains the on-device 12-tone model from a synthetic, literature-grounded dataset (numpy only).

  python3 tools/model/train.py                       # synthetic only, prints metrics, writes ToneModelWeights.kt
  python3 tools/model/train.py --real labeled.csv    # mix in real labeled measurement vectors (see README.md)
  python3 tools/model/train.py --no-export           # metrics only

Pipeline per synthetic face (mirrors PersonalColorAnalyzer at region level):
  per-tone truth (SPEC, RESEARCH.md §5) → makeup / hair dye / missing features
  → illuminant (Planckian 3000–7000 K + green/magenta tint) partly undone by the camera's own AWB
  → camera exposure + face auto-exposure + camera "look" (saturation) → 8-bit-ish clipping + sampling noise
  → our white balance (paper > sclera > none, same gains/clamps/rejects as Kotlin) → features → model.

SYNTHETIC ACCURACY IS NOT REAL-WORLD ACCURACY: it only measures how separable the SPEC distributions are
and how well the measurement pipeline survives the simulated nuisances. Real photos add domain shift
(camera tone curves, beauty filters, mixed lighting) that only real labeled data can measure.
"""
import argparse
import csv
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_KT = os.path.join(HERE, "..", "..", "core", "analysis", "src", "main", "java", "com", "gun", "personalcolor",
                      "core", "analysis", "ToneModelWeights.kt")

# Same order as ToneClassifier.TONE_IDS.
TONES = ["spring_light", "spring_true", "spring_bright", "summer_light", "summer_true", "summer_mute",
         "autumn_mute", "autumn_true", "autumn_deep", "winter_bright", "winter_true", "winter_deep"]
SEASONS = ["spring", "summer", "autumn", "winter"]
WARM = {"spring", "autumn"}

# ---- Quantitative spec (RESEARCH.md §5). (mean, sd) per tone; Korean adults, spectrophotometer-like D65 Lab. ----
# skin = cheek; h°/C* parametrise a*/b*. hair = natural (undyed) hair; iris = photo L* of the iris ring.
# Columns: skin L*a*b* (cheek), natural hair L* and b*, iris L*, lip h° (signed), lip C*, lip L*.
# Skin means: interpolated per tone from Lee&Park 2012 (warm/cool), Choi&Bae 2024 (4 seasons), Kim et al. 2018
# (8 types, N=993 population norms). Within-tone SDs are the sourced population SDs shrunk a little (L 1.8,
# a 1.4, b 1.3) so the tones overlap as heavily as the real data do. Hair/iris/lip per-tone values are
# interpolated from population anchors only (no per-type data exists) — see RESEARCH.md §5.
SPEC_COLS = ["skinL", "skinA", "skinB", "hairL", "hairB", "irisL", "lipH", "lipC", "lipL"]
_SD = [1.8, 1.4, 1.3, 3.5, 2.5, 4.5, 6.0, 4.0, 3.0]
_MEANS = {
    #                skinL  skinA  skinB  hairL hairB irisL  lipH  lipC  lipL
    "spring_light":  [67.5,  9.0, 16.0,  22.0, 8.0,  27.0, 40.0, 30.0, 56.0],
    "spring_true":   [65.5,  9.6, 18.0,  19.0, 8.5,  25.0, 42.0, 32.0, 54.0],
    "spring_bright": [66.5, 10.6, 17.6,  15.0, 6.0,  22.0, 38.0, 34.0, 53.0],
    "summer_light":  [68.0,  8.8, 13.2,  21.0, 3.5,  26.0, 30.0, 29.0, 56.0],
    "summer_true":   [66.0, 10.4, 13.4,  18.0, 3.0,  23.0, 27.0, 29.0, 54.0],
    "summer_mute":   [63.5,  9.0, 13.8,  19.0, 4.0,  24.0, 32.0, 25.0, 53.0],
    "autumn_mute":   [63.5,  9.8, 16.4,  19.0, 7.0,  23.0, 40.0, 26.0, 52.0],
    "autumn_true":   [62.5, 10.4, 17.6,  17.0, 7.5,  22.0, 43.0, 30.0, 50.0],
    "autumn_deep":   [60.5, 10.8, 17.8,  13.0, 5.0,  18.0, 40.0, 29.0, 48.0],
    "winter_bright": [64.5, 11.0, 14.2,  12.0, 2.0,  18.0, 25.0, 34.0, 51.0],
    "winter_true":   [62.5, 11.4, 13.8,  13.0, 1.5,  19.0, 25.0, 31.0, 50.0],
    "winter_deep":   [60.5, 11.6, 14.6,  11.0, 2.0,  16.0, 28.0, 30.0, 47.0],
}
SPEC = {t: list(zip(m, _SD)) for t, m in _MEANS.items()}
# Prior: Korean surveys are cool-majority (60-77%), summer most common, spring/autumn rarest (RESEARCH.md §4).
# Used for sampling, so it's baked into the model's probabilities.
PRIOR = np.array([0.07, 0.05, 0.05, 0.14, 0.12, 0.13, 0.08, 0.06, 0.05, 0.07, 0.09, 0.09])
PRIOR = PRIOR / PRIOR.sum()

# ---- Mirrors of PersonalColorAnalyzer constants (keep in sync) ----
SCLERA_TARGET = (85.0, 1.5, 5.0)
PAPER_TARGET = (94.0, 0.5, -3.0)
SCLERA_GAIN = (0.6, 1.7, 2.4)   # min, max, reject ratio
PAPER_GAIN = (0.45, 2.2, 3.5)
PAPER_CLIPPED, PAPER_TOO_DARK = 0.97, 0.05
HAIR_MIN_DARKER_L, HAIR_MIN_DE, IRIS_MIN_DARKER_L = 10.0, 15.0, 10.0
LIP_MAKEUP_C = 42.0

# ---- Model feature vector (mirror of ToneModel.vector in Kotlin; keep in sync) ----
FEATURES = ["skinL", "skinA", "skinB", "skinH", "skinC", "hairL", "hairB", "hasHair", "irisL", "hasIris",
            "contrast", "lipH", "lipC", "hasLip"]
FILL = {"hairL": 17.0, "hairB": 5.0, "irisL": 22.0, "contrast": 47.0, "lipH": 33.0, "lipC": 30.0}

# ---------------- color math (same constants as ColorMath.kt) ----------------
M = np.array([[0.4124564, 0.3575761, 0.1804375], [0.2126729, 0.7151522, 0.0721750], [0.0193339, 0.1191920, 0.9503041]])
MINV = np.array([[3.2404542, -1.5371385, -0.4985314], [-0.9692660, 1.8760108, 0.0415560], [0.0556434, -0.2040259, 1.0572252]])
WN = np.array([0.95047, 1.0, 1.08883])
EPS, KAPPA = 216 / 24389, 24389 / 27


def lin_to_lab(rgb):
    t = (rgb @ M.T) / WN
    f = np.where(t > EPS, np.cbrt(np.maximum(t, 0)), (KAPPA * t + 16) / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]), 200 * (f[..., 1] - f[..., 2])], -1)


def lab_to_lin(lab):
    fy = (lab[..., 0] + 16) / 116
    f = np.stack([fy + lab[..., 1] / 500, fy, fy - lab[..., 2] / 200], -1)
    t = np.where(f ** 3 > EPS, f ** 3, (116 * f - 16) / KAPPA) * WN
    return t @ MINV.T


def hue(lab):
    return np.degrees(np.arctan2(lab[..., 2], lab[..., 1])) % 360


def signed_hue(lab):
    h = hue(lab)
    return np.where(h > 180, h - 360, h)


def chroma(lab):
    return np.hypot(lab[..., 1], lab[..., 2])


def polar_lab(L, h, C):
    return np.stack([L, C * np.cos(np.radians(h)), C * np.sin(np.radians(h))], -1)


def planck_rgb(T):
    """Linear-sRGB multiplier of a Planckian illuminant relative to D65 (Kim et al. 2002 xy approximation)."""
    T = np.asarray(T, float)
    x = np.where(T <= 4000,
                 -0.2661239e9 / T ** 3 - 0.2343589e6 / T ** 2 + 0.8776956e3 / T + 0.179910,
                 -3.0258469e9 / T ** 3 + 2.1070379e6 / T ** 2 + 0.2226347e3 / T + 0.240390)
    y = np.where(T <= 2222, -1.1063814 * x ** 3 - 1.34811020 * x ** 2 + 2.18555832 * x - 0.20219683,
                 np.where(T <= 4000, -0.9549476 * x ** 3 - 1.37418593 * x ** 2 + 2.09137015 * x - 0.16748867,
                          3.0817580 * x ** 3 - 5.87338670 * x ** 2 + 3.75112997 * x - 0.37001483))
    xyz = np.stack([x / y, np.ones_like(x), (1 - x - y) / y], -1)
    rgb = xyz @ MINV.T
    return rgb / rgb[..., 1:2]


# ---------------- synthetic faces ----------------

def draw_truth(rng, tones):
    """Per-face true colors (spectro-like Lab) for the given tone indices."""
    n = len(tones)
    mu = np.array([[m for m, _ in SPEC[TONES[t]]] for t in range(12)])
    sd = np.array([[s for _, s in SPEC[TONES[t]]] for t in range(12)])
    v = mu[tones] + rng.standard_normal((n, len(SPEC_COLS))) * sd[tones]
    c = {k: v[:, i] for i, k in enumerate(SPEC_COLS)}
    skin = np.stack([c["skinL"], c["skinA"], c["skinB"]], -1)
    hair = np.stack([c["hairL"], rng.normal(3.0, 1.5, n) + 0.3 * c["hairB"], c["hairB"]], -1)
    iris = np.stack([c["irisL"], rng.normal(6.0, 2.0, n), rng.normal(12.0, 3.0, n) + 0.3 * (c["skinB"] - 15.5)], -1)
    lips = polar_lab(c["lipL"], c["lipH"], c["lipC"])
    sclera = np.stack([rng.normal(82, 4, n), rng.normal(1.5, 1.2, n), rng.normal(6.0, 2.5, n)], -1)
    paper = np.stack([rng.normal(94, 1.5, n), rng.normal(0.5, 0.8, n), rng.normal(-3.0, 1.5, n)], -1)  # OBA paper: slightly blue
    return dict(skin=skin, hair=hair, iris=iris, lips=lips, sclera=sclera, paper=paper)


def apply_makeup(rng, f, p_found=0.45, p_lip=0.45, p_dye=0.3):
    n = len(f["skin"])
    found = rng.random(n) < p_found          # cushion/foundation pulls skin toward a standard 21-23호 shade
    shade = np.stack([rng.normal(65.5, 2.0, n), rng.normal(9.5, 0.8, n), rng.normal(16.5, 1.0, n)], -1)
    k = rng.uniform(0.2, 0.5, n)[:, None]
    f["skin"] = np.where(found[:, None], (1 - k) * f["skin"] + k * shade, f["skin"])
    lip = rng.random(n) < p_lip               # lipstick/tint: any hue, often saturated
    stick = polar_lab(rng.uniform(40, 56, n), rng.uniform(-5, 45, n), rng.uniform(25, 60, n))
    f["lips"] = np.where(lip[:, None], stick, f["lips"])
    dye = rng.random(n) < p_dye               # brown dye
    dyed = np.stack([rng.uniform(18, 45, n), rng.uniform(3, 10, n), rng.uniform(5, 22, n)], -1)
    f["hair"] = np.where(dye[:, None], dyed, f["hair"])
    return f


def photograph(rng, f, cct=None, tint=None, awb_residual=None, look=True):
    """Truth Lab → measured linear RGB per region (before our white balance)."""
    n = len(f["skin"])
    T = rng.uniform(3000, 7000, n) if cct is None else np.full(n, float(cct))
    ill = planck_rgb(T)
    tn = rng.uniform(-0.06, 0.06, n) if tint is None else np.full(n, float(tint))
    ill = ill * np.stack([np.ones(n), 1 + tn, np.ones(n)], -1)
    res = rng.uniform(0.0, 0.7, n) if awb_residual is None else np.full(n, float(awb_residual))
    cast = ill ** res[:, None]                 # camera AWB removes (1 − res) of the cast in log space
    skin_lin = lab_to_lin(f["skin"]) * cast
    y = skin_lin @ M[1]
    ae = (0.31 / np.maximum(y, 1e-3)) ** rng.uniform(0.2, 0.6, n) * np.exp(rng.normal(0, 0.15, n))  # face AE + error
    sat = rng.uniform(0.9, 1.3, n) if look else np.ones(n)   # phone "look": saturation boost
    out = {}
    for k in ["skin", "hair", "iris", "lips", "sclera", "paper"]:
        lin = lab_to_lin(f[k]) * cast * ae[:, None]
        if k == "sclera":
            lin = lin * rng.uniform(0.6, 1.0, n)[:, None]    # lid shadow
        if k == "paper":
            lin = lin * rng.uniform(0.7, 1.3, n)[:, None]    # paper angle vs face
        lab = lin_to_lab(np.clip(lin, 0, 1))
        lab[:, 1:] *= sat[:, None]
        out[k] = np.clip(lab_to_lin(lab), 0, 1)
    return out


def gains_from(m, target_ratio, gmin, gmax, reject):
    raw = target_ratio / (m / m[:, 1:2])
    ok = raw.max(1) / raw.min(1) <= reject
    return np.clip(raw, gmin, gmax), ok


def white_balance(meas, mode, rng):
    """Returns gains (n,3), source (0 paper, 1 sclera, 2 none), ok mask (False = STRONG_COLOR_CAST rejected)."""
    n = len(meas["skin"])
    g = np.ones((n, 3))
    src = np.full(n, 2)
    ok = np.ones(n, bool)
    if mode == "none":
        return g, src, ok
    sc_t = lab_to_lin(np.array([SCLERA_TARGET]))[0]
    sg, sok = gains_from(meas["sclera"], sc_t / sc_t[1], *SCLERA_GAIN)
    pp_t = lab_to_lin(np.array([PAPER_TARGET]))[0]
    pg, pok = gains_from(meas["paper"], pp_t / pp_t[1], *PAPER_GAIN)
    paper_usable = (meas["paper"].max(1) <= PAPER_CLIPPED) & (meas["paper"].min(1) >= PAPER_TOO_DARK)
    if mode == "mix":
        has_paper = rng.random(n) < 0.2
    else:
        has_paper = np.full(n, mode == "paper")
    use_paper = has_paper & paper_usable
    g = np.where(use_paper[:, None], pg, sg)
    src = np.where(use_paper, 0, 1)
    ok = np.where(use_paper, pok, sok)
    return g, src, ok


def measure(rng, f, meas, gains, p_hair_missing=0.25, p_iris_missing=0.1, p_lip_missing=0.05):
    """Balanced Lab per region + sampling noise + the analyzer's reliability rules → feature matrix."""
    n = len(f["skin"])
    lab = {k: lin_to_lab(meas[k] * gains) for k in ["skin", "hair", "iris", "lips"]}
    for k, s in [("skin", 0.7), ("hair", 2.0), ("iris", 2.5), ("lips", 1.5)]:
        lab[k] = lab[k] + rng.standard_normal((n, 3)) * s
    skin = lab["skin"]
    hair_ok = (rng.random(n) >= p_hair_missing) & (lab["hair"][:, 0] < skin[:, 0] - HAIR_MIN_DARKER_L) & \
        (np.linalg.norm(lab["hair"] - skin, axis=1) > HAIR_MIN_DE)
    iris_ok = (rng.random(n) >= p_iris_missing) & (lab["iris"][:, 0] < skin[:, 0] - IRIS_MIN_DARKER_L)
    lip_ok = (rng.random(n) >= p_lip_missing) & (chroma(lab["lips"]) <= LIP_MAKEUP_C)
    return features(skin, np.where(hair_ok[:, None], lab["hair"], np.nan),
                    np.where(iris_ok[:, None], lab["iris"], np.nan), np.where(lip_ok[:, None], lab["lips"], np.nan))


def features(skin, hair, iris, lips):
    """NaN rows = missing. Mirror of ToneModel.vector."""
    hh, hi, hl = ~np.isnan(hair[:, 0]), ~np.isnan(iris[:, 0]), ~np.isnan(lips[:, 0])
    darkest = np.fmin(np.where(hh, hair[:, 0], np.nan), np.where(hi, iris[:, 0], np.nan))
    contrast = np.where(np.isnan(darkest), FILL["contrast"], skin[:, 0] - darkest)
    return np.stack([
        skin[:, 0], skin[:, 1], skin[:, 2], hue(skin), chroma(skin),
        np.where(hh, hair[:, 0], FILL["hairL"]), np.where(hh, hair[:, 2], FILL["hairB"]), hh.astype(float),
        np.where(hi, iris[:, 0], FILL["irisL"]), hi.astype(float),
        contrast,
        np.where(hl, signed_hue(np.nan_to_num(lips)), FILL["lipH"]), np.where(hl, chroma(np.nan_to_num(lips)), FILL["lipC"]),
        hl.astype(float),
    ], -1)


def synth(rng, n, wb="mix", cct=None, tint=None, awb_residual=None, makeup=True, missing=True, look=True):
    tones = rng.choice(12, n, p=PRIOR)
    f = draw_truth(rng, tones)
    if makeup:
        f = apply_makeup(rng, f)
    meas = photograph(rng, f, cct, tint, awb_residual, look)
    g, src, ok = white_balance(meas, wb, rng)
    miss = dict() if missing else dict(p_hair_missing=0, p_iris_missing=0, p_lip_missing=0)
    X = measure(rng, f, meas, g, **miss)
    return X[ok], tones[ok], ok.mean()


# ---------------- model: tiny MLP (hidden=0 → multinomial logistic regression) ----------------

def softmax(z):
    z = z - z.max(1, keepdims=True)
    e = np.exp(z)
    return e / e.sum(1, keepdims=True)


def train_mlp(X, y, hidden, rng, epochs=40, lr=3e-3, l2=1e-4, batch=512, sample_weight=None):
    mean, sd = X.mean(0), X.std(0) + 1e-6
    Z = (X - mean) / sd
    sw = np.ones(len(y)) if sample_weight is None else sample_weight
    d = Z.shape[1]
    P = {}
    if hidden:
        P["W1"] = rng.standard_normal((d, hidden)) * math.sqrt(1 / d)
        P["b1"] = np.zeros(hidden)
        P["W2"] = rng.standard_normal((hidden, 12)) * math.sqrt(1 / hidden)
    else:
        P["W2"] = np.zeros((d, 12))
    P["b2"] = np.zeros(12)
    m = {k: np.zeros_like(v) for k, v in P.items()}
    v2 = {k: np.zeros_like(v) for k, v in P.items()}
    Y = np.eye(12)[y]
    t = 0
    for ep in range(epochs):
        idx = rng.permutation(len(y))
        for s in range(0, len(y), batch):
            b = idx[s:s + batch]
            h = np.tanh(Z[b] @ P["W1"] + P["b1"]) if hidden else Z[b]
            p = softmax(h @ P["W2"] + P["b2"])
            dz = (p - Y[b]) * sw[b, None] / sw[b].sum()
            G = {"W2": h.T @ dz + l2 * P["W2"], "b2": dz.sum(0)}
            if hidden:
                dh = (dz @ P["W2"].T) * (1 - h ** 2)
                G["W1"] = Z[b].T @ dh + l2 * P["W1"]
                G["b1"] = dh.sum(0)
            t += 1
            for k in P:
                m[k] = 0.9 * m[k] + 0.1 * G[k]
                v2[k] = 0.999 * v2[k] + 0.001 * G[k] ** 2
                P[k] -= lr * (m[k] / (1 - 0.9 ** t)) / (np.sqrt(v2[k] / (1 - 0.999 ** t)) + 1e-8)
        if ep == int(epochs * 0.7):
            lr *= 0.3
    P["mean"], P["sd"], P["T"] = mean, sd, 1.0
    return P


def logits(P, X):
    Z = (X - P["mean"]) / P["sd"]
    h = np.tanh(Z @ P["W1"] + P["b1"]) if "W1" in P else Z
    return h @ P["W2"] + P["b2"]


def predict(P, X):
    return softmax(logits(P, X) / P["T"])


def calibrate(P, X, y):
    """Temperature scaling on held-out data (grid search on NLL)."""
    L = logits(P, X)
    best = min(np.arange(0.5, 3.01, 0.05), key=lambda T: -np.log(softmax(L / T)[np.arange(len(y)), y] + 1e-12).mean())
    P["T"] = float(best)
    return P


# ---------------- metrics ----------------
SEASON_OF = np.array([SEASONS.index(t.split("_")[0]) for t in TONES])


def metrics(p, y):
    sp = np.stack([p[:, SEASON_OF == s].sum(1) for s in range(4)], -1)
    warm_p = sp[:, [0, 2]].sum(1)
    top2 = np.argsort(-p, 1)[:, :2]
    conf = p.max(1)
    bins = np.minimum((conf * 10).astype(int), 9)
    correct = p.argmax(1) == y
    ece = sum(abs(correct[bins == b].mean() - conf[bins == b].mean()) * (bins == b).mean() for b in range(10) if (bins == b).any())
    return {
        "season": (sp.argmax(1) == SEASON_OF[y]).mean(),
        "warm/cool": ((warm_p > 0.5) == np.isin(SEASON_OF[y], [0, 2])).mean(),
        "12 top1": correct.mean(),
        "12 top2": (top2 == y[:, None]).any(1).mean(),
        "ECE": ece,
    }


def fmt(m):
    return " | ".join(f"{100 * m[k]:5.1f}%" if k != "ECE" else f"{m[k]:.3f}" for k in ["season", "warm/cool", "12 top1", "12 top2", "ECE"])


# ---------------- real data (opt-in measurement vectors + expert labels) ----------------

def load_real(path):
    """CSV with columns tone + measurementVector keys (see README.md). Returns X, y."""
    X, y = [], []
    with open(path, newline="") as fh:
        for r in csv.DictReader(fh):
            if r.get("tone") not in TONES:
                continue
            g = lambda k: float(r[k]) if r.get(k) not in (None, "") else np.nan
            skin = np.array([[g("skin_l"), g("skin_a"), g("skin_b")]])
            hair = np.array([[g("hair_l"), g("hair_a"), g("hair_b")]])
            iris = np.array([[g("iris_l"), np.nan, np.nan]])
            lips = np.full((1, 3), np.nan)
            if not np.isnan(g("lip_h")) and g("lip_makeup") != 1.0:
                lips = polar_lab(np.array([g("lip_l")]), np.array([g("lip_h")]), np.array([g("lip_c")]))
            X.append(features(skin, hair, iris, lips)[0])
            y.append(TONES.index(r["tone"]))
    return np.array(X), np.array(y)


# ---------------- export ----------------

def kt_array(a):
    return "doubleArrayOf(" + ", ".join(f"{x:.7e}" for x in np.ravel(a)) + ")"


def export(P, X_parity, version):
    W1 = P.get("W1")
    hidden = 0 if W1 is None else W1.shape[1]
    lines = [
        "package com.gun.personalcolor.core.analysis",
        "",
        f"// GENERATED by tools/model/train.py ({version}) — do not edit by hand. Rerun the script instead.",
        "internal object ToneModelWeights {",
        f"    const val VERSION = \"{version}\"",
        f"    const val HIDDEN = {hidden}",
        f"    const val TEMPERATURE = {P['T']:.4f}",
        f"    /** Feature order: {', '.join(FEATURES)} */",
        f"    val MEAN = {kt_array(P['mean'])}",
        f"    val SD = {kt_array(P['sd'])}",
    ]
    if hidden:
        lines += [f"    /** [input][hidden], row-major. */", f"    val W1 = {kt_array(W1)}", f"    val B1 = {kt_array(P['b1'])}"]
    else:
        lines += ["    val W1 = DoubleArray(0)", "    val B1 = DoubleArray(0)"]
    lines += [
        f"    /** [{'hidden' if hidden else 'input'}][tone], row-major. */",
        f"    val W2 = {kt_array(P['W2'])}",
        f"    val B2 = {kt_array(P['b2'])}",
        "    // Parity check for the unit test: raw feature vectors and the probabilities Python computed for them.",
        f"    val PARITY_X = {kt_array(X_parity)}",
        f"    val PARITY_P = {kt_array(predict(P, X_parity))}",
        "}",
        "",
    ]
    with open(OUT_KT, "w") as fh:
        fh.write("\n".join(lines))
    print(f"wrote {os.path.relpath(OUT_KT)}")


# ---------------- rule-based baseline (old ToneClassifier on ruleAxes) ----------------
PROTOS = np.array([[0.5, 0.9, 0.3], [1.0, 0.3, 0.5], [0.5, 0.2, 1.0], [-0.5, 0.9, 0.0], [-1.0, 0.3, -0.2], [-0.4, 0.0, -0.9],
                   [0.4, 0.0, -0.9], [1.0, -0.4, -0.3], [0.5, -0.9, 0.0], [-0.5, 0.0, 1.0], [-1.0, -0.2, 0.5], [-0.5, -0.9, 0.3]])


def rule_probs(X):
    """Port of PersonalColorAnalyzer.ruleAxes + ToneClassifier.classify (keep constants in sync)."""
    c = {k: X[:, i] for i, k in enumerate(FEATURES)}
    t = lambda x, m, s: np.clip((x - m) / s, -1, 1)
    hue_t, ba_t = t(c["skinH"], 56, 5), t(c["skinB"] - c["skinA"], 5, 3)
    warm = np.where(c["hasLip"] > 0, 0.6 * hue_t + 0.25 * ba_t + 0.15 * t(c["lipH"], 33, 10), (0.6 * hue_t + 0.25 * ba_t) / 0.85)
    has_f = (c["hasHair"] + c["hasIris"]) > 0
    feat_l = c["skinL"] - c["contrast"]
    val = np.where(has_f, 0.6 * t(c["skinL"], 64.5, 3.5) + 0.4 * t(feat_l, 16, 6), t(c["skinL"], 64.5, 3.5))
    clar = np.where(has_f, 0.5 * t(c["skinC"], 18.5, 2) + 0.5 * t(c["contrast"], 47, 5), t(c["skinC"], 18.5, 2))
    A = np.stack([np.clip(warm, -1, 1), np.clip(val, -1, 1), np.clip(clar, -1, 1)], -1)
    d2 = ((A[:, None, :] - PROTOS[None]) ** 2).sum(-1)
    return softmax(-d2 / 0.6)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--real", help="CSV of real labeled measurement vectors")
    ap.add_argument("--real-weight", type=float, default=5.0, help="sample weight of each real row vs a synthetic one")
    ap.add_argument("--n", type=int, default=240_000)
    ap.add_argument("--hidden", type=int, default=24)
    ap.add_argument("--no-export", action="store_true")
    a = ap.parse_args()

    rng = np.random.default_rng(42)
    X, y, acc_rate = synth(rng, a.n)
    Xv, yv, _ = synth(np.random.default_rng(7), 30_000)
    w = np.ones(len(y))
    if a.real:
        Xr, yr = load_real(a.real)
        print(f"real rows: {len(yr)}")
        # ponytail: real rows are also used for validation-free training; hold some out yourself when you have >500.
        X, y, w = np.vstack([X, Xr]), np.concatenate([y, yr]), np.concatenate([w, np.full(len(yr), a.real_weight)])
    print(f"train {len(y)} (kept {100 * acc_rate:.1f}% after cast rejection)")

    lr_model = calibrate(train_mlp(X, y, 0, np.random.default_rng(1), sample_weight=w), Xv, yv)
    mlp = calibrate(train_mlp(X, y, a.hidden, np.random.default_rng(1), sample_weight=w), Xv, yv)

    trng = lambda: np.random.default_rng(2024)   # same test faces for every model/condition
    Xt, yt, _ = synth(trng(), 40_000)
    print("\nHeld-out synthetic (realistic mix: random illuminant, makeup, dye, missing features)")
    print(f"{'model':28s}  season | warm/cool | 12 top1 | 12 top2 | ECE")
    print(f"{'rule axes + prototypes':28s}  {fmt(metrics(rule_probs(Xt), yt))}")
    print(f"{'logistic regression':28s}  {fmt(metrics(predict(lr_model, Xt), yt))}")
    print(f"{f'MLP {a.hidden} tanh':28s}  {fmt(metrics(predict(mlp, Xt), yt))}")
    pt = predict(mlp, Xt)
    sp = np.stack([pt[:, SEASON_OF == k].sum(1) for k in range(4)], -1)
    conf = sp.max(1)    # = Success.confidence before the quality discount
    print("\nSelective accuracy, MLP (answer only the most confident X% — the rest → draping check / retake)")
    for cov in (1.0, 0.7, 0.5, 0.3):
        keep = conf >= np.quantile(conf, 1 - cov)
        m = metrics(pt[keep], yt[keep])
        print(f"  coverage {100 * cov:3.0f}% (confidence ≥ {np.quantile(conf, 1 - cov):.2f}): season {100 * m['season']:.1f}%, "
              f"warm/cool {100 * m['warm/cool']:.1f}%, 12 top2 {100 * m['12 top2']:.1f}%")
    Xc, yc, _ = synth(trng(), 40_000, cct=6500, tint=0, awb_residual=0, makeup=False, missing=False, look=False)
    print(f"{'rule axes, ideal':28s}  {fmt(metrics(rule_probs(Xc), yc))}")
    print(f"{'MLP, ideal (D65, bare face)':28s}  {fmt(metrics(predict(mlp, Xc), yc))}")

    print("\nIlluminant robustness, MLP (camera AWB leaves 30% / 100% of the cast; no makeup)")
    print(f"{'CCT':>6s} {'residual':>8s}  {'WB':6s}  season | warm/cool | 12 top1 | 12 top2 | ECE | kept")
    for res in (0.3, 1.0):
        for cct in (3000, 4000, 5000, 6500, 7000):
            for wb in ("none", "sclera", "paper"):
                Xi, yi, kept = synth(trng(), 12_000, wb=wb, cct=cct, tint=0, awb_residual=res, makeup=False)
                print(f"{cct:6d} {res:8.1f}  {wb:6s}  {fmt(metrics(predict(mlp, Xi), yi))} | {100 * kept:5.1f}%")

    if not a.no_export:
        version = f"synthetic-v1{'+real' if a.real else ''}"
        export(mlp, Xt[:3], version)


if __name__ == "__main__":
    sys.exit(main())
