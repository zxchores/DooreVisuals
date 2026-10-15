"""Преобразует glTF-модель ножа (Sketchfab) в кубоидную модель предмета Minecraft.

Модель в игре состоит только из кубов, поэтому сетка превращается в карту высот: плоскость клинка делится на клетки,
у каждой клетки берётся толщина (минимум и максимум по z) и цвет передней и задней стороны из текстуры исходника.
Соседние клетки с одинаковыми параметрами склеиваются в один куб. Цвета сводятся к палитре из 256 оттенков, палитра
лежит в текстуре 64x64 (по блоку 4x4 на цвет, чтобы мипмапы не смешивали соседей).

Использование:
    python -I tools/mesh_knife.py inspect <папка-модели> <out.png>
    python -I tools/mesh_knife.py build <slug> [--preview]
Параметры моделей (оси, длина, исключаемые части) лежат в MODELS ниже.
"""
import json
import math
import os
import struct
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "doorevisuals")
PREVIEW = os.path.join(HERE, "preview")
SCRATCH = os.environ.get("KNIFE_SRC", os.path.join(HERE, "src_models"))

sys.path.insert(0, HERE)


# ------------------------------------------------------------------------------------------ матрицы (по столбцам)


def ident():
    return [1.0, 0, 0, 0, 0, 1.0, 0, 0, 0, 0, 1.0, 0, 0, 0, 0, 1.0]


def mmul(a, b):
    r = [0.0] * 16
    for c in range(4):
        for rr in range(4):
            r[c * 4 + rr] = sum(a[k * 4 + rr] * b[c * 4 + k] for k in range(4))
    return r


def trs(t, q, s):
    x, y, z, w = q
    xx, yy, zz = x * x, y * y, z * z
    xy, xz, yz = x * y, x * z, y * z
    wx, wy, wz = w * x, w * y, w * z
    r = [
        1 - 2 * (yy + zz), 2 * (xy + wz), 2 * (xz - wy), 0,
        2 * (xy - wz), 1 - 2 * (xx + zz), 2 * (yz + wx), 0,
        2 * (xz + wy), 2 * (yz - wx), 1 - 2 * (xx + yy), 0,
        0, 0, 0, 1,
    ]
    for c in range(3):
        for rr in range(3):
            r[c * 4 + rr] *= s[c]
    r[12], r[13], r[14] = t
    return r


def apply(m, p):
    return (
        m[0] * p[0] + m[4] * p[1] + m[8] * p[2] + m[12],
        m[1] * p[0] + m[5] * p[1] + m[9] * p[2] + m[13],
        m[2] * p[0] + m[6] * p[1] + m[10] * p[2] + m[14],
    )


def invert(m):
    # общая обратная матрица 4x4 (метод Гаусса)
    a = [[m[c * 4 + r] for c in range(4)] + [1.0 if r == i else 0.0 for i in range(4)] for r in range(4)]
    n = 4
    for i in range(n):
        piv = max(range(i, n), key=lambda r: abs(a[r][i]))
        a[i], a[piv] = a[piv], a[i]
        d = a[i][i]
        if abs(d) < 1e-12:
            return ident()
        a[i] = [v / d for v in a[i]]
        for r in range(n):
            if r != i:
                f = a[r][i]
                a[r] = [v - f * w for v, w in zip(a[r], a[i])]
    out = [0.0] * 16
    for r in range(4):
        for c in range(4):
            out[c * 4 + r] = a[r][4 + c]
    return out


# ------------------------------------------------------------------------------------------ загрузка glTF

COMP = {5120: ("b", 1), 5121: ("B", 1), 5122: ("h", 2), 5123: ("H", 2), 5125: ("I", 4), 5126: ("f", 4)}
NUM = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


