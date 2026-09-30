#!/usr/bin/env python3
"""Generate compact, title-free vector cover art for the bundled classics.

Run from any directory. Requires rsvg-convert; Pillow is used when available to
reduce the PNG size. Artwork is deterministic, so the source remains reproducible.
"""

import hashlib
import json
import math
import random
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
COVERS = ASSETS / "covers"
CATALOGS = [ASSETS / "bookInfo-simplified.json", ASSETS / "bookInfo-traditional.json"]

# Existing illustrated covers were checked at full size and have no lettering.
KEEP = {
    "三国演义", "吕氏春秋", "唐诗三百首", "大唐西域记", "文子",
    "文心雕龙", "楚辞", "水浒传", "红楼梦", "西游记", "诗经",
}

PALETTES = [
    ("#e8dbc2", "#26494a", "#b05e3f", "#dba872"),
    ("#d8e1da", "#213d50", "#9b6651", "#c9a96c"),
    ("#efe0d5", "#4c3c52", "#a85a57", "#d6a989"),
    ("#e1e0d2", "#354756", "#8a6856", "#c8a15e"),
    ("#d9e2df", "#234d54", "#a45c45", "#d8bb78"),
    ("#e9dbce", "#364b40", "#9b6552", "#c6a26f"),
    ("#d8dbd8", "#343b55", "#a85e54", "#d9b886"),
    ("#e6dfd1", "#2a4651", "#92664d", "#b8a374"),
]


def circle(x, y, radius, fill, opacity=1, stroke="none", width=1):
    return (f'<circle cx="{x}" cy="{y}" r="{radius}" fill="{fill}" '
            f'fill-opacity="{opacity}" stroke="{stroke}" stroke-width="{width}"/>')


def path(d, fill="none", stroke="none", width=1, opacity=1):
    return (f'<path d="{d}" fill="{fill}" stroke="{stroke}" '
            f'stroke-width="{width}" stroke-linecap="round" '
            f'stroke-linejoin="round" opacity="{opacity}"/>')


