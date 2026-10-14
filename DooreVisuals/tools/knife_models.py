"""Генератор моделей клинков для Custom Sword.

Каждый нож собирается из кубов в вертикальной позе (рукоять внизу, остриё вверху, центр по x = 8, z = 8),
а при записи каждый элемент поворачивается на -45 градусов вокруг (8, 8, 8): так предмет в руке стоит по
диагонали, как обычный меч. Допустимый для ванильных моделей угол 45 используется один и тот же, поэтому
поворот не зависит от версии игры. Изгибы и волны набираются тонкими срезами, а не углами.

Для каждого ножа рисуется своя текстура 64x64: сетка 4x4 плиток по 16x16 пикселей (сталь с фаской и долом,
кромка, обух, обмотка, дерево, металл, золото, руны). UV в моделях заданы в системе 0..16, поэтому плитка (i, j)
занимает прямоугольник [4i, 4j, 4i + 4, 4j + 4].

Запуск:  python tools/knife_models.py            записать модели, предметы и текстуры
         python tools/knife_models.py --preview  дополнительно сохранить превью в tools/preview/
"""

import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "doorevisuals")
PREVIEW = os.path.join(ROOT, "tools", "preview")
TILE = 16

# ванильные display-блоки для предмета в руке (те же значения, что у готовых ножей)
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "gui": {"rotation": [20, -40, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def clamp(v):
    return max(0, min(255, int(v)))


def shade(rgb, k):
    return tuple(clamp(c * k) for c in rgb)


def mix(a, b, t):
    return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))


# ------------------------------------------------------------------------------------------ плитки


def tile_steel(base, hi, lo, seed=1):
    """Клинок: сверху (остриё) светлее, внизу темнее; левая половина фаски светлее правой, по краям кромка."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE))
    for y in range(TILE):
        g = mix(hi, lo, y / (TILE - 1) * 0.8)
        for x in range(TILE):
            c = mix(base, g, 0.55)
            c = shade(c, 1.10 if x < 8 else 0.88)
            if x in (7, 8):
                c = shade(c, 0.78)
            if x in (0, 15):
                c = mix(c, (245, 248, 252), 0.55)
            n = 1.0 + (rnd.random() - 0.5) * 0.07
            img.putpixel((x, y), shade(c, n) + (255,))
    return img


def tile_flat(base, hi=None, noise=0.05, seed=2, vertical=0.0):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE))
    for y in range(TILE):
        for x in range(TILE):
            c = base if hi is None else mix(hi, base, y / (TILE - 1) * vertical + (1 - vertical) * 0.5)
            img.putpixel((x, y), shade(c, 1.0 + (rnd.random() - 0.5) * noise) + (255,))
    return img


def tile_ridged(base, dark, step=3, seed=3):
    """Рифлёная рукоять: горизонтальные канавки."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE))
    for y in range(TILE):
        groove = (y % step) == 0
        for x in range(TILE):
            c = dark if groove else base
            c = shade(c, 1.0 + (rnd.random() - 0.5) * 0.08 + (0.06 if x < 4 else (-0.06 if x > 11 else 0)))
            img.putpixel((x, y), c + (255,))
    return img


def tile_wrap(base, alt):
    """Обмотка: диагональные полосы двух цветов."""
    img = Image.new("RGBA", (TILE, TILE))
    for y in range(TILE):
        for x in range(TILE):
            c = base if ((x + y) // 2) % 2 == 0 else alt
            img.putpixel((x, y), shade(c, 1.07 if x < 4 else (0.9 if x > 11 else 1.0)) + (255,))
    return img


def tile_wood(base, dark, seed=4):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE))
    for x in range(TILE):
        line = mix(base, dark, 0.5 + 0.5 * math.sin(x * 1.3 + rnd.random()))
        for y in range(TILE):
            img.putpixel((x, y), shade(line, 1.0 + (rnd.random() - 0.5) * 0.06) + (255,))
    return img


def tile_metal(base, seed=5):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE))
    for y in range(TILE):
        for x in range(TILE):
            k = 1.18 - 0.35 * (y / (TILE - 1)) + (rnd.random() - 0.5) * 0.06
            img.putpixel((x, y), shade(base, k) + (255,))
    return img