class Gltf:
    def __init__(self, folder):
        self.folder = folder
        with open(os.path.join(folder, "scene.gltf"), encoding="utf-8") as fh:
            self.g = json.load(fh)
        self.bufs = []
        for b in self.g["buffers"]:
            with open(os.path.join(folder, b["uri"]), "rb") as fh:
                self.bufs.append(fh.read())
        self.images = {}

    def accessor(self, idx):
        a = self.g["accessors"][idx]
        view = self.g["bufferViews"][a["bufferView"]]
        fmt, size = COMP[a["componentType"]]
        n = NUM[a["type"]]
        stride = view.get("byteStride") or size * n
        base = view.get("byteOffset", 0) + a.get("byteOffset", 0)
        data = self.bufs[view["buffer"]]
        out = []
        for i in range(a["count"]):
            out.append(struct.unpack_from("<" + fmt * n, data, base + i * stride))
        return out

    def image_for(self, material):
        pbr = self.g["materials"][material].get("pbrMetallicRoughness", {})
        tex = pbr.get("baseColorTexture")
        factor = pbr.get("baseColorFactor", [1, 1, 1, 1])
        img = None
        if tex is not None:
            src = self.g["textures"][tex["index"]]["source"]
            if src not in self.images:
                uri = self.g["images"][src]["uri"]
                self.images[src] = Image.open(os.path.join(self.folder, uri)).convert("RGB")
            img = self.images[src]
        return img, factor

    def triangles(self):
        """Список (p0, p1, p2, uv0, uv1, uv2, имя_части, материал) в мировых координатах исходника."""
        g = self.g
        nodes = g["nodes"]
        world = {}

        def walk(i, parent):
            n = nodes[i]
            if "matrix" in n:
                local = list(n["matrix"])
            else:
                local = trs(n.get("translation", [0, 0, 0]), n.get("rotation", [0, 0, 0, 1]), n.get("scale", [1, 1, 1]))
            world[i] = mmul(parent, local)
            for c in n.get("children", []):
                walk(c, world[i])

        for root in g["scenes"][g.get("scene", 0)]["nodes"]:
            walk(root, ident())
        out = []
        for i, n in enumerate(nodes):
            if "mesh" not in n:
                continue
            skin = g["skins"][n["skin"]] if "skin" in n and "skins" in g else None
            joints = None
            if skin is not None:
                ibm = self.accessor(skin["inverseBindMatrices"])
                inv_world = invert(world[i])
                joints = [mmul(inv_world, mmul(world[j], list(ibm[k]))) for k, j in enumerate(skin["joints"])]
            for prim in g["meshes"][n["mesh"]]["primitives"]:
                pos = self.accessor(prim["attributes"]["POSITION"])
                uv = self.accessor(prim["attributes"]["TEXCOORD_0"]) if "TEXCOORD_0" in prim["attributes"] else [(0.0, 0.0)] * len(pos)
                if joints is not None and "JOINTS_0" in prim["attributes"]:
                    jj = self.accessor(prim["attributes"]["JOINTS_0"])
                    ww = self.accessor(prim["attributes"]["WEIGHTS_0"])
                    skinned = []
                    for p, jv, wv in zip(pos, jj, ww):
                        acc = [0.0, 0.0, 0.0]
                        for jx, wx in zip(jv, wv):
                            if wx > 0:
                                q = apply(joints[jx], p)
                                acc = [acc[0] + q[0] * wx, acc[1] + q[1] * wx, acc[2] + q[2] * wx]
                        skinned.append(tuple(acc))
                    pos = skinned
                idx = [v[0] for v in self.accessor(prim["indices"])] if "indices" in prim else list(range(len(pos)))
                w = world[i]
                wp = [apply(w, p) for p in pos]
                for t in range(0, len(idx) - 2, 3):
                    a, b, c = idx[t], idx[t + 1], idx[t + 2]
                    out.append((wp[a], wp[b], wp[c], uv[a], uv[b], uv[c], n.get("name", str(i)), prim.get("material", 0)))
        return out


# ------------------------------------------------------------------------------------------ выборка поверхности


