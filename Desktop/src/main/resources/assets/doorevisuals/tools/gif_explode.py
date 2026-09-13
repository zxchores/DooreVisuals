#!/usr/bin/env python3
"""Explode a GIF into PNG frames with per-frame delays (Pillow)."""
from __future__ import annotations

import sys
from pathlib import Path

try:
	from PIL import Image, ImageSequence
except ImportError:
	sys.exit(2)


def explode(src: Path, out: Path) -> None:
	out.mkdir(parents=True, exist_ok=True)
	for old in out.glob("frame_*.png"):
		old.unlink(missing_ok=True)
	im = Image.open(src)
	canvas_w, canvas_h = im.size
	canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
	backup = None
	delays: list[int] = []
	n = getattr(im, "n_frames", 1)
	written = 0
	for i in range(n):
		im.seek(i)
		duration = int(im.info.get("duration") or 80)
		if duration <= 10:
			duration = 100
		duration = max(20, min(2000, duration))
		disposal = int(im.info.get("disposal") or 0)
		left = int(im.info.get("left") or 0)
		top = int(im.info.get("top") or 0)
		frame = im.convert("RGBA")
		if i > 0 and disposal == 2:
			# restore to background — clear previous frame rect if we know it
			canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
		elif i > 0 and disposal == 3 and backup is not None:
			canvas = backup.copy()
		if disposal == 3:
			backup = canvas.copy()
		else:
			backup = None
		canvas.paste(frame, (left, top), frame)
		canvas.copy().save(out / f"frame_{written:03d}.png")
		delays.append(duration)
		written += 1
		if written >= 120:
			break
	if written == 0:
		raise SystemExit("empty gif")
	(out / "delay.txt").write_text(",".join(str(d) for d in delays), encoding="utf-8")


if __name__ == "__main__":
	if len(sys.argv) < 3:
		sys.exit(1)
	explode(Path(sys.argv[1]), Path(sys.argv[2]))
