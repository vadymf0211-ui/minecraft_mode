#!/usr/bin/env python3
"""Design tool for the Gravity Gun pixel art.

Composes the 16x16 sprite from primitives, auto-outlines it, then dumps:
  - texturegen/palette.px      (palette: char -> RGBA hex)
  - texturegen/gravity_gun.px  (16 rows of palette chars — consumed by Gradle)
  - texturegen/preview.png     (x24 upscale for visual inspection)
The actual in-game PNGs are generated from the .px files by the Gradle task,
so the repository stays 100% text.
"""
from PIL import Image

S = 16
UP = 24  # preview upscale

PAL = {
    'K': (16, 16, 20, 255),     # outline
    'D': (51, 54, 63, 255),     # dark steel
    'G': (78, 85, 96, 255),     # steel
    'L': (123, 132, 148, 255),  # light steel
    'S': (168, 176, 191, 255),  # specular
    'O': (255, 142, 31, 255),   # orange
    'o': (194, 90, 10, 255),    # dark orange
    'Y': (255, 208, 138, 255),  # orange highlight
    'C': (191, 251, 255, 255),  # core bright
    'c': (63, 214, 236, 255),   # cyan
    'u': (26, 127, 158, 255),   # cyan dark
    'H': (63, 50, 48, 255),     # grip dark
    'h': (99, 80, 74, 255),     # grip light
    'B': (38, 40, 45, 255),     # belt rubber base
    'b': (58, 61, 68, 255),     # belt seam
    'E': (23, 24, 28, 255),     # belt edge
    'P': (196, 154, 108, 255),  # cardboard base
    'p': (154, 116, 72, 255),   # cardboard dark
    'q': (219, 185, 138, 255),  # cardboard light
    'w': (138, 111, 82, 255),   # wet cardboard base
    'W': (105, 84, 64, 255),    # wet cardboard dark
    'T': (222, 200, 130, 255),  # tape
    't': (176, 154, 86, 255),   # tape dark
    '0': (0, 0, 0, 255),        # gui outline
    '1': (255, 255, 255, 255),  # gui bevel light
    '2': (198, 198, 198, 255),  # gui panel
    '3': (139, 139, 139, 255),  # gui slot fill
    '4': (85, 85, 85, 255),     # gui bevel shade
    '5': (55, 55, 55, 255),     # gui slot border dark
    '.': (0, 0, 0, 0),
}
CHAR_OF = {v: k for k, v in PAL.items()}

grid = [['.' for _ in range(S)] for _ in range(S)]

def put(x, y, ch):
    if 0 <= x < S and 0 <= y < S:
        grid[y][x] = ch

def band(x0, y0, n, widths, colors):
    """Diagonal band going up-right; widths offset along (+1,+1)."""
    for t in range(n):
        for w, ch in zip(widths, colors):
            put(x0 + t + w, y0 - t + w, ch)

# --- grip (bottom-left) ---
band(2, 13, 4, (0, 1), ('h', 'H'))

# --- body: chunky diagonal block ---
band(5, 10, 5, (-1, 0, 1, 2), ('L', 'G', 'G', 'D'))
put(5, 9, 'S')          # spec glint on top edge
put(6, 8, 'S')
put(7, 11, 'D')         # underside step
band(6, 11, 2, (0,), ('D',))  # trigger-ish shadow under body

# --- fill checkerboard holes left by the diagonal bands (grip + body only) ---
for _ in range(2):
    for y in range(S):
        for x in range(S):
            if grid[y][x] == '.':
                nbrs = [grid[ny][nx] for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))
                        if 0 <= nx < S and 0 <= ny < S]
                solid = [ch for ch in nbrs if ch != '.']
                if len(nbrs) == 4 and len(solid) == 4:
                    grid[y][x] = max(set(solid), key=solid.count)

# --- energy core in the muzzle mouth ---
put(10, 4, 'c'); put(11, 4, 'C')
put(10, 5, 'C'); put(11, 5, 'c')
put(9, 5, 'u')          # core shadow against body plate
put(12, 3, 'C')         # forward spark

# --- upper claw ---
for x, y, ch in [(8, 5, 'o'), (9, 4, 'O'), (10, 3, 'O'), (11, 2, 'O'), (12, 2, 'O'), (13, 2, 'Y'), (13, 3, 'o')]:
    put(x, y, ch)

# --- lower claw ---
for x, y, ch in [(11, 8, 'o'), (12, 7, 'O'), (13, 6, 'O'), (14, 5, 'O'), (14, 4, 'Y'), (13, 4, 'o')]:
    put(x, y, ch)

# --- auto outline: any transparent neighbour of a colored pixel becomes K ---
colored = {(x, y) for y in range(S) for x in range(S) if grid[y][x] != '.'}
outline = set()
for (x, y) in colored:
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            nx, ny = x + dx, y + dy
            if 0 <= nx < S and 0 <= ny < S and grid[ny][nx] == '.':
                outline.add((nx, ny))
for (x, y) in outline:
    grid[y][x] = 'K'

# --- interior black (K fully surrounded by solid pixels) becomes dark steel ---
for y in range(S):
    for x in range(S):
        if grid[y][x] == 'K':
            nbrs = [grid[ny][nx] for nx in (x - 1, x, x + 1) for ny in (y - 1, y, y + 1)
                    if 0 <= nx < S and 0 <= ny < S and not (nx == x and ny == y)]
            if len(nbrs) == 8 and all(ch != '.' for ch in nbrs):
                grid[y][x] = 'D'

# keep the muzzle mouth open: clear outline pixels that would plug the very
# front opening between the claw tips (they should frame a dark gap, not seal it
# into a blob past the tips)
for (x, y) in [(15, 3), (15, 2), (14, 1), (13, 1), (12, 1), (15, 1)]:
    if grid[y][x] == 'K':
        grid[y][x] = '.'

# --- manual touch-ups: widen the core glow inside the claw ring ---
put(11, 3, 'c')
put(12, 4, 'u')

# --- dump .px files ---
with open('texturegen/palette.px', 'w') as f:
    f.write("# char  RRGGBBAA (AA optional, FF assumed); '.' = transparent\n")
    for ch, (r, g, b, a) in PAL.items():
        if ch == '.':
            f.write(". transparent\n")
        else:
            f.write(f"{ch} {r:02x}{g:02x}{b:02x}{a:02x}\n")

with open('texturegen/gravity_gun.px', 'w') as f:
    for row in grid:
        f.write(''.join(row) + '\n')

# --- render preview ---
img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
for y in range(S):
    for x in range(S):
        img.putpixel((x, y), PAL[grid[y][x]])

img.save('texturegen/gravity_gun_16.png')

# checkerboard background so transparency is visible
prev = Image.new('RGBA', (S * UP, S * UP), (0, 0, 0, 255))
for y in range(S * UP):
    for x in range(S * UP):
        c = (58, 58, 66, 255) if ((x // UP) + (y // UP)) % 2 == 0 else (44, 44, 52, 255)
        prev.putpixel((x, y), c)
big = img.resize((S * UP, S * UP), Image.NEAREST)
prev.alpha_composite(big)
prev.save('texturegen/preview.png')
print("wrote texturegen/{palette.px,gravity_gun.px,gravity_gun_16.png,preview.png}")