def sample_color(img, factor, uv):
    if img is None:
        return tuple(int(max(0.0, min(1.0, f)) * 255) for f in factor[:3])
    w, h = img.size
    u = uv[0] % 1.0
    v = uv[1] % 1.0
    px = img.getpixel((min(w - 1, int(u * w)), min(h - 1, int(v * h))))
    return (int(px[0] * factor[0]), int(px[1] * factor[1]), int(px[2] * factor[2]))


def surface_points(gl, tris, step, keep=None):
    """Точки поверхности: (x, y, z, (r, g, b), нормаль_z, часть) с шагом примерно step."""
    cache = {}
    pts = []
    for p0, p1, p2, t0, t1, t2, name, mat in tris:
        if keep is not None and not keep(name):
            continue
        if mat not in cache:
            cache[mat] = gl.image_for(mat)
        img, factor = cache[mat]
        e1 = (p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2])
        e2 = (p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2])
        nx = e1[1] * e2[2] - e1[2] * e2[1]
        ny = e1[2] * e2[0] - e1[0] * e2[2]
        nz = e1[0] * e2[1] - e1[1] * e2[0]
        nl = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
        edge = max(math.dist(p0, p1), math.dist(p1, p2), math.dist(p0, p2))
        n = max(1, int(math.ceil(edge / step)))
        for i in range(n + 1):
            for j in range(n + 1 - i):
                a = i / n
                b = j / n
                c = 1.0 - a - b
                p = (p0[0] * c + p1[0] * a + p2[0] * b, p0[1] * c + p1[1] * a + p2[1] * b, p0[2] * c + p1[2] * a + p2[2] * b)
                uv = (t0[0] * c + t1[0] * a + t2[0] * b, t0[1] * c + t1[1] * a + t2[1] * b)
                pts.append((p[0], p[1], p[2], sample_color(img, factor, uv), (nx / nl, ny / nl, nz / nl), name))
    return pts


# ------------------------------------------------------------------------------------------ осмотр


def view_png(pts, axes, size=420, bg=(30, 32, 40)):
    """Рисует вид на плоскость осей axes=(горизонталь, вертикаль, глубина); знак оси: -1 переворачивает."""
    (ah, sh), (av, sv), (ad, sd) = axes
    h = [p[ah] * sh for p in pts]
    v = [p[av] * sv for p in pts]
    lo_h, hi_h, lo_v, hi_v = min(h), max(h), min(v), max(v)
    k = (size - 20) / max(hi_h - lo_h, hi_v - lo_v, 1e-9)
    w = int((hi_h - lo_h) * k) + 20
    ht = int((hi_v - lo_v) * k) + 20
    img = Image.new("RGB", (w, ht), bg)
    depth = {}
    for p, x, y in zip(pts, h, v):
        px = int((x - lo_h) * k) + 10
        py = ht - 10 - int((y - lo_v) * k)
        d = p[ad] * sd
        if (px, py) not in depth or d > depth[(px, py)][0]:
            depth[(px, py)] = (d, p[3])
    for (px, py), (_, col) in depth.items():
        if 0 <= px < w and 0 <= py < ht:
            img.putpixel((px, py), col)
    return img