def motif(kind, ink, red, gold, rng):
    parts = []
    if kind == 0:  # mountain ridges
        parts += [circle(274, 245, 75, gold, .82)]
        for y, color in [(425, ink), (470, red), (515, ink)]:
            peaks = [(0, y), (70, y-75), (132, y-28), (210, y-145),
                     (297, y-58), (395, y-129), (395, 630), (0, 630)]
            parts.append(path("M" + " L".join(f"{x} {v}" for x,v in peaks) + " Z",
                              color, opacity=.85 if y == 425 else .55))
    elif kind == 1:  # bamboo
        for x, slant in [(109, -17), (185, 16), (274, -11)]:
            parts.append(path(f"M{x} 513 Q{x+slant} 326 {x+slant} 152", stroke=ink, width=6))
            for y in (218, 282, 355, 431):
                xx = x + slant * (513-y)/361
                parts.append(path(f"M{xx-9:.1f} {y} L{xx+9:.1f} {y}", stroke=ink, width=3))
                side = -1 if (x+y) % 2 else 1
                parts.append(path(f"M{xx:.1f} {y} Q{xx+side*58:.1f} {y-61} "
                                  f"{xx+side*80:.1f} {y-41} Q{xx+side*45:.1f} {y-6} "
                                  f"{xx:.1f} {y} Z", red if y == 282 else ink, opacity=.75))
    elif kind == 2:  # lotus
        parts.append(circle(198, 342, 137, gold, .24))
        for angle in range(0, 360, 30):
            parts.append(f'<ellipse cx="198" cy="293" rx="30" ry="92" '
                         f'fill="{red if angle % 60 else gold}" fill-opacity=".75" '
                         f'transform="rotate({angle} 198 363)"/>')
        parts += [circle(198, 363, 33, ink, .9),
                  path("M75 465 Q198 493 320 465", stroke=ink, width=5)]
    elif kind == 3:  # fan and ribs
        parts.append(path("M54 420 Q90 220 198 178 Q306 220 342 420 Q198 368 54 420 Z", gold))
        parts.append(path("M54 420 Q198 355 342 420", stroke=ink, width=6))
        for x in range(54, 343, 36):
            parts.append(path(f"M198 480 L{x} {420-120*math.sin(math.pi*(x-54)/288):.0f}",
                              stroke=ink, width=2, opacity=.7))
        parts.append(circle(198, 478, 12, red))
    elif kind == 4:  # night sky and orbit
        parts.append(circle(199, 321, 110, gold, .85))
        parts.append(circle(235, 289, 108, "#ffffff", .52))
        for radius in (145, 182):
            parts.append(circle(198, 326, radius, "none", 1, ink, 2))
        for _ in range(28):
            x, y = rng.randrange(34, 365), rng.randrange(128, 520)
            parts.append(circle(x, y, rng.choice((1,2,3)), ink, .7))
    elif kind == 5:  # ceramic vessel and leaves
        parts.append(path("M151 222 L245 222 L245 249 Q265 273 270 334 "
                          "L258 453 Q198 482 137 453 L125 334 Q131 273 151 249 Z", ink))
        parts.append(path("M154 252 Q198 268 242 252", stroke=gold, width=3))
        parts.append(path("M141 400 Q198 418 255 400", stroke=gold, width=3))
        for a in range(-3,4):
            x = 198 + a*35
            parts.append(path(f"M198 225 Q{x} 165 {x+22} 112", stroke=red, width=3))
            parts.append(f'<ellipse cx="{x+13}" cy="{151+abs(a)*12}" rx="12" ry="27" '
                         f'fill="{red}" transform="rotate({a*15} {x+13} {151+abs(a)*12})"/>')
    elif kind == 6:  # rolling waves
        parts.append(circle(277, 236, 74, gold, .88))
        for row in range(7):
            y = 268 + row*41
            for col in range(-1,5):
                x = col*112 + (row%2)*55
                parts.append(path(f"M{x} {y} Q{x+28} {y-35} {x+56} {y} "
                                  f"Q{x+84} {y+35} {x+112} {y}",
                                  stroke=ink if row%2 else red, width=4, opacity=.8))
    elif kind == 7:  # pagoda silhouette
        parts.append(circle(267, 251, 77, gold, .8))
        for tier, y in enumerate((276, 333, 390, 447)):
            inset = tier*12
            parts.append(path(f"M{65+inset} {y+9} Q198 {y+32} {330-inset} {y+9} "
                              f"L{294-inset} {y-11} L{101+inset} {y-11} Z", ink))
            parts.append(path(f"M{126+inset} {y+11} L{126+inset} {y+49} "
                              f"L{269-inset} {y+49} L{269-inset} {y+11} Z", red, opacity=.7))
        parts.append(path("M198 214 L198 267", stroke=ink, width=5))
    elif kind == 8:  # blade and banners
        parts.append(path("M198 140 L218 376 L198 409 L178 376 Z", gold, ink, 4))
        parts.append(path("M151 386 Q198 415 245 386", stroke=ink, width=11))
        parts.append(path("M198 406 L198 492", stroke=ink, width=10))
        parts.append(circle(198, 499, 13, red))
        for side in (-1,1):
            x = 198 + side*80
            parts.append(path(f"M{x} 178 L{x} 482", stroke=ink, width=3))
            parts.append(path(f"M{x} 188 Q{x+side*39} 198 {x+side*62} 223 "
                              f"L{x} 257 Z", red, opacity=.75))
    elif kind == 9:  # flowering branches
        for side in (-1,1):
            start = 20 if side==1 else 375
            parts.append(path(f"M{start} 485 Q{198-side*40} 366 198 181", stroke=ink, width=7))
            for j in range(5):
                x = start + side*(38+j*25)
                y = 440-j*49
                parts.append(path(f"M{x} {y} Q{x+side*37} {y-30} {x+side*50} {y-59}",
                                  stroke=ink, width=3))
                for k in range(5):
                    a = k*math.tau/5
                    xx=x+side*49+math.cos(a)*13
                    yy=y-59+math.sin(a)*13
                    parts.append(circle(f"{xx:.1f}", f"{yy:.1f}", 9, red, .82))
                parts.append(circle(x+side*49, y-59, 6, gold))
    else:  # interlocking rings and seal-like geometry
        for radius in (125, 97, 67):
            parts.append(circle(198, 317, radius, "none", 1, ink, 3))
        for angle in range(0,360,45):
            x=198+125*math.cos(math.radians(angle))
            y=317+125*math.sin(math.radians(angle))
            parts.append(circle(f"{x:.1f}",f"{y:.1f}",9,red))
        parts.append(path("M198 260 L251 317 L198 374 L145 317 Z", gold, ink, 3))
    return "".join(parts)


