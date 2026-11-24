"""Рисует текстуры мода: Визершторм, Херобрин, земной шар.

Запуск из корня проекта:  python tools/textures.py
Раскладка кубов совпадает с WitherStormModel / GlobeRenderer — меняйте их вместе.
"""
import random
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mobtower/textures/entity"
CLEAR = (0, 0, 0, 0)


class Image:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[CLEAR] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c if len(c) == 4 else (*c, 255)

    def fill(self, x, y, w, h, color_fn):
        for j in range(h):
            for i in range(w):
                self.set(x + i, y + j, color_fn(i, j))

    def save(self, name):
        raw = b"".join(b"\x00" + bytes(v for p in row for v in p) for row in self.px)

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
        OUT.mkdir(parents=True, exist_ok=True)
        (OUT / name).write_bytes(png)


def box_faces(u, v, w, h, d):
    """Грани куба на развёртке, как её делает Minecraft: имя -> (x, y, ширина, высота)."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def shade(c, k):
    return tuple(max(0, min(255, int(ch * k))) for ch in c)


def noisy(base, rng, amount=0.18):
    return lambda i, j: shade(base, 1.0 + rng.uniform(-amount, amount))


# ---------------------------------------------------------------- Визершторм
def wither_storm():
    rng = random.Random(7)
    img = Image(128, 128)
    eyes = Image(128, 128)

    # Поглощённые блоки: обсидиан, глубинный сланец, земля, пурпур
    palette = [(28, 16, 38), (20, 12, 26), (44, 30, 52), (36, 34, 40), (52, 36, 28), (60, 26, 78), (14, 8, 18)]

    def absorbed(x0, y0, w, h):
        tiles = {}
        for j in range(h):
            for i in range(w):
                key = ((x0 + i) // 4, (y0 + j) // 4)
                if key not in tiles:
                    tiles[key] = rng.choice(palette)
                c = shade(tiles[key], 1.0 + rng.uniform(-0.25, 0.25))
                if rng.random() < 0.04:
                    c = (120, 40, 170)  # фиолетовые прожилки
                img.set(x0 + i, y0 + j, c)

    boxes = [
        (0, 0, 16, 16, 16),   # центральная голова
        (64, 0, 12, 12, 12),  # боковые головы
        (0, 32, 48, 6, 6),    # плечи
        (0, 44, 12, 18, 8),   # тело
        (40, 44, 4, 8, 4),    # сегмент щупальца
        (56, 44, 6, 6, 6),    # обломок
        (80, 44, 20, 3, 3),   # ребро
    ]
    for b in boxes:
        for (x, y, w, h) in box_faces(*b).values():
            absorbed(x, y, w, h)

    glow_core, glow_edge = (240, 200, 255), (176, 64, 255)

    def face(fx, fy, size, eye_rows, eye_cols, mouth):
        for r in eye_rows:
            for c0, c1 in eye_cols:
                for c in range(c0, c1 + 1):
                    col = glow_core if c0 < c < c1 else glow_edge
                    img.set(fx + c, fy + r, col)
                    eyes.set(fx + c, fy + r, col)
        mr0, mr1, mc0, mc1 = mouth
        for r in range(mr0, mr1 + 1):
            for c in range(mc0, mc1 + 1):
                img.set(fx + c, fy + r, (6, 0, 8))
        for c in range(mc0, mc1 + 1, 2):  # зубы
            img.set(fx + c, fy + mr0, (150, 140, 130))

    face(16, 16, 16, (5, 6), ((2, 6), (9, 13)), (10, 13, 3, 12))
    face(76, 12, 12, (4, 5), ((1, 4), (7, 10)), (8, 9, 3, 8))

    img.save("wither_storm.png")
    eyes.save("wither_storm_eyes.png")


# ---------------------------------------------------------------- Херобрин
def herobrine():
    rng = random.Random(42)
    img = Image(64, 64)
    eyes = Image(64, 64)

    skin, hair, shirt, pants, shoes = (180, 132, 109), (43, 30, 13), (0, 168, 168), (63, 63, 160), (90, 90, 90)

    head = box_faces(0, 0, 8, 8, 8)
    for name, (x, y, w, h) in head.items():
        base = hair if name in ("top", "back") else skin
        img.fill(x, y, w, h, noisy(base, rng, 0.08))
        if name in ("right", "left", "front"):
            img.fill(x, y, w, 2 if name == "front" else 3, noisy(hair, rng, 0.1))
    fx, fy = head["front"][0], head["front"][1]
    for c in (1, 2, 5, 6):  # белые глаза без зрачков
        img.set(fx + c, fy + 4, (255, 255, 255))
        eyes.set(fx + c, fy + 4, (255, 255, 255))
    img.set(fx + 3, fy + 5, shade(skin, 0.8))
    img.set(fx + 4, fy + 5, shade(skin, 0.8))
    for c in range(2, 6):
        img.set(fx + c, fy + 6, (106, 64, 48))
        img.set(fx + c, fy + 7, shade(hair, 1.4))

    for name, (x, y, w, h) in box_faces(16, 16, 8, 12, 4).items():
        img.fill(x, y, w, h, noisy(shirt, rng, 0.1))

    for name, (x, y, w, h) in box_faces(40, 16, 4, 12, 4).items():
        img.fill(x, y, w, h, noisy(skin, rng, 0.08))
        if name not in ("top", "bottom"):
            img.fill(x, y, w, 4, noisy(shirt, rng, 0.1))
        elif name == "top":
            img.fill(x, y, w, h, noisy(shirt, rng, 0.1))

    for name, (x, y, w, h) in box_faces(0, 16, 4, 12, 4).items():
        img.fill(x, y, w, h, noisy(pants, rng, 0.1))
        if name == "bottom":
            img.fill(x, y, w, h, noisy(shoes, rng, 0.1))
        elif name != "top":
            img.fill(x, y + h - 2, w, 2, noisy(shoes, rng, 0.1))

    img.save("herobrine.png")
    eyes.save("herobrine_eyes.png")


# ---------------------------------------------------------------- Земной шар
def globe():
    rng = random.Random(3)
    img = Image(32, 16)
    # Коричневый, как земля: светлая почва, тёмные комья и прожилки, сухие «полюса»
    ocean, land, sand, ice = (121, 85, 58), (84, 58, 38), (150, 108, 74), (134, 96, 66)

    faces = box_faces(0, 0, 8, 8, 8)
    for name, (x, y, w, h) in faces.items():
        if name in ("top", "bottom"):
            img.fill(x, y, w, h, noisy(ice, rng, 0.05))
            continue
        land_mask = [[False] * w for _ in range(h)]
        for _ in range(2):
            cx, cy, r = rng.randint(1, 6), rng.randint(2, 5), rng.uniform(1.5, 2.8)
            for j in range(h):
                for i in range(w):
                    if (i - cx) ** 2 + ((j - cy) * 1.3) ** 2 < r * r + rng.uniform(-1, 1):
                        land_mask[j][i] = True
        for j in range(h):
            for i in range(w):
                if j == 0 or j == h - 1:
                    c = ice
                elif land_mask[j][i]:
                    edge = any(
                        not land_mask[j + dj][i + di]
                        for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1))
                        if 0 <= i + di < w and 0 <= j + dj < h
                    )
                    c = sand if edge and rng.random() < 0.4 else land
                else:
                    c = ocean
                img.set(x + i, y + j, shade(c, 1.0 + rng.uniform(-0.1, 0.1)))

    img.save("globe.png")


if __name__ == "__main__":
    wither_storm()
    herobrine()
    globe()
    print("Текстуры записаны в", OUT)
