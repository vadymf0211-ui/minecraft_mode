#!/usr/bin/env python3
"""Cardboard textures: sheet (item + carpet), wet sheet, box top/side/bottom.

Colors must exist in texturegen/palette.px (written by design.py).
"""
from PIL import Image

COLORS = {
    'P': (196, 154, 108, 255),  # cardboard base
    'p': (154, 116, 72, 255),   # cardboard dark
    'q': (219, 185, 138, 255),  # cardboard light
    'w': (138, 111, 82, 255),   # wet base
    'W': (105, 84, 64, 255),    # wet dark
    'T': (222, 200, 130, 255),  # tape
    't': (176, 154, 86, 255),   # tape dark
    'u': (26, 127, 158, 255),   # water drip
    '.': (0, 0, 0, 0),
}


def grid16(fill):
    return [[fill] * 16 for _ in range(16)]


def border(g, ch):
    for i in range(16):
        g[0][i] = ch
        g[15][i] = ch
        g[i][0] = ch
        g[i][15] = ch


def sheet(base, dark, light):
    g = grid16(base)
    border(g, dark)
    for x in range(1, 8):        # light catch, top-left
        g[1][x] = light
    for y in range(1, 6):
        g[y][1] = light
    for row in (5, 10):          # corrugation dashes
        for x in list(range(3, 7)) + list(range(9, 13)):
            g[row][x] = dark
    g[13][13] = light            # folded corner
    g[12][13] = dark
    g[13][12] = dark
    return g


cardboard = sheet('P', 'p', 'q')

wet = sheet('w', 'W', 'w')
for (x, y) in ((4, 15), (9, 14), (12, 15), (6, 8)):
    wet[y][x] = 'u'              # drips and a soggy spot

box_side = grid16('P')
border(box_side, 'p')
for x in range(3, 15, 3):        # vertical corrugation
    for y in range(1, 15):
        box_side[y][x] = 'q'
for k in range(3):               # printed "this way up" arrow
    box_side[4 + k][7 - k] = 'p'
    box_side[4 + k][8 + k] = 'p'
for y in range(5, 11):
    box_side[y][7] = 'p'
    box_side[y][8] = 'p'
for x in range(5, 11):           # arrow baseline
    box_side[12][x] = 'p'

box_top = grid16('P')
border(box_top, 'p')
for y in range(1, 15):           # flap seam + crease highlight
    box_top[y][7] = 'p'
    box_top[y][8] = 'q'
for y in (6, 7, 8, 9):           # tape band across the seam
    for x in range(16):
        box_top[y][x] = 'T'
for x in range(16):
    box_top[6][x] = 't'
    box_top[9][x] = 't'

box_bottom = grid16('P')
border(box_bottom, 'p')
for x in range(2, 14):           # thin tape strip
    box_bottom[7][x] = 'T'
    box_bottom[8][x] = 't'
box_bottom[2][2] = 'q'
box_bottom[13][13] = 'q'

SPRITES = {
    'cardboard.px': cardboard,
    'wet_cardboard.px': wet,
    'cardboard_box_side.px': box_side,
    'cardboard_box_top.px': box_top,
    'cardboard_box_bottom.px': box_bottom,
}

for name, g in SPRITES.items():
    with open(f'texturegen/{name}', 'w') as fh:
        fh.write('\n'.join(''.join(row) for row in g) + '\n')

# preview strip
UP = 20
tiles = list(SPRITES.values())
canvas = Image.new('RGBA', ((16 * UP + 8) * len(tiles) - 8, 16 * UP), (40, 40, 46, 255))
for i, g in enumerate(tiles):
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), COLORS[g[y][x]])
    canvas.paste(img.resize((16 * UP, 16 * UP), Image.NEAREST), (i * (16 * UP + 8), 0))
canvas.save('texturegen/preview_cardboard.png')
print('wrote cardboard sprites + preview_cardboard.png')
