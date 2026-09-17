"""Generates the Network Routing textures (no external art required).

Run from anywhere:  python tools/generate_textures.py

Outputs (relative to this module):
  common/src/main/resources/assets/network_routing/textures/item/routing_linker.png

The routing terminal's chest atlas
(common/src/main/resources/assets/network_routing/textures/entity/chest/network_routing.png)
is hand-supplied art, so it is only written when explicitly requested with --chest. A plain run
writes the linker sprite alone and can never clobber the terminal texture.

The chest atlas follows the vanilla chest model UV layout (64x64). For a box at UV (u,v) with
size (dx,dy,dz): down/bottom = (u+dz, v), up/top = (u+dz+dx, v), sides = row at y=v+dz in the
order west, north, east, south.
  lid    texOffs(0, 0)  box 14x5x14  -> underside (14,0)-(28,14), top (28,0)-(42,14), sides at y=14
  bottom texOffs(0, 19) box 14x10x14 -> floor (28,19)-(42,33), underside (14,19)-(28,33), sides at y=33
  lock   texOffs(0, 0)  box 2x4x1    -> (0,0)-(6,5)
The lid underside is the terminal screen; the bottom floor is the keyboard.
"""

import argparse
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "network_routing", "textures")

# Palette shared with the supplied terminal atlas: copper/wood body, grey steel, green CRT.
CASING = (46, 52, 58, 255)
CASING_D = (30, 35, 40, 255)
CASING_L = (70, 78, 86, 255)
CASING_X = (58, 66, 74, 255)
ACCENT = (70, 210, 120, 255)
SCREEN = (6, 14, 8, 255)
GREEN = (86, 255, 130, 255)
GREEN_DIM = (40, 150, 70, 255)
KEY = (60, 66, 74, 255)
WELL = (34, 40, 46, 255)

# Terminal-atlas palette (hand-drawn source), used for the linker so the two match.
T_COPPER = (104, 66, 30, 255)
T_COPPER_L = (198, 140, 74, 255)
T_COPPER_D = (72, 46, 20, 255)
T_SEAM = (34, 22, 12, 255)
T_STEEL = (66, 64, 70, 255)
T_STEEL_L = (104, 102, 110, 255)
T_STEEL_D = (40, 38, 42, 255)
T_CRT = (10, 26, 10, 255)
T_GREEN = (128, 236, 76, 255)
T_KEY = (182, 180, 188, 255)
T_ANTENNA = (188, 96, 44, 255)
T_ANTENNA_D = (120, 60, 28, 255)


def face(d, x, y, w, h, color):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def panel(d, x, y, w, h, color, edge_top=CASING_L, edge_bottom=CASING_D):
    face(d, x, y, w, h, color)
    d.line([x, y, x + w - 1, y], fill=edge_top)
    d.line([x, y + h - 1, x + w - 1, y + h - 1], fill=edge_bottom)