def cmd_inspect(folder, out):
    gl = Gltf(folder)
    tris = gl.triangles()
    print("треугольников:", len(tris))
    names = {}
    for t in tris:
        names.setdefault(t[6], [0, [1e9] * 3, [-1e9] * 3])
        e = names[t[6]]
        e[0] += 1
        for p in t[:3]:
            for a in range(3):
                e[1][a] = min(e[1][a], p[a])
                e[2][a] = max(e[2][a], p[a])
    for n, (c, lo, hi) in names.items():
        print("%-34s %6d  min(%.2f %.2f %.2f) max(%.2f %.2f %.2f)" % (n, c, *lo, *hi))
    lo = [min(p[a] for t in tris for p in t[:3]) for a in range(3)]
    hi = [max(p[a] for t in tris for p in t[:3]) for a in range(3)]
    print("общий габарит", [round(h - l, 3) for l, h in zip(lo, hi)], "min", [round(x, 3) for x in lo])
    size = max(h - l for l, h in zip(lo, hi))
    pts = surface_points(gl, tris, size / 260.0)
    views = [
        view_png(pts, ((0, 1), (1, 1), (2, 1))),
        view_png(pts, ((0, 1), (2, 1), (1, -1))),
        view_png(pts, ((2, 1), (1, 1), (0, 1))),
    ]
    total_w = sum(v.width for v in views) + 10 * (len(views) - 1)
    sheet = Image.new("RGB", (total_w, max(v.height for v in views)), (10, 10, 12))
    x = 0
    for v in views:
        sheet.paste(v, (x, 0))
        x += v.width + 10
    sheet.save(out)
    print("сохранено", out)


# ------------------------------------------------------------------------------------------ подготовка моделей


def rot_xz(pts, part, pivot, angle):
    """Поворот точек части в плоскости x-z вокруг pivot (x, z); нормали поворачиваются так же."""
    ca, sa = math.cos(angle), math.sin(angle)
    out = []
    for p in pts:
        if p[5] != part:
            out.append(p)
            continue
        dx, dz = p[0] - pivot[0], p[2] - pivot[1]
        n = p[4]
        out.append((pivot[0] + dx * ca - dz * sa, p[1], pivot[1] + dx * sa + dz * ca, p[3], (n[0] * ca - n[2] * sa, n[1], n[0] * sa + n[2] * ca), p[5]))
    return out


def shift(pts, part, d):
    return [(p[0] + d[0], p[1] + d[1], p[2] + d[2], p[3], p[4], p[5]) if p[5].startswith(part) else p for p in pts]


def prep_balisong(pts):
    """Рукояти в модели разведены «галочкой»; ставим их в линию за клинком, одна над другой по толщине."""
    pivot_top, pivot_bot = (0.01, 0.188), (-0.061, 0.178)
    a_top = -math.atan2(2.199 - 0.01, 4.842 + 0.188)
    a_bot = -math.atan2(-1.818 + 0.061, 5.054 + 0.178)
    pts = rot_xz(pts, "TopHandle_TopHandle_carbon_0", pivot_top, a_top)
    pts = rot_xz(pts, "BottomHandle_BottomHandle_carbon_0", pivot_bot, a_bot)
    pts = shift(pts, "TopHandle", (-0.01, 0.28, 0.0))
    pts = shift(pts, "BottomHandle", (0.061, -0.28, 0.0))
    return pts


def prep_stiletto(pts):
    """Клинок в модели спрятан в рукояти; выдвигаем его вперёд (по +z), основание остаётся внутри."""
    return shift(pts, "Blade_low", (0.0, 0.0, 15.0))


# axes: для каждой оси результата (x — ширина, y — вдоль клинка остриём вверх, z — толщина) пара (ось исходника, знак)
MODELS = {
    "sk_m9": {
        "folder": "m9_bayonet", "title": "М9 · sh4m4n",
        "axes": ((1, 1), (0, -1), (2, 1)), "length": 15.6, "cell": 0.3, "mint": 0.3, "prep": None,
    },
    "sk_butterfly": {
        "folder": "balisong_butterfly-knife", "title": "Бабочка · martiangold",
        "axes": ((0, 1), (2, 1), (1, -1)), "length": 15.6, "cell": 0.3, "mint": 0.3, "prep": prep_balisong,
    },
    "sk_stiletto": {
        "folder": "stiletto_knife", "title": "Стилет · komov",
        "axes": ((1, 1), (2, 1), (0, 1)), "length": 15.6, "cell": 0.3, "mint": 0.3, "prep": prep_stiletto,
    },
}


