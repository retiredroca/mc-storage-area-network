"""Generates the Remote Access Terminal block texture (no external art required).

Run from anywhere:  python tools/generate_textures.py

Output (relative to this module):
  common/src/main/resources/assets/remote_access_terminal/textures/block/terminal.png

The texture is drawn white: the client colour providers tint it per dye (each block and block item
carries its own ARGB tint), so one 16x16 sprite serves all sixteen terminals. Black marks the 2px
corner strips (4px arms wrapping each corner) and the solid 4x4 centre pad.
"""

import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets",
                      "remote_access_terminal", "textures", "block")

WHITE = (255, 255, 255, 255)
BLACK = (0, 0, 0, 255)

# '#' = black, '.' = white. The 2px edge strips have 4px arms wrapping each corner, and the
# centre carries a solid 4x4 pad.
GRID = (
    "####........####",
    "####........####",
    "##............##",
    "##............##",
    "................",
    "................",
    "##....####....##",
    "##....####....##",
    "##....####....##",
    "##....####....##",
    "................",
    "................",
    "##............##",
    "##............##",
    "####........####",
    "####........####",
)


def terminal_texture():
    img = Image.new("RGBA", (16, 16), WHITE)
    d = ImageDraw.Draw(img)
    for y, row in enumerate(GRID):
        for x, cell in enumerate(row):
            if cell == "#":
                d.point((x, y), fill=BLACK)
    return img


def main():
    path = os.path.join(ASSETS, "terminal.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    terminal_texture().save(path)
    print("wrote", os.path.normpath(path))
    for row in GRID:
        print(row)


if __name__ == "__main__":
    main()
