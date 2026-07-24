#!/usr/bin/env python3
"""Conveyor belt textures.

Generates:
  - texturegen/conveyor_belt_top.px  — animated top: 4 frames of 16x16 stacked
    vertically (16x64). Orange chevrons scroll toward the belt's facing
    direction (north in model space); frametime is set in the .mcmeta file.
  - texturegen/conveyor_belt.px      — 16x16 metal frame used for the sides
    and the bottom (only the bottom 2 texture rows are visible on the sides).
  - texturegen/preview_conveyor.png  — upscaled preview of all frames + side.

Colors must exist in texturegen/palette.px (written by design.py).
"""
from PIL import Image

COLORS = {
    'K': (16, 16, 20, 255),
    'D': (51, 54, 63, 255),
    'G': (78, 85, 96, 255),
    'L': (123, 132, 148, 255),
    'O': (255, 142, 31, 255),
    'o': (194, 90, 10, 255),
    'Y': (255, 208, 138, 255),
    'B': (38, 40, 45, 255),
    'b': (58, 61, 68, 255),
    'E': (23, 24, 28, 255),
    '.': (0, 0, 0, 0),
}

W = H = 16
FRAMES = 4
PERIOD = 8   # chevron repeat, px
SHIFT = 2    # scroll per frame, px (FRAMES * SHIFT == PERIOD -> seamless loop)

# --- animated top: rubber belt with scrolling chevrons pointing north ---
top_rows = []
for f in range(FRAMES):
    for y in range(H):
        phase = (y + f * SHIFT) % PERIOD
        row = []
        for x in range(W):
            ch = 'B'
            if x in (0, 15):
                ch = 'E'          # rubber edge
            elif phase == 7:
                ch = 'b'          # belt segment seam
            if x not in (0, 15) and phase < 4 and x in (7 - phase, 8 + phase):
                ch = 'Y' if phase == 0 else 'O'   # chevron arm (bright tip)
            row.append(ch)
        top_rows.append(''.join(row))

# --- side/bottom: riveted metal frame; bottom 2 rows are the visible side ---
side_rows = []
for y in range(16):
    row = []
    for x in range(16):
        ch = 'G'
        if y in (0, 15) or x in (0, 15):
            ch = 'D'
        if (x, y) in ((2, 2), (13, 2), (2, 5), (13, 5)):
            ch = 'L'              # rivets (bottom face)
        if y == 8:
            ch = 'E'              # rubber belt wrapping over the edge
        if y == 9:
            ch = 'D'              # top rail
        if 10 <= y <= 13:
            ch = 'D' if x in (0, 15) else 'G'
            if y in (11, 12) and x % 4 in (1, 2):
                ch = 'O' if (y == 11 and x % 4 == 1) else 'o'  # rollers
        if y == 14:
            ch = 'D'              # bottom rail
        if y == 15:
            ch = 'K'              # ground shadow
        row.append(ch)
    side_rows.append(''.join(row))

with open('texturegen/conveyor_belt_top.px', 'w') as fh:
    fh.write('\n'.join(top_rows) + '\n')
with open('texturegen/conveyor_belt.px', 'w') as fh:
    fh.write('\n'.join(side_rows) + '\n')

# --- preview: 4 frames + side texture, upscaled ---
def render(rows):
    height, width = len(rows), len(rows[0])
    img = Image.new('RGBA', (width, height))
    for yy, r in enumerate(rows):
        for xx, c in enumerate(r):
            img.putpixel((xx, yy), COLORS[c])
    return img

UP = 20
top = render(top_rows)
tiles = [top.crop((0, 16 * i, 16, 16 * (i + 1))) for i in range(FRAMES)] + [render(side_rows)]
canvas = Image.new('RGBA', ((16 * UP + 8) * len(tiles) - 8, 16 * UP), (40, 40, 46, 255))
for i, tile in enumerate(tiles):
    canvas.paste(tile.resize((16 * UP, 16 * UP), Image.NEAREST), (i * (16 * UP + 8), 0))
canvas.save('texturegen/preview_conveyor.png')
print('wrote conveyor_belt_top.px (16x64), conveyor_belt.px, preview_conveyor.png')