def convert(cfg):
    gl = Gltf(os.path.join(SCRATCH, cfg["folder"]))
    tris = gl.triangles()
    (ax, sx), (ay, sy), (az, sz) = cfg["axes"]
    # длина по вершинам с учётом подготовки (клинок стилета выдвигается и меняет габарит)
    probe = surface_points(gl, tris, max(1e-9, 1e9))
    if cfg["prep"]:
        probe = cfg["prep"](probe)
    vy = [p[ay] * sy for p in probe]
    k = cfg["length"] / (max(vy) - min(vy))
    step = cfg["cell"] / 3.0 / k
    pts = surface_points(gl, tris, step)
    if cfg["prep"]:
        pts = cfg["prep"](pts)
    mapped = []
    for p in pts:
        n = p[4]
        mapped.append((p[ax] * sx * k, p[ay] * sy * k, p[az] * sz * k, p[3], n[az] * sz, p[5]))
    xs = [m[0] for m in mapped]
    ys = [m[1] for m in mapped]
    zs = [m[2] for m in mapped]
    cx = (min(xs) + max(xs)) / 2.0
    cz = (min(zs) + max(zs)) / 2.0
    y0 = min(ys)
    c = cfg["cell"]
    cells = {}
    for x, y, z, col, nz, _ in mapped:
        key = (int(math.floor((x - cx + 8.0) / c)), int(math.floor((y - y0) / c)))
        z -= cz
        e = cells.get(key)
        if e is None:
            e = cells[key] = [z, z, [], [], []]
        e[0] = min(e[0], z)
        e[1] = max(e[1], z)
        (e[2] if nz > 0.25 else e[3] if nz < -0.25 else e[4]).append(col)

    def avg(cols):
        return tuple(sum(c_[i] for c_ in cols) // len(cols) for i in range(3))

    for e in cells.values():
        allc = e[2] + e[3] + e[4]
        front = avg(e[2]) if e[2] else avg(allc)
        back = avg(e[3]) if e[3] else avg(allc)
        side = avg(allc)
        thick = max(cfg["mint"], e[1] - e[0])
        mid = (e[0] + e[1]) / 2.0
        e[:] = [round((mid - thick / 2.0) / 0.12) * 0.12, round((mid + thick / 2.0) / 0.12) * 0.12, front, back, side]
    return cells, c


def quantize(cells, count=252):
    cols = []
    for e in cells.values():
        cols += [e[2], e[3], e[4]]
    uniq = sorted(set(cols))
    strip = Image.new("RGB", (len(uniq), 1))
    strip.putdata(uniq)
    q = strip.quantize(colors=count, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    pal = q.getpalette()[: 3 * count]
    idx = {c: q.getpixel((i, 0)) for i, c in enumerate(uniq)}
    return idx, [tuple(pal[i * 3:i * 3 + 3]) for i in range(count)]


def build(slug, preview=False):
    from knife_models import DISPLAY
    cfg = MODELS[slug]
    cells, c = convert(cfg)
    idx, pal = quantize(cells)
    grid = {}
    for key, e in cells.items():
        grid[key] = (e[0], e[1], idx[e[2]], idx[e[3]], idx[e[4]])
    used = set()
    boxes = []
    for iy in sorted({k[1] for k in grid}):
        for ix in sorted(k[0] for k in grid if k[1] == iy):
            if (ix, iy) in used:
                continue
            val = grid[(ix, iy)]
            w = 1
            while grid.get((ix + w, iy)) == val and (ix + w, iy) not in used:
                w += 1
            h = 1
            while all(grid.get((ix + i, iy + h)) == val and (ix + i, iy + h) not in used for i in range(w)):
                h += 1
            for j in range(h):
                for i in range(w):
                    used.add((ix + i, iy + j))
            boxes.append((ix, iy, w, h, val))
    atlas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for i, col in enumerate(pal):
        atlas.paste(col + (255,), ((i % 16) * 4, (i // 16) * 4, (i % 16) * 4 + 4, (i // 16) * 4 + 4))

    def uv(i):
        bx, by = float(i % 16), float(i // 16)
        return [bx + 0.25, by + 0.25, bx + 0.75, by + 0.75]

    elements = []
    for ix, iy, w, h, (z0, z1, front, back, side) in boxes:
        faces = {"north": {"uv": uv(back), "texture": "#0"}, "south": {"uv": uv(front), "texture": "#0"}}
        for f in ("east", "west", "up", "down"):
            faces[f] = {"uv": uv(side), "texture": "#0"}
        elements.append({
            "from": [round(ix * c, 3), round(iy * c, 3), round(8.0 + z0, 3)],
            "to": [round((ix + w) * c, 3), round((iy + h) * c, 3), round(8.0 + z1, 3)],
            "rotation": {"angle": -45, "axis": "z", "origin": [8, 8, 8]},
            "faces": faces,
        })
    model = {
        "credit": "Based on a 3D model by " + cfg["title"].split("· ")[1] + " (Sketchfab, CC BY 4.0), converted for DooreVisuals",
        "textures": {"0": "doorevisuals:item/kn_" + slug, "particle": "doorevisuals:item/kn_" + slug},
        "display": DISPLAY,
        "elements": elements,
    }
    for sub in ("models/item", "items", "textures/item"):
        os.makedirs(os.path.join(ASSETS, *sub.split("/")), exist_ok=True)
    with open(os.path.join(ASSETS, "models", "item", slug + ".json"), "w", encoding="utf-8") as fh:
        json.dump(model, fh, ensure_ascii=False, indent=1)
    with open(os.path.join(ASSETS, "items", slug + ".json"), "w", encoding="utf-8") as fh:
        json.dump({"model": {"type": "minecraft:model", "model": "doorevisuals:item/" + slug}}, fh, indent=2)
    atlas.save(os.path.join(ASSETS, "textures", "item", "kn_" + slug + ".png"))
    print("%-12s %4d элементов, из клеток %d" % (slug, len(elements), len(grid)))
    if preview:
        os.makedirs(PREVIEW, exist_ok=True)
        render_preview(elements, atlas).save(os.path.join(PREVIEW, slug + ".png"))


def render_preview(elements, atlas, scale=24):
    """Вид спереди и сбоку без поворота руки: силуэт, раскраска и толщина."""
    w = 16 * scale
    img = Image.new("RGBA", (w * 2 + 10, w), (34, 36, 44, 255))

    def tex(face):
        u0, v0, u1, v1 = face["uv"]
        return atlas.getpixel((min(63, int((u0 + u1) * 2)), min(63, int((v0 + v1) * 2))))

    for e in sorted(elements, key=lambda e: e["to"][2]):
        x0, y0, _ = e["from"]
        x1, y1, _ = e["to"]
        px0, px1, py0, py1 = int(x0 * scale), int(x1 * scale), int((16 - y1) * scale), int((16 - y0) * scale)
        img.paste(tex(e["faces"]["south"]), (px0, py0, max(px1, px0 + 1), max(py1, py0 + 1)))
    for e in sorted(elements, key=lambda e: e["to"][0]):
        _, y0, z0 = e["from"]
        _, y1, z1 = e["to"]
        px0, px1, py0, py1 = int(z0 * scale) + w + 10, int(z1 * scale) + w + 10, int((16 - y1) * scale), int((16 - y0) * scale)
        img.paste(tex(e["faces"]["east"]), (px0, py0, max(px1, px0 + 1), max(py1, py0 + 1)))
    return img


if __name__ == "__main__":
    if len(sys.argv) >= 4 and sys.argv[1] == "inspect":
        cmd_inspect(sys.argv[2], sys.argv[3])
    elif len(sys.argv) >= 3 and sys.argv[1] == "build":
        for name in (list(MODELS) if sys.argv[2] == "all" else [sys.argv[2]]):
            build(name, "--preview" in sys.argv)
