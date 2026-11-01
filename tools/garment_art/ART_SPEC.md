# Garment art v6 — shared spec for all garment artists

App: "나의 컬러는?" (personal color app). **Target: women in their late teens to mid-20s, in Korea.** The owner rejected the old art
as "아재패션" (middle-aged men's fashion) and "밤티" (tacky). Every piece must look like something a 20-year-old Korean woman would
buy on 무신사 우먼 / 29CM / 지그재그 / 에이블리: current silhouettes (cropped, oversized, A-line, wide, etc.), pretty details.

## Style (match the reference exactly)
- Reference image: `kit/reference_sheet.png`. Reference code: `kit/art_kit.py`. That file has helpers (`piece`, `smooth`, `sym`,
  `mir`, `K`, `Wt`, `stitch`, `line`, `button`, `pearl`, `raised`, `shoe_svg`, `pair`, `render`) and 14 finished examples
  (short_coat, boucle_jacket, cropped_cardigan, turtleneck, oversized_knit, wide_denim, pleated_mini, long_skirt, loafers,
  mary_janes, long_boots, rain_boots, shoulder_bag, umbrella). Read 2–3 examples that are closest to your garments first.
- Soft fashion flat-lay illustration that reads almost like a cut-out product photo. **No thick cartoon outlines.** Use real
  silhouettes, gentle volume, and fabric texture: knit ribs, denim twill and seams, wool grain, boucle loops, leather sheen,
  satin gloss.
- Garments are shown flat and front-facing. Shoes are a side-view pair, like the examples. Bags are front view.

## Hard technical contract (the app recolors every piece at runtime)
- One function per kind: `def <key>(color: str) -> str` returning an SVG built with `piece(...)` (or `shoe_svg` for shoes),
  in `garments/<key>.py`, starting with `from kit.art_kit import *`.
- The **only** place the garment color appears is the `base` fill (the `color` argument). All shading, texture and highlights
  must be **pure black or pure white at an alpha** (layers shade / light / texture / form / fabric). Never derive a darker/lighter
  hex of `color` (the runtime can't do that).
- Fixed-colour hardware/details (gold buttons, pearl buttons, buckles, soles, laces, zips, lenses) go in `detail` with literal
  colours. Keep them neutral (black, gold, silver, pearl, white, tan).
- `under` (lining or inside of a collar or hood) must also be black or white at an alpha drawn over the base, never another
  colour. Read how short_coat does its inside neckline.
- **Scale:** keep real-world proportions consistent with the references. Use the same units as short_coat (400×430) for
  jackets and tops, wide_denim for trousers, pleated_mini for skirts, loafers for shoes and shoulder_bag for bags. A
  crop tee is visibly smaller than a coat; earrings are small.
- Don't edit `kit/art_kit.py`. If you need a helper, define it in your own file.

## Check before you hand in (mandatory)
- Render each of your kinds in 4 colours: `#141414` black, `#F3EEE6` ivory, `#5E1224` wine, `#9DB4D6` light blue. Use the
  `render` approach from art_kit (headless Chrome) and put the PNGs in `sheets/<your-id>/`.
- Make one contact sheet `sheets/<your-id>/sheet.png` with your pieces next to 3 of the reference pieces.
- **Critique it like the target customer would:** Is it pretty? Is it trendy for a 20-year-old woman? Does the silhouette
  read at 60dp? Does black still show form and does ivory still show shade? Is it consistent with the reference? Redo
  anything that looks like clip art, menswear or something a middle-aged person would wear.
- Report: your file list, one line per piece, and any weak spots.

## Don'ts
- No repo edits, no app builds, no phone or emulator. You only write `garments/<key>.py` and `sheets/<your-id>/`.