def svg_for(work_id):
    seed = int.from_bytes(hashlib.sha256(work_id.encode()).digest()[:8], "big")
    rng = random.Random(seed)
    bg, ink, red, gold = PALETTES[seed % len(PALETTES)]
    # Favor imagery that fits the work while keeping every composition text free.
    if any(word in work_id for word in ("兵法", "尉缭", "商君", "东周")):
        kind = 8
    elif any(word in work_id for word in ("山海", "西游", "梦溪")):
        kind = 0
    elif any(word in work_id for word in ("洛神", "桃花", "镜花", "浮生")):
        kind = 9
    elif any(word in work_id for word in ("道德", "周易", "关尹", "列子")):
        kind = 4
    else:
        kind = seed % 11
    paper_flecks = "".join(
        circle(rng.randrange(25,370), rng.randrange(28,600),
               rng.choice((.5, .7, 1)), ink, .13)
        for _ in range(145)
    )
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="395" height="630" '
            f'viewBox="0 0 395 630"><rect width="395" height="630" fill="{bg}"/>'
            f'{paper_flecks}<rect x="16" y="16" width="363" height="598" '
            f'fill="none" stroke="{ink}" stroke-width="2" opacity=".5"/>'
            f'<rect x="23" y="23" width="349" height="584" fill="none" '
            f'stroke="{gold}" stroke-width="1" opacity=".75"/>'
            f'{motif(kind, ink, red, gold, rng)}'
            f'<path d="M55 546 L340 546" stroke="{ink}" stroke-width="2" opacity=".4"/>'
            f'<circle cx="198" cy="573" r="7" fill="{red}" opacity=".75"/></svg>')


def main():
    catalogs = [json.loads(path.read_text(encoding="utf-8")) for path in CATALOGS]
    generated = set()
    for index, book in enumerate(catalogs[0]):
        work_id = book["id"]
        if work_id in KEEP:
            target = book["bookCover"]
        else:
            target = "art_" + work_id
            if target not in generated:
                svg = svg_for(work_id)
                assert "<text" not in svg
                output = COVERS / f"{target}.png"
                subprocess.run(["rsvg-convert", "--format", "png", "--output",
                                str(output)], input=svg.encode(), check=True)
                try:
                    from PIL import Image
                    with Image.open(output) as image:
                        image.convert("RGB").quantize(colors=128).save(output, optimize=True)
                except ImportError:
                    pass
                generated.add(target)
        for catalog in catalogs:
            assert catalog[index]["id"] == work_id
            catalog[index]["bookCover"] = target

    for path, catalog in zip(CATALOGS, catalogs):
        path.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n",
                        encoding="utf-8")
    retained = {book["bookCover"] for catalog in catalogs for book in catalog}
    for cover in COVERS.glob("*.png"):
        if cover.stem not in retained:
            cover.unlink()
    print(f"Generated {len(generated)} covers; {len(retained)} unique covers in catalog")


if __name__ == "__main__":
    main()
