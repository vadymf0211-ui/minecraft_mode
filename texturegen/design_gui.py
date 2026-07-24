#!/usr/bin/env python3
"""Container GUI texture for the cardboard box (14 slots: 2 rows x 7).

The canvas is 256x256 (vanilla drawTexture assumes a 256x256 sheet); the panel
itself is 176x150 in the top-left corner. Slot cells line up exactly with the
slot coordinates in CardboardBoxScreenHandler.
"""
from PIL import Image

COLORS = {
    '0': (0, 0, 0, 255),        # outline
    '1': (255, 255, 255, 255),  # bevel light
    '2': (198, 198, 198, 255),  # panel
    '3': (139, 139, 139, 255),  # slot fill
    '4': (85, 85, 85, 255),     # bevel shade
    '5': (55, 55, 55, 255),     # slot border dark
    '.': (0, 0, 0, 0),
}

W = H = 256
PW, PH = 176, 150
g = [['.'] * W for _ in range(H)]

# panel base
for y in range(PH):
    for x in range(PW):
        g[y][x] = '2'
# bevel: light top/left, shade bottom/right
for x in range(PW):
    g[1][x] = '1'
    g[2][x] = '1'
    g[PH - 3][x] = '4'
    g[PH - 2][x] = '4'
for y in range(PH):
    g[y][1] = '1'
    g[y][2] = '1'
    g[y][PW - 3] = '4'
    g[y][PW - 2] = '4'
# interior back to panel colour
for y in range(3, PH - 3):
    for x in range(3, PW - 3):
        g[y][x] = '2'
# outer outline with rounded corners
for x in range(PW):
    g[0][x] = '0'
    g[PH - 1][x] = '0'
for y in range(PH):
    g[y][0] = '0'
    g[y][PW - 1] = '0'
for (x, y) in ((0, 0), (1, 0), (0, 1), (PW - 1, 0), (PW - 2, 0), (PW - 1, 1),
               (0, PH - 1), (1, PH - 1), (0, PH - 2),
               (PW - 1, PH - 1), (PW - 2, PH - 1), (PW - 1, PH - 2)):
    g[y][x] = '.'


def slot(sx, sy):
    """sx, sy = top-left of the inner 16x16 item cell (handler slot coords)."""
    for x in range(sx - 1, sx + 17):
        g[sy - 1][x] = '5'
        g[sy + 16][x] = '1'
    for y in range(sy - 1, sy + 17):
        g[y][sx - 1] = '5'
        g[y][sx + 16] = '1'
    g[sy - 1][sx + 16] = '3'
    g[sy + 16][sx - 1] = '3'
    for y in range(sy, sy + 16):
        for x in range(sx, sx + 16):
            g[y][x] = '3'


for row in range(2):            # box slots, 2 x 7
    for col in range(7):
        slot(25 + col * 18, 18 + row * 18)
for row in range(3):            # player inventory
    for col in range(9):
        slot(8 + col * 18, 67 + row * 18)
for col in range(9):            # hotbar
    slot(8 + col * 18, 125)

with open('texturegen/cardboard_gui.px', 'w') as fh:
    fh.write('\n'.join(''.join(row) for row in g) + '\n')

img = Image.new('RGBA', (W, H))
for y in range(H):
    for x in range(W):
        img.putpixel((x, y), COLORS[g[y][x]])
img.crop((0, 0, PW, PH)).resize((PW * 2, PH * 2), Image.NEAREST).save('texturegen/preview_gui.png')
print('wrote cardboard_gui.px + preview_gui.png')