def chest_atlas():
    img = Image.new("RGBA", (64, 64), CASING_D)
    d = ImageDraw.Draw(img)

    # --- lid sides (east, north, west, south) at y=14 ---
    panel(d, 0, 14, 14, 5, CASING)
    panel(d, 14, 14, 14, 5, CASING)
    panel(d, 28, 14, 14, 5, CASING)
    panel(d, 42, 14, 14, 5, CASING)
    # vent slits on the two long faces
    for i in range(4):
        d.line([15 + i * 3, 15, 15 + i * 3, 17], fill=CASING_D)
        d.line([43 + i * 3, 15, 43 + i * 3, 17], fill=CASING_D)

    # --- lid underside: terminal screen (down face at u+dz) ---
    face(d, 14, 0, 14, 14, SCREEN)
    d.rectangle([14, 0, 27, 13], outline=(18, 28, 20, 255))
    for row in range(4):
        y = 3 + row * 2
        length = (10, 7, 9, 4)[row]
        d.line([16, y, 16 + length, y], fill=GREEN if row < 3 else GREEN_DIM)
    d.rectangle([16, 11, 17, 12], fill=GREEN)  # cursor

    # --- lid top (exterior casing, up face at u+dz+dx) ---
    panel(d, 28, 0, 14, 14, CASING_L)
    d.rectangle([29, 1, 40, 12], outline=CASING)
    d.rectangle([31, 3, 38, 10], outline=CASING_X)
    d.rectangle([35, 6, 37, 8], fill=ACCENT)

    # --- bottom sides at y=33 ---
    panel(d, 0, 33, 14, 10, CASING)
    panel(d, 14, 33, 14, 10, CASING)
    panel(d, 28, 33, 14, 10, CASING)
    panel(d, 42, 33, 14, 10, CASING)
    for x in range(2, 12, 3):
        d.line([x, 36, x, 40], fill=CASING_D)

    # --- bottom top face: keyboard (up face at u+dz+dx) ---
    face(d, 28, 19, 14, 14, WELL)
    for row in range(3):
        for col in range(5):
            x = 30 + col * 2
            y = 21 + row * 3
            d.rectangle([x, y, x + 1, y + 1], fill=KEY)
    d.rectangle([30, 30, 39, 31], fill=KEY)  # spacebar

    # --- bottom underside (down face, unseen) ---
    panel(d, 14, 19, 14, 14, CASING_D, edge_top=CASING_D, edge_bottom=CASING_D)

    # --- lock / latch ---
    face(d, 0, 0, 6, 5, ACCENT)
    d.rectangle([0, 0, 5, 4], outline=(30, 90, 50, 255))
    d.rectangle([2, 1, 3, 3], fill=(20, 60, 34, 255))

    return img


def linker_sprite():
    """A hand-held pocket unit matching the supplied terminal: copper case, steel band, green CRT."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # antenna, offset to the top-left so the silhouette stays readable at 16x16
    d.rectangle([4, 1, 5, 3], fill=T_ANTENNA_D)
    d.point((4, 0), fill=T_ANTENNA)
    d.point((5, 0), fill=T_ANTENNA)
    d.point((4, 1), fill=T_ANTENNA)

    # copper case with a darker seam edge
    panel(d, 2, 4, 12, 11, T_COPPER, edge_top=T_COPPER_L, edge_bottom=T_SEAM)

    # steel faceplate band around the screen
    panel(d, 3, 5, 10, 6, T_STEEL, edge_top=T_STEEL_L, edge_bottom=T_STEEL_D)

    # CRT screen with two readout rows and a cursor
    face(d, 4, 6, 8, 5, T_CRT)
    d.line([5, 7, 10, 7], fill=T_GREEN)
    d.line([5, 8, 8, 8], fill=T_GREEN)
    d.point((5, 9), fill=T_GREEN)
    d.rectangle([4, 6, 11, 10], outline=T_STEEL_D)

    # pale key row along the bottom of the case
    for col in range(4):
        d.rectangle([3 + col * 3, 12, 4 + col * 3, 13], fill=T_KEY)

    # green status lamp above the keys
    d.point((12, 5), fill=T_GREEN)

    return img


def main():
    ap = argparse.ArgumentParser(description="Generate Network Routing textures.")
    ap.add_argument("--chest", action="store_true",
                    help="also (re)write the terminal chest atlas; it is hand-supplied art, so this "
                         "is off by default to avoid clobbering it")
    args = ap.parse_args()

    linker_path = os.path.join(ASSETS, "item", "routing_linker.png")
    os.makedirs(os.path.dirname(linker_path), exist_ok=True)
    linker_sprite().save(linker_path)
    print("wrote", os.path.normpath(linker_path))

    if args.chest:
        entity_path = os.path.join(ASSETS, "entity", "chest", "network_routing.png")
        os.makedirs(os.path.dirname(entity_path), exist_ok=True)
        chest_atlas().save(entity_path)
        print("wrote", os.path.normpath(entity_path))
    else:
        print("skipped the chest atlas (hand-supplied); pass --chest to regenerate a placeholder")


if __name__ == "__main__":
    main()