def tile_glow(core, rim):
    """Светящаяся деталь: яркий центр, цветной край."""
    img = Image.new("RGBA", (TILE, TILE))
    for y in range(TILE):
        for x in range(TILE):
            d = math.hypot(x - 7.5, y - 7.5) / 8.5
            img.putpixel((x, y), mix(core, rim, min(1.0, d)) + (255,))
    return img


def tile_runes(base, glow, seed=6):
    """Клинок с вырезанными рунами: тёмная сталь и яркие символы по центру."""
    rnd = random.Random(seed)
    img = tile_steel(base, shade(base, 1.35), shade(base, 0.7), seed)
    for k in range(3):
        cy = 2 + k * 5
        for dx in range(-2, 3):
            for dy in range(0, 3):
                if rnd.random() < 0.55:
                    img.putpixel((8 + dx, min(TILE - 1, cy + dy)), glow + (255,))
    return img


TILES = ["steel", "edge", "spine", "fuller", "grip", "gripside", "guard", "guarddark", "accent", "accent2", "glow", "gem", "extra", "extra2", "extra3", "extra4"]


class Atlas:
    """Сетка 4x4 плиток 16x16; имена плиток привязываются к ячейкам по порядку TILES."""

    def __init__(self):
        self.img = Image.new("RGBA", (TILE * 4, TILE * 4), (0, 0, 0, 0))
        self.slot = {}

    def put(self, name, tile):
        i = len(self.slot)
        self.slot[name] = i
        self.img.paste(tile, ((i % 4) * TILE, (i // 4) * TILE))

    def uv(self, name, vfrac=None, flip=False):
        i = self.slot[name]
        x0, y0 = (i % 4) * 4.0, (i // 4) * 4.0
        v0, v1 = (0.0, 4.0) if vfrac is None else (vfrac[0] * 4.0, vfrac[1] * 4.0)
        return [x0, round(y0 + v0, 3), x0 + 4.0, round(y0 + v1, 3)]


# ------------------------------------------------------------------------------------------ геометрия


class Knife:
    def __init__(self, slug, title, about):
        self.slug = slug
        self.title = title
        self.about = about
        self.atlas = Atlas()
        self.elements = []

    def box(self, x0, y0, z0, x1, y1, z1, main, side=None, vfrac=None, emissive=False):
        """Куб в вертикальной позе. main — плитка широких граней (z), side — остальных."""
        side = side or main
        if x1 - x0 < 0.04:
            x1 = x0 + 0.04
        if y1 - y0 < 0.04:
            y1 = y0 + 0.04
        if z1 - z0 < 0.04:
            z1 = z0 + 0.04
        faces = {}
        for face in ("north", "south"):
            faces[face] = {"uv": self.atlas.uv(main, vfrac), "texture": "#0"}
        for face in ("east", "west", "up", "down"):
            faces[face] = {"uv": self.atlas.uv(side, vfrac), "texture": "#0"}
        if emissive:
            for f in faces.values():
                f["light_emission"] = 15
        self.elements.append({
            "from": [round(x0, 3), round(y0, 3), round(z0, 3)],
            "to": [round(x1, 3), round(y1, 3), round(z1, 3)],
            "faces": faces,
        })

    def slices(self, y0, y1, width, center, thick, main, side=None, steps=None, z=8.0, emissive=False):
        """Клинок срезами: width(s) и center(s), s от 0 (у основания) до 1 (остриё)."""
        steps = steps or max(4, int((y1 - y0) / 0.5))
        for i in range(steps):
            s0, s1 = i / steps, (i + 1) / steps
            sm = (s0 + s1) / 2.0
            w = max(0.12, width(sm))
            c = center(sm)
            self.box(c - w / 2.0, y0 + (y1 - y0) * s0 - 0.04, z - thick / 2.0, c + w / 2.0, y0 + (y1 - y0) * s1 + 0.04, z + thick / 2.0,
                     main, side or "spine", vfrac=(1.0 - s1, 1.0 - s0), emissive=emissive)

    def finish(self):
        out = []
        for e in self.elements:
            e = dict(e)
            e["rotation"] = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}
            out.append(e)
        return {
            "credit": "DooreVisuals · " + self.title,
            "textures": {"0": "doorevisuals:item/kn_" + self.slug, "particle": "doorevisuals:item/kn_" + self.slug},
            "display": DISPLAY,
            "elements": out,
        }


def ring(k, cx, cy, r, thick, tile, count=10, z=8.0, depth=1.4):
    """Кольцо из коротких коробок, плоское (в плоскости x, y)."""
    for i in range(count):
        a = 2 * math.pi * i / count
        px, py = cx + math.cos(a) * r, cy + math.sin(a) * r
        k.box(px - thick / 2.0, py - thick / 2.0, z - depth / 2.0, px + thick / 2.0, py + thick / 2.0, z + depth / 2.0, tile)


# ------------------------------------------------------------------------------------------ ножи


def knife_m9():
    k = Knife("m9", "М9", "Прямой клинок с долом и пилой по обуху")
    a = k.atlas
    a.put("steel", tile_steel((188, 198, 212), (232, 238, 246), (120, 130, 146)))
    a.put("edge", tile_flat((246, 249, 253), (255, 255, 255), 0.03))
    a.put("spine", tile_metal((98, 106, 120)))
    a.put("fuller", tile_flat((92, 100, 114), (130, 140, 154), 0.05, vertical=1.0))
    a.put("grip", tile_ridged((34, 38, 46), (18, 20, 25)))
    a.put("gripside", tile_flat((26, 29, 36)))
    a.put("guard", tile_metal((176, 184, 196)))
    a.put("guarddark", tile_metal((72, 78, 90)))
    k.box(7.1, 0.0, 7.1, 8.9, 0.7, 8.9, "guard", "guarddark")
    k.box(7.0, 0.7, 7.0, 9.0, 4.7, 9.0, "grip", "gripside")
    for y in (1.2, 2.2, 3.2):
        k.box(6.92, y, 6.92, 9.08, y + 0.3, 9.08, "guarddark")
    k.box(5.5, 4.7, 7.05, 10.5, 5.4, 8.95, "guard", "guarddark")
    ring(k, 4.7, 5.05, 0.9, 0.5, "guard", count=8)
    k.box(7.3, 5.4, 7.2, 8.7, 5.9, 8.8, "guarddark")

    def width(s):
        return 2.9 if s < 0.74 else max(0.3, 2.9 * (1 - (s - 0.74) / 0.26) ** 0.85 + 0.2)

    def center(s):
        return 8.0 if s < 0.74 else 8.0 + 1.0 * ((s - 0.74) / 0.26)

    k.slices(5.9, 15.6, width, center, 1.0, "steel", "spine", steps=24)
    for i in range(5):
        k.box(9.45, 6.8 + i * 0.95, 7.55, 10.0, 7.15 + i * 0.95, 8.45, "edge", "spine")
    k.box(7.78, 6.5, 8.5, 8.22, 12.6, 8.58, "fuller")
    k.box(7.78, 6.5, 7.42, 8.22, 12.6, 7.5, "fuller")
    k.box(6.55, 6.2, 7.7, 6.9, 12.8, 8.3, "edge")
    return k


def knife_karambit():
    k = Knife("karambit", "Карамбит", "Изогнутый серп с кольцом на рукояти")
    a = k.atlas
    a.put("steel", tile_steel((180, 192, 208), (238, 243, 250), (110, 122, 140)))
    a.put("edge", tile_flat((248, 250, 254), (255, 255, 255), 0.03))
    a.put("spine", tile_metal((92, 100, 114)))
    a.put("fuller", tile_flat((84, 92, 106), (124, 134, 148), 0.05, vertical=1.0))
    a.put("grip", tile_wrap((38, 44, 54), (22, 26, 34)))
    a.put("gripside", tile_flat((26, 30, 38)))
    a.put("guard", tile_metal((150, 160, 174)))
    a.put("guarddark", tile_metal((60, 66, 78)))
    a.put("accent", tile_flat((214, 60, 70), (255, 120, 120), 0.05, vertical=1.0))
    ring(k, 7.4, -0.9, 1.9, 0.75, "guard", count=14, depth=1.6)
    ring(k, 7.4, -0.9, 1.15, 0.45, "guarddark", count=10, depth=1.2)
    k.box(6.7, 0.9, 7.1, 8.5, 4.8, 8.9, "grip", "gripside")
    for y in (1.4, 2.5, 3.6):
        k.box(6.62, y, 7.02, 8.58, y + 0.3, 8.98, "guarddark")
    k.box(6.4, 4.8, 7.2, 8.8, 5.4, 8.8, "guard", "guarddark")

    length = 9.6

    def width(s):
        return 2.3 * (1 - s ** 1.7) + 0.3

    def center(s):
        d = s * length
        return 7.6 - 0.058 * d * d * 0.85

    k.slices(5.4, 5.4 + length, width, center, 0.95, "steel", "spine", steps=26)
    # красная вставка у основания и кромка по внутренней дуге
    k.box(7.1, 5.5, 8.46, 8.1, 6.3, 8.54, "accent")
    k.box(7.1, 5.5, 7.46, 8.1, 6.3, 7.54, "accent")
    for i in range(10):
        s = i / 10.0 * 0.85
        c = center(s + 0.04)
        k.box(c - width(s) / 2.0 - 0.28, 5.4 + s * length, 7.7, c - width(s) / 2.0 + 0.1, 5.4 + (s + 0.1) * length, 8.3, "edge")
    return k


def knife_butterfly():
    k = Knife("butterfly", "Бабочка", "Клинок-лепесток и две рукояти на шарнире")
    a = k.atlas
    a.put("steel", tile_steel((200, 208, 220), (240, 244, 250), (128, 138, 154)))
    a.put("edge", tile_flat((248, 250, 254), (255, 255, 255), 0.03))
    a.put("spine", tile_metal((104, 112, 126)))
    a.put("fuller", tile_flat((96, 104, 118), (132, 142, 156), 0.05, vertical=1.0))
    a.put("grip", tile_flat((30, 34, 42), (50, 56, 68), 0.06, vertical=1.0))
    a.put("gripside", tile_flat((22, 25, 32)))
    a.put("guard", tile_metal((178, 150, 82)))
    a.put("guarddark", tile_metal((70, 76, 88)))
    a.put("accent", tile_flat((226, 190, 96), (255, 235, 160), 0.06, vertical=1.0))
    # две половины рукояти по бокам клинка
    for sx in (-1, 1):
        x0 = 8 + sx * 0.55
        k.box(min(x0, x0 + sx * 1.5), 0.0, 7.2, max(x0, x0 + sx * 1.5), 6.6, 8.8, "grip", "gripside")
        k.box(min(x0, x0 + sx * 1.5) + (0.2 if sx > 0 else -0.2), 0.6, 8.8, max(x0, x0 + sx * 1.5) - (0.2 if sx > 0 else -0.2), 5.9, 8.95, "guarddark")
    k.box(6.1, 6.6, 7.2, 9.9, 7.3, 8.8, "guarddark")
    for y in (1.0, 6.2):
        k.box(7.35, y, 6.95, 8.65, y + 0.9, 9.05, "accent")
    # клинок: широкая спинка, короткое остриё
    def width(s):
        return 2.6 if s < 0.62 else max(0.3, 2.6 * (1 - (s - 0.62) / 0.38) ** 0.8 + 0.2)

    def center(s):
        return 8.0 + (0.9 * ((s - 0.62) / 0.38) if s > 0.62 else 0.0)

    k.slices(7.3, 15.7, width, center, 0.9, "steel", "spine", steps=20)
    k.box(7.85, 7.6, 8.43, 8.15, 12.4, 8.5, "fuller")
    k.box(7.85, 7.6, 7.5, 8.15, 12.4, 7.57, "fuller")
    k.box(6.65, 7.5, 7.7, 6.95, 13.0, 8.3, "edge")
    for i in range(3):
        k.box(9.2, 7.9 + i * 1.3, 7.7, 9.55, 8.3 + i * 1.3, 8.3, "guarddark")
    return k


def knife_bayonet():
    k = Knife("bayonet", "Штык-нож", "Узкий прямой клинок с кольцом под ствол")
    a = k.atlas
    a.put("steel", tile_steel((176, 186, 200), (228, 234, 242), (104, 114, 130)))
    a.put("edge", tile_flat((248, 250, 254), (255, 255, 255), 0.03))
    a.put("spine", tile_metal((86, 94, 108)))
    a.put("fuller", tile_flat((78, 86, 100), (116, 126, 140), 0.05, vertical=1.0))
    a.put("grip", tile_ridged((24, 28, 34), (12, 14, 18), step=2))
    a.put("gripside", tile_flat((20, 23, 29)))
    a.put("guard", tile_metal((112, 120, 134)))
    a.put("guarddark", tile_metal((58, 64, 76)))
    k.box(7.0, -0.2, 7.0, 9.0, 0.5, 9.0, "guarddark")
    k.box(7.1, 0.5, 7.1, 8.9, 4.2, 8.9, "grip", "gripside")
    for y in (0.9, 1.7, 2.5, 3.3):
        k.box(7.02, y, 7.02, 8.98, y + 0.25, 8.98, "guarddark")
    k.box(5.9, 4.2, 7.2, 10.1, 4.9, 8.8, "guard", "guarddark")
    ring(k, 5.3, 4.85, 1.15, 0.55, "guard", count=10, depth=1.5)
    k.box(7.4, 4.9, 7.3, 8.6, 5.5, 8.7, "guarddark")

    def width(s):
        return 2.3 if s < 0.82 else max(0.3, 2.3 * (1 - (s - 0.82) / 0.18) + 0.25)

    k.slices(5.5, 16.0, width, lambda s: 8.0, 1.0, "steel", "spine", steps=22)
    k.box(7.8, 6.0, 8.5, 8.2, 14.0, 8.58, "fuller")
    k.box(7.8, 6.0, 7.42, 8.2, 14.0, 7.5, "fuller")
    k.box(6.95, 6.0, 7.7, 7.25, 15.0, 8.3, "edge")
    k.box(8.8, 6.0, 7.6, 9.2, 14.2, 8.4, "spine")
    return k


def knife_hunter():
    k = Knife("hunter", "Охотничий", "Широкий клинок с крюком для свежевания")
    a = k.atlas
    a.put("steel", tile_steel((184, 194, 206), (232, 238, 244), (112, 122, 136)))
    a.put("edge", tile_flat((248, 250, 254), (255, 255, 255), 0.03))
    a.put("spine", tile_metal((94, 102, 114)))
    a.put("fuller", tile_flat((86, 94, 108), (126, 136, 150), 0.05, vertical=1.0))
    a.put("grip", tile_wood((124, 78, 42), (78, 46, 24)))
    a.put("gripside", tile_wood((96, 60, 32), (60, 36, 18), seed=9))
    a.put("guard", tile_metal((196, 156, 78)))
    a.put("guarddark", tile_flat((52, 34, 22), noise=0.08))
    k.box(7.0, -0.1, 7.0, 9.0, 0.6, 9.0, "guarddark")
    k.box(6.9, 0.6, 7.0, 9.1, 4.8, 9.0, "grip", "gripside")
    for y in (1.3, 2.6, 3.9):
        k.box(6.84, y, 7.8, 9.16, y + 0.4, 8.2, "guard")
    k.box(6.2, 4.8, 7.15, 9.8, 5.5, 8.85, "guard", "guarddark")

    def width(s):
        # прямой обух и дугой идущее брюшко: остриё сходится к обуху
        return 3.5 * (1 - s ** 2.3) + 0.35

    def center(s):
        return 9.5 - width(s) / 2.0

    k.slices(5.5, 15.4, width, center, 1.1, "steel", "spine", steps=22)
    k.box(7.7, 6.0, 8.55, 8.3, 11.0, 8.63, "fuller")
    k.box(7.7, 6.0, 7.37, 8.3, 11.0, 7.45, "fuller")
    k.box(6.1, 6.2, 7.75, 6.45, 11.6, 8.25, "edge")
    # крюк на обухе
    k.box(9.7, 9.0, 7.8, 10.5, 9.6, 8.2, "spine")
    k.box(10.1, 8.4, 7.8, 10.5, 9.0, 8.2, "spine")
    return k


def knife_arcanum():
    k = Knife("arcanum", "Арканум", "Тонкий клинок с долами и светящимися рунами")
    a = k.atlas
    a.put("steel", tile_runes((84, 92, 150), (150, 120, 255)))
    a.put("edge", tile_flat((226, 220, 255), (255, 255, 255), 0.03))
    a.put("spine", tile_metal((60, 56, 110)))
    a.put("fuller", tile_flat((58, 54, 110), (110, 90, 220), 0.05, vertical=1.0))
    a.put("grip", tile_wrap((52, 40, 100), (34, 26, 70)))
    a.put("gripside", tile_flat((30, 24, 62)))
    a.put("guard", tile_metal((116, 96, 210)))
    a.put("guarddark", tile_metal((58, 46, 120)))
    a.put("glow", tile_glow((235, 255, 255), (60, 220, 240)))
    a.put("gem", tile_glow((255, 255, 255), (130, 90, 255)))
    k.box(7.1, 0.0, 7.1, 8.9, 0.8, 8.9, "guard", "guarddark")
    k.box(7.5, -1.4, 7.5, 8.5, 0.0, 8.5, "glow", emissive=True)
    k.box(7.2, 0.8, 7.2, 8.8, 4.4, 8.8, "grip", "gripside")
    k.box(5.8, 4.4, 7.2, 10.2, 5.1, 8.8, "guard", "guarddark")
    k.box(6.1, 5.0, 7.3, 6.9, 6.0, 8.7, "guarddark")
    k.box(9.1, 5.0, 7.3, 9.9, 6.0, 8.7, "guarddark")
    k.box(7.2, 4.45, 7.0, 8.8, 5.45, 7.12, "gem", emissive=True)
    k.box(7.2, 4.45, 8.88, 8.8, 5.45, 9.0, "gem", emissive=True)

    def width(s):
        return 2.4 * (1 - 0.35 * s) if s < 0.86 else max(0.3, 1.6 * (1 - (s - 0.86) / 0.14) + 0.2)

    k.slices(5.1, 16.4, width, lambda s: 8.0, 0.9, "steel", "spine", steps=24)
    k.box(7.7, 6.0, 8.44, 8.3, 14.2, 8.5, "glow", emissive=True)
    k.box(7.7, 6.0, 7.5, 8.3, 14.2, 7.56, "glow", emissive=True)
    k.box(6.95, 6.0, 7.7, 7.25, 15.4, 8.3, "edge")
    return k


def knife_flame():
    k = Knife("flame", "Пламенный", "Волнистая кромка и огненный дол")
    a = k.atlas
    a.put("steel", tile_steel((236, 150, 90), (255, 226, 160), (170, 70, 28)))
    a.put("edge", tile_flat((255, 238, 190), (255, 255, 240), 0.03))
    a.put("spine", tile_metal((110, 44, 20)))
    a.put("fuller", tile_flat((255, 110, 40), (255, 190, 80), 0.06, vertical=1.0))
    a.put("grip", tile_wrap((74, 30, 14), (44, 18, 8)))
    a.put("gripside", tile_flat((40, 16, 8)))
    a.put("guard", tile_metal((178, 70, 28)))
    a.put("guarddark", tile_metal((72, 28, 12)))
    a.put("glow", tile_glow((255, 244, 190), (255, 110, 30)))
    k.box(7.2, 0.0, 7.2, 8.8, 0.7, 8.8, "guard", "guarddark")
    k.box(7.2, 0.7, 7.2, 8.8, 4.5, 8.8, "grip", "gripside")
    k.box(5.6, 4.5, 7.2, 10.4, 5.2, 8.8, "guard", "guarddark")
    k.box(6.1, 5.2, 7.4, 6.8, 6.0, 8.6, "glow", emissive=True)
    k.box(9.2, 5.2, 7.4, 9.9, 6.0, 8.6, "glow", emissive=True)

    def width(s):
        base = 2.0 * (1 - 0.5 * s) + 0.7 * math.sin(s * 22.0) * (1 - s)
        return base if s < 0.9 else max(0.3, base * (1 - (s - 0.9) / 0.1) + 0.2)

    def center(s):
        return 8.0 + 0.55 * math.sin(s * 11.0) * (1 - s)

    k.slices(5.2, 16.2, width, center, 0.95, "steel", "spine", steps=44)
    k.slices(6.0, 14.6, lambda s: 0.5, lambda s: center(0.07 + 0.82 * s), 1.05, "fuller", "fuller", steps=30, emissive=True)
    return k


def knife_demon():
    k = Knife("demon", "Демонический", "Крючковатый клинок с зубцами")
    a = k.atlas
    a.put("steel", tile_steel((150, 30, 46), (230, 80, 90), (70, 10, 22)))
    a.put("edge", tile_flat((255, 170, 170), (255, 230, 230), 0.04))
    a.put("spine", tile_metal((44, 10, 18)))
    a.put("fuller", tile_flat((36, 8, 14), (90, 16, 28), 0.06, vertical=1.0))
    a.put("grip", tile_ridged((26, 12, 16), (12, 5, 8)))
    a.put("gripside", tile_flat((18, 8, 11)))
    a.put("guard", tile_metal((96, 22, 34)))
    a.put("guarddark", tile_metal((34, 8, 14)))
    a.put("glow", tile_glow((255, 220, 180), (255, 60, 40)))
    k.box(7.2, 0.0, 7.2, 8.8, 0.8, 8.8, "guarddark")
    k.box(7.3, -1.0, 7.3, 8.7, 0.0, 8.7, "glow", emissive=True)
    k.box(7.2, 0.8, 7.2, 8.8, 4.6, 8.8, "grip", "gripside")
    k.box(5.4, 4.6, 7.2, 10.6, 5.3, 8.8, "guard", "guarddark")
    # рога гарды
    k.box(5.2, 5.3, 7.5, 5.9, 6.5, 8.5, "guarddark")
    k.box(10.1, 5.3, 7.5, 10.8, 6.5, 8.5, "guarddark")
    k.box(4.8, 6.5, 7.7, 5.5, 7.3, 8.3, "guarddark")
    k.box(10.5, 6.5, 7.7, 11.2, 7.3, 8.3, "guarddark")

    length = 10.6

    def width(s):
        return 2.6 * (1 - 0.4 * s) if s < 0.84 else max(0.3, 1.6 * (1 - (s - 0.84) / 0.16) + 0.25)

    def center(s):
        return 8.0 + 3.2 * s * s

    k.slices(5.3, 5.3 + length, width, center, 1.0, "steel", "spine", steps=30)
    # зубцы по внутренней кромке
    for i in range(5):
        s = 0.12 + i * 0.15
        c = center(s) - width(s) / 2.0
        k.box(c - 0.95, 5.3 + s * length, 7.65, c + 0.1, 5.3 + s * length + 0.8, 8.35, "steel", "spine")
    k.box(7.9, 6.0, 8.47, 8.3, 12.0, 8.53, "glow", emissive=True)
    return k


KNIVES = [knife_m9, knife_karambit, knife_butterfly, knife_bayonet, knife_hunter, knife_arcanum, knife_flame, knife_demon]


# ------------------------------------------------------------------------------------------ превью


def preview(k, scale=22):
    """Вид спереди без поворота: показывает силуэт и раскраску, а не ванильную раскладку руки."""
    atlas = k.atlas.img
    w = int(16 * scale)
    img = Image.new("RGBA", (w, w), (34, 36, 44, 255))
    order = sorted(k.elements, key=lambda e: e["to"][2])
    for e in order:
        x0, y0, z0 = e["from"]
        x1, y1, z1 = e["to"]
        face = e["faces"]["south"]
        u0, v0, u1, v1 = face["uv"]
        px0, px1 = int(x0 * scale), int(x1 * scale) + 1
        py0, py1 = int((16 - y1) * scale), int((16 - y0) * scale) + 1
        for py in range(max(0, py0), min(w, py1)):
            for px in range(max(0, px0), min(w, px1)):
                fx = (px - px0) / max(1, px1 - px0 - 1)
                fy = (py - py0) / max(1, py1 - py0 - 1)
                ax = (u0 + (u1 - u0) * fx) * 4.0
                ay = (v0 + (v1 - v0) * fy) * 4.0
                ax = min(63, max(0, int(ax)))
                ay = min(63, max(0, int(ay)))
                img.putpixel((px, py), atlas.getpixel((ax, ay)))
    return img


def main():
    os.makedirs(os.path.join(ASSETS, "models", "item"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "items"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    want_preview = "--preview" in sys.argv
    if want_preview:
        os.makedirs(PREVIEW, exist_ok=True)
    for build in KNIVES:
        k = build()
        model = k.finish()
        with open(os.path.join(ASSETS, "models", "item", k.slug + ".json"), "w", encoding="utf-8") as fh:
            json.dump(model, fh, ensure_ascii=False, indent=1)
        with open(os.path.join(ASSETS, "items", k.slug + ".json"), "w", encoding="utf-8") as fh:
            json.dump({"model": {"type": "minecraft:model", "model": "doorevisuals:item/" + k.slug}}, fh, indent=2)
        k.atlas.img.save(os.path.join(ASSETS, "textures", "item", "kn_" + k.slug + ".png"))
        print("%-10s %3d элементов" % (k.slug, len(k.elements)))
        if want_preview:
            preview(k).save(os.path.join(PREVIEW, k.slug + ".png"))


if __name__ == "__main__":
    main()
