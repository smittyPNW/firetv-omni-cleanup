#!/usr/bin/env python3
"""Put your own photos into the My Home launcher: home screen backgrounds and the idle slideshow.

Every landscape photo in FOLDER becomes a slideshow image (saver_01.jpg, saver_02.jpg, ...), center-cropped
to 16:9 and scaled to 1920x1080. --backgrounds picks four of them for the time-of-day home screen.
Rebuild and reinstall the launcher afterwards.

    python3 tools/prep_photos.py ~/Pictures/tv --backgrounds dawn.jpg rainbow.jpg sunset.jpg milkyway.jpg
    python3 tools/prep_photos.py --placeholders      # the plain gradients this repo ships with

Needs Pillow (pip install pillow).
"""
import argparse
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageOps

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "launcher", "app", "src", "main", "res",
                   "drawable-nodpi")
SIZE = (1920, 1080)
MIN_WIDTH = 1600   # smaller photos look soft on a TV
PARTS = ("morning", "afternoon", "evening", "night")


def fit(im):
    """Center-crop to 16:9, then scale to 1920x1080."""
    im = ImageOps.exif_transpose(im).convert("RGB")
    w, h = im.size
    if w / h > 16 / 9:
        cw = round(h * 16 / 9)
        im = im.crop(((w - cw) // 2, 0, (w - cw) // 2 + cw, h))
    else:
        ch = round(w * 9 / 16)
        im = im.crop((0, (h - ch) // 2, w, (h - ch) // 2 + ch))
    return im.resize(SIZE, Image.LANCZOS)


def save(im, name):
    im.save(os.path.join(OUT, name + ".jpg"), quality=86, optimize=True)
    print(name + ".jpg")


def clear_savers():
    for f in os.listdir(OUT):
        if f.startswith("saver_") and f.endswith(".jpg"):
            os.remove(os.path.join(OUT, f))


def gradient(top, bottom, glow):
    """A dark sky gradient with a soft glow on the horizon."""
    im = Image.new("RGB", SIZE)
    d = ImageDraw.Draw(im)
    for y in range(SIZE[1]):
        t = y / (SIZE[1] - 1)
        d.line([(0, y), (SIZE[0], y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    halo = Image.new("RGB", SIZE, glow)
    mask = Image.new("L", SIZE, 0)
    ImageDraw.Draw(mask).ellipse((-400, 700, SIZE[0] + 400, 1500), fill=90)
    return Image.composite(halo, im, mask.filter(ImageFilter.GaussianBlur(160)))


def placeholders():
    skies = {"morning": ((40, 52, 92), (12, 14, 24), (214, 150, 90)),
             "afternoon": ((54, 86, 128), (16, 22, 34), (190, 170, 130)),
             "evening": ((70, 40, 84), (14, 10, 22), (230, 110, 90)),
             "night": ((10, 14, 30), (2, 3, 8), (60, 80, 140))}
    clear_savers()
    for i, part in enumerate(PARTS, 1):
        sky = gradient(*skies[part])
        save(sky, "bg_" + part)
        save(sky, "saver_%02d" % i)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("folder", nargs="?", help="folder of your photos (.jpg, .jpeg, .png)")
    ap.add_argument("--backgrounds", nargs=4, metavar=PARTS, help="file names in FOLDER for each part of the day")
    ap.add_argument("--placeholders", action="store_true", help="write the plain gradient placeholders")
    args = ap.parse_args()
    if args.placeholders:
        placeholders()
        return
    if not args.folder:
        ap.error("give a photo folder, or --placeholders")

    photos = sorted(f for f in os.listdir(args.folder) if f.lower().endswith((".jpg", ".jpeg", ".png")))
    clear_savers()
    n = 0
    for f in photos:
        with Image.open(os.path.join(args.folder, f)) as im:
            w, h = ImageOps.exif_transpose(im).size
            if h > w or w < MIN_WIDTH:
                print("skipped %s (%dx%d: portrait or under %d px wide)" % (f, w, h, MIN_WIDTH), file=sys.stderr)
                continue
            n += 1
            save(fit(im), "saver_%02d" % n)
    if args.backgrounds:
        for part, f in zip(PARTS, args.backgrounds):
            with Image.open(os.path.join(args.folder, f)) as im:
                save(fit(im), "bg_" + part)
    print("%d slideshow photos. Rebuild and reinstall the launcher." % n)


if __name__ == "__main__":
    main()
