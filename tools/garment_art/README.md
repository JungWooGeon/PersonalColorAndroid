# Garment art

Source of the painted garment illustrations (`core/ui/src/main/assets/garments`).

- `garments/<kind>.py`: one SVG per kind. The color appears only as the base fill; shading is black/white at an alpha.
- `ART_SPEC.md`: style and technical rules. `KINDS.md`: the kind list.
- Export (needs Google Chrome, numpy, Pillow): `python3 export.py ../../core/ui/src/main/assets/garments [kind ...]`
