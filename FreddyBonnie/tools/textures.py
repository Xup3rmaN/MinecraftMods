"""Рисует текстуры Фредди и Бонни.

Запуск из корня проекта:  python tools/textures.py
Раскладка кубов совпадает с FreddyModel / BonnieModel — меняйте их вместе.
"""
import random
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/freddybonnie/textures/entity"
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
    """Грани куба на развёртке, как её делает Minecraft: имя -> (x, y, ширина, высота).
    На гранях right/left столбцы идут от задней стороны к передней (right) и наоборот (left)."""
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


class Painter:
    def __init__(self, img, seed):
        self.img = img
        self.rng = random.Random(seed)

    def noisy(self, base, amount=0.06):
        return lambda i, j: shade(base, 1.0 + self.rng.uniform(-amount, amount))

    def box(self, box, base, amount=0.06):
        for (x, y, w, h) in box_faces(*box).values():
            self.img.fill(x, y, w, h, self.noisy(base, amount))

    def face(self, box, name):
        return box_faces(*box)[name]


# ---------------------------------------------------------------- Фредди
def freddy():
    img = Image(128, 64)
    p = Painter(img, 5)
    fur, light, black, blue, nose = (122, 74, 40), (196, 150, 104), (24, 22, 24), (70, 140, 230), (20, 14, 12)

    head = (0, 0, 10, 9, 10)
    p.box(head, fur)
    x, y, w, h = p.face(head, "front")
    for c in (2, 3, 6, 7):  # голубые глаза
        img.set(x + c, y + 3, blue)
        img.set(x + c, y + 4, shade(blue, 0.7))
    for c in (1, 2, 7, 8):  # брови
        img.set(x + c, y + 1, shade(fur, 0.6))
    img.fill(x + 2, y + 7, 6, 2, p.noisy(shade(light, 0.95)))  # подбородок

    p.box((40, 0, 7, 1, 7), black, 0.04)    # поля цилиндра
    p.box((68, 0, 5, 5, 5), black, 0.04)    # тулья
    for side in ("front", "back", "right", "left"):
        x, y, w, h = p.face((68, 0, 5, 5, 5), side)
        img.fill(x, y + h - 1, w, 1, lambda i, j: (60, 58, 62))  # лента
    p.box((88, 0, 3, 3, 1), fur)            # уши
    x, y, w, h = p.face((88, 0, 3, 3, 1), "front")
    img.set(x + 1, y + 1, light)
    p.box((96, 0, 5, 3, 2), light)          # морда
    x, y, w, h = p.face((96, 0, 5, 3, 2), "front")
    img.fill(x, y + 2, w, 1, p.noisy(shade(light, 0.7)))  # рот
    p.box((110, 0, 2, 1, 1), nose, 0.05)    # нос

    body = (0, 19, 10, 12, 6)
    p.box(body, fur)
    x, y, w, h = p.face(body, "front")
    img.fill(x + 2, y + 3, 6, 8, p.noisy(light))  # пузо
    p.box((32, 19, 5, 2, 1), black, 0.04)   # бабочка
    p.box((44, 19, 4, 12, 4), fur)          # руки
    leg = (60, 19, 5, 12, 5)
    p.box(leg, fur)
    x, y, w, h = p.face(leg, "bottom")
    img.fill(x, y, w, h, p.noisy(light))    # подошвы

    img.save("freddy.png")


# ---------------------------------------------------------------- Бонни
def bonnie():
    img = Image(128, 64)
    p = Painter(img, 9)
    fur, light, red, pink, eye = (96, 92, 196), (172, 168, 226), (200, 30, 40), (226, 140, 190), (220, 40, 80)
    wood, dark_wood, hole, string = (178, 112, 52), (110, 66, 32), (40, 24, 14), (230, 230, 210)

    head = (0, 0, 8, 8, 8)
    p.box(head, fur)
    x, y, w, h = p.face(head, "front")
    for c in (1, 2, 5, 6):  # красные глаза
        img.set(x + c, y + 3, eye)
    img.set(x + 2, y + 2, (255, 255, 255))
    img.set(x + 5, y + 2, (255, 255, 255))
    img.fill(x + 2, y + 6, 4, 2, p.noisy(light))

    ear = (32, 0, 2, 10, 1)
    p.box(ear, fur)
    x, y, w, h = p.face(ear, "front")
    img.fill(x, y + 1, w, h - 2, p.noisy(pink))  # розовое внутри
    p.box((38, 0, 4, 2, 1), light)          # морда
    x, y, w, h = p.face((38, 0, 4, 2, 1), "front")
    img.set(x + 1, y, (40, 20, 30))
    img.set(x + 2, y, (40, 20, 30))

    body = (0, 16, 8, 12, 4)
    p.box(body, fur)
    x, y, w, h = p.face(body, "front")
    img.fill(x + 1, y + 3, 6, 8, p.noisy(light))
    p.box((24, 16, 4, 2, 1), red, 0.04)     # бабочка
    p.box((40, 16, 4, 12, 4), fur)          # руки
    p.box((56, 16, 4, 12, 4), fur)          # ноги

    # Балалайка
    p.box((72, 16, 1, 1, 8), dark_wood)     # гриф
    neck_top = p.face((72, 16, 1, 1, 8), "top")
    img.fill(neck_top[0], neck_top[1], 1, 8, lambda i, j: string)
    body_box = (90, 16, 1, 7, 6)
    for name in ("top", "bottom", "front", "back"):
        x, y, w, h = p.face(body_box, name)
        img.fill(x, y, w, h, p.noisy(wood))
    # Треугольник на боковых гранях: узкий у грифа, широкий к концу
    for name, apex_first in (("right", True), ("left", False)):
        x, y, w, h = p.face(body_box, name)
        for i in range(w):
            k = i if apex_first else w - 1 - i  # 0 — у грифа
            half = 0.5 + k * (h / 2 - 0.5) / (w - 1)
            for j in range(h):
                if abs(j - (h - 1) / 2) <= half:
                    img.set(x + i, y + j, shade(wood, 1.0 + p.rng.uniform(-0.06, 0.06)))
                else:
                    img.set(x + i, y + j, CLEAR)
        hole_col = 2 if apex_first else w - 3
        img.set(x + hole_col, y + 3, hole)
        for i in range(w):
            img.set(x + i, y + 3, string) if img.px[y + 3][x + i] != CLEAR and i != hole_col else None

    img.save("bonnie.png")


if __name__ == "__main__":
    freddy()
    bonnie()
    print("Текстуры записаны в", OUT)
