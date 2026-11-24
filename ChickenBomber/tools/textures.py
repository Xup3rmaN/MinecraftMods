"""Рисует текстуру самолёта.

Запуск из корня проекта:  python tools/textures.py
Раскладка кубов совпадает с AirplaneModel — меняйте их вместе.
"""
import random
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/chickenbomber/textures/entity"
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


def airplane():
    rng = random.Random(11)
    img = Image(128, 128)

    def noisy(base, amount=0.07):
        return lambda i, j: shade(base, 1.0 + rng.uniform(-amount, amount))

    def paint_box(box, base, amount=0.07):
        for (x, y, w, h) in box_faces(*box).values():
            img.fill(x, y, w, h, noisy(base, amount))

    red, white, cream, wood, steel, glass, rubber, dark = (
        (196, 38, 38), (238, 238, 232), (232, 224, 196), (122, 84, 44), (120, 124, 130), (150, 205, 235), (30, 30, 30), (40, 28, 24)
    )

    # Фюзеляж: красный, с белой полосой по бортам и открытой кабиной сверху
    fuselage = (0, 0, 10, 10, 36)
    paint_box(fuselage, red)
    faces = box_faces(*fuselage)
    for side in ("right", "left"):
        x, y, w, h = faces[side]
        img.fill(x, y + 4, w, 2, noisy(white))
    x, y, w, h = faces["top"]
    img.fill(x + 2, y + 13, w - 4, 10, noisy(dark, 0.1))
    x, y, w, h = faces["front"]
    img.fill(x, y, w, h, noisy(steel))

    # Крылья: кремовые, с красными кругами-опознавательными знаками
    wing = (0, 46, 30, 2, 12)
    paint_box(wing, cream)
    x, y, w, h = box_faces(*wing)["top"]
    for j in range(h):
        for i in range(w):
            r2 = (i - 20) ** 2 + (j - 5.5) ** 2
            if r2 <= 16:
                img.set(x + i, y + j, white if r2 <= 4 else red)
    for face in ("top", "bottom"):
        x, y, w, h = box_faces(*wing)[face]
        img.fill(x, y, 2, h, noisy(red))

    paint_box((0, 60, 24, 2, 8), cream)          # стабилизатор
    paint_box((64, 60, 2, 10, 8), red)           # киль
    for side in ("right", "left"):
        x, y, w, h = box_faces(64, 60, 2, 10, 8)[side]
        img.fill(x, y + 3, w, 2, noisy(white))
    paint_box((92, 0, 2, 16, 1), wood, 0.12)     # лопасть винта
    paint_box((100, 0, 4, 4, 3), steel)          # кок винта
    paint_box((100, 10, 10, 4, 1), glass, 0.04)  # лобовое стекло
    x, y, w, h = box_faces(100, 10, 10, 4, 1)["front"]
    img.fill(x + 1, y, 2, 1, lambda i, j: white)
    paint_box((110, 0, 2, 3, 3), rubber, 0.15)   # колёса

    img.save("airplane.png")


if __name__ == "__main__":
    airplane()
    print("Текстура записана в", OUT)
