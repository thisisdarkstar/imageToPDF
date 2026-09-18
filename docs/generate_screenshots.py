"""
Generate Play Store screenshots (1080x1920) for ImageToPdf Free
Renders 5 polished mock screens:
  1. Splash / Welcome
  2. Home screen – image grid
  3. PDF Config dialog
  4. B&W Filter applied
  5. History / My Documents
"""

import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ─── Palette (matches app theme) ───────────────────────────────────────────────
BG_DARK       = (13,  17,  35)
SURFACE       = (22,  28,  50)
SURFACE_2     = (30,  38,  65)
PRIMARY       = (99, 102, 241)   # Indigo-500
PRIMARY_DARK  = (67,  56, 202)   # Indigo-700
ACCENT        = (6,  182, 212)   # Cyan-500
RED           = (220,  38,  38)
SUCCESS       = (16, 185, 129)
TEXT_PRIMARY  = (248, 250, 252)
TEXT_SEC      = (148, 163, 184)
TEXT_DIM      = ( 71,  85, 105)
DIVIDER       = ( 30,  41,  59)
WHITE         = (255, 255, 255)
W, H          = 1080, 1920

FONT_DIR = os.path.dirname(os.path.abspath(__file__))


def load(name, size):
    for path in [f"C:/Windows/Fonts/{name}", f"/usr/share/fonts/truetype/liberation/{name}", name]:
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()


def bold(size):  return load("arialbd.ttf", size)
def regular(size): return load("arial.ttf", size)


def new_canvas():
    img = Image.new("RGBA", (W, H), BG_DARK)
    draw = ImageDraw.Draw(img)
    # Subtle radial glow top-right
    glow = Image.new("RGBA", (W, H), (0,0,0,0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([500, -200, 1280, 700], fill=(*PRIMARY, 22))
    gd.ellipse([700, 100, 1300, 800], fill=(*ACCENT, 12))
    glow = glow.filter(ImageFilter.GaussianBlur(120))
    img = Image.alpha_composite(img, glow)
    return img, ImageDraw.Draw(img)


def status_bar(draw, y=0):
    """Mock Android status bar."""
    draw.rectangle([0, y, W, y+80], fill=(*BG_DARK, 220))
    draw.text((52, y+22), "9:41", fill=TEXT_SEC, font=bold(28))
    # Battery & signal icons (simplified)
    for i in range(4):
        draw.rectangle([W-170+i*22, y+26, W-170+i*22+14, y+54-(i*6)], fill=TEXT_SEC)
    draw.rounded_rectangle([W-80, y+24, W-44, y+56], radius=4, outline=TEXT_SEC, width=2)
    draw.rectangle([W-78, y+28, W-58, y+52], fill=TEXT_SEC)
    draw.rectangle([W-44, y+34, W-40, y+46], fill=TEXT_SEC)


def top_bar(draw, title, subtitle=None, y=80):
    """App top bar."""
    bar_h = 120 if subtitle else 100
    draw.rectangle([0, y, W, y+bar_h], fill=SURFACE)
    draw.line([0, y+bar_h, W, y+bar_h], fill=DIVIDER, width=2)
    ty = y + (bar_h - 44) // 2 - (20 if subtitle else 0)
    draw.text((52, ty), title, fill=TEXT_PRIMARY, font=bold(40))
    if subtitle:
        draw.text((52, ty+50), subtitle, fill=TEXT_SEC, font=regular(26))
    return y + bar_h


def bottom_nav(img, draw, active=0):
    """Bottom navigation bar."""
    nav_y = H - 160
    draw.rectangle([0, nav_y, W, H], fill=SURFACE)
    draw.line([0, nav_y, W, nav_y], fill=DIVIDER, width=2)
    tabs = [("🏠", "Home"), ("📄", "History")]
    tw = W // len(tabs)
    for i, (icon, label) in enumerate(tabs):
        cx = tw * i + tw // 2
        color = PRIMARY if i == active else TEXT_DIM
        draw.text((cx - 22, nav_y + 16), icon, fill=color, font=bold(40))
        draw.text((cx - len(label)*8, nav_y + 68), label, fill=color, font=regular(26))
    return nav_y


def fab(draw, y_bottom):
    """Floating action button."""
    cx, cy = W - 100, y_bottom - 100
    r = 60
    # Shadow
    shadow = Image.new("RGBA", (r*4, r*4), (0,0,0,0))
    sd = ImageDraw.Draw(shadow)
    sd.ellipse([r//2, r//2, r*4-r//2, r*4-r//2], fill=(*PRIMARY_DARK, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    # draw.bitmap((cx-r*2+14, cy-r*2+14), shadow)  # skip for simplicity
    draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=PRIMARY)
    draw.text((cx-20, cy-22), "+", fill=WHITE, font=bold(52))


def card(draw, x, y, w, h, radius=24, fill=SURFACE_2, border=DIVIDER):
    draw.rounded_rectangle([x, y, x+w, y+h], radius=radius, fill=fill, outline=border, width=2)


def pill(draw, x, y, text, color, font_obj):
    tw = len(text) * 11 + 28
    draw.rounded_rectangle([x, y, x+tw, y+36], radius=10,
                            fill=(*color, 30), outline=color, width=1)
    draw.text((x+14, y+8), text, fill=color, font=font_obj)
    return tw


# ─── Screen 1: Splash / Welcome ────────────────────────────────────────────────
def screen_splash(icon_path, out_path):
    img, draw = new_canvas()

    # Central logo
    if os.path.exists(icon_path):
        icon = Image.open(icon_path).resize((280, 280), Image.Resampling.LANCZOS)
        ix, iy = (W-280)//2, 580
        # Glow behind icon
        glow = Image.new("RGBA", (W, H), (0,0,0,0))
        gd = ImageDraw.Draw(glow)
        gd.ellipse([ix-80, iy-80, ix+360, iy+360], fill=(*PRIMARY, 35))
        glow = glow.filter(ImageFilter.GaussianBlur(60))
        img = Image.alpha_composite(img, glow)
        draw = ImageDraw.Draw(img)
        img.paste(icon, (ix, iy), icon)

    # App name
    name = "ImageToPdf Free"
    nf = bold(72)
    bb = draw.textbbox((0,0), name, font=nf)
    nw = bb[2]-bb[0]
    draw.text(((W-nw)//2, 910), name, fill=TEXT_PRIMARY, font=nf)

    # Tagline
    tag = "Convert • Enhance • Share"
    tf = regular(34)
    bb2 = draw.textbbox((0,0), tag, font=tf)
    tw = bb2[2]-bb2[0]
    draw.text(((W-tw)//2, 1002), tag, fill=TEXT_SEC, font=tf)

    # Feature pills
    pf = bold(24)
    pills_data = [("100% OFFLINE", SUCCESS), ("PRIVATE", ACCENT), ("FREE", PRIMARY)]
    total = sum(len(t)*11+28+18 for t,_ in pills_data) - 18
    px = (W - total) // 2
    py = 1090
    for label, color in pills_data:
        pw = pill(draw, px, py, label, color, pf)
        px += pw + 18

    # Version
    draw.text(((W-120)//2, H-200), "v1.0.0", fill=TEXT_DIM, font=regular(28))

    img.save(out_path, "PNG")
    print(f"  ✓ Splash: {out_path}")


# ─── Screen 2: Home – Image Grid ───────────────────────────────────────────────
def screen_home(out_path):
    img, draw = new_canvas()
    status_bar(draw)
    content_y = top_bar(draw, "ImageToPdf Free", "Select images to convert", y=80)

    # Info chip
    draw.rounded_rectangle([52, content_y+24, 480, content_y+70], radius=16,
                            fill=(*ACCENT, 18), outline=(*ACCENT, 80), width=1)
    draw.text((72, content_y+36), "📸  Tap images to select", fill=ACCENT, font=regular(26))

    # Image grid (mock thumbnails)
    gw, gh = 318, 318
    gap = 24
    grid_x, grid_y = 52, content_y + 110
    colors = [
        [(99,102,241),(67,56,202)],   # Indigo gradient
        [(6,182,212),(4,120,140)],    # Cyan
        [(16,185,129),(4,120,87)],    # Green
        [(245,158,11),(180,105,5)],   # Amber
        [(220,38,38),(153,27,27)],    # Red
        [(139,92,246),(91,33,182)],   # Purple
    ]
    labels = ["vacation_2024.jpg","document_scan.jpg","receipt_001.jpg",
              "photo_001.jpg","contract.jpg","receipt_002.jpg"]

    for i in range(6):
        col = i % 3
        row = i // 3
        x = grid_x + col * (gw + gap)
        y = grid_y + row * (gh + gap)
        c1, c2 = colors[i]
        # gradient mock image
        for py2 in range(gh):
            t = py2 / gh
            r2 = int(c1[0] + t*(c2[0]-c1[0]))
            g2 = int(c1[1] + t*(c2[1]-c1[1]))
            b2 = int(c1[2] + t*(c2[2]-c1[2]))
            draw.line([(x, y+py2), (x+gw, y+py2)], fill=(r2,g2,b2))
        # Rounded mask effect (just draw rounded rect on top with BG color)
        # Draw filename chip
        draw.rectangle([x, y+gh-52, x+gw, y+gh], fill=(0,0,0,90))
        draw.text((x+10, y+gh-42), labels[i][:18], fill=WHITE, font=regular(22))
        # Selection circle (show 2 as selected)
        if i in [0, 2]:
            draw.ellipse([x+gw-50, y+10, x+gw-10, y+50], fill=PRIMARY)
            draw.text((x+gw-40, y+14), "✓", fill=WHITE, font=bold(28))
        else:
            draw.ellipse([x+gw-50, y+10, x+gw-10, y+50], outline=WHITE, width=2, fill=(255,255,255,30))

    nav_y = bottom_nav(img, draw, active=0)
    fab(draw, nav_y)

    # Selection bar at bottom
    sel_y = nav_y - 100
    draw.rectangle([0, sel_y, W, nav_y], fill=(*PRIMARY_DARK, 230))
    draw.text((52, sel_y+28), "2 images selected", fill=WHITE, font=bold(30))
    draw.rounded_rectangle([W-280, sel_y+18, W-52, sel_y+78], radius=16, fill=WHITE)
    draw.text((W-248, sel_y+30), "Convert →", fill=PRIMARY_DARK, font=bold(30))

    img.save(out_path, "PNG")
    print(f"  ✓ Home Grid: {out_path}")


# ─── Screen 3: PDF Config Dialog ───────────────────────────────────────────────
def screen_config(out_path):
    img, draw = new_canvas()
    status_bar(draw)
    top_bar(draw, "ImageToPdf Free", y=80)

    # Dim overlay
    overlay = Image.new("RGBA", (W, H), (0,0,0,150))
    img = Image.alpha_composite(img, overlay)
    draw = ImageDraw.Draw(img)

    # Dialog card
    dx, dy, dw, dh = 52, 320, W-104, 1260
    draw.rounded_rectangle([dx, dy, dx+dw, dy+dh], radius=32, fill=SURFACE, outline=DIVIDER, width=2)

    # Header
    draw.text((dx+52, dy+52), "PDF Settings", fill=TEXT_PRIMARY, font=bold(44))
    draw.line([dx+40, dy+118, dx+dw-40, dy+118], fill=DIVIDER, width=2)

    # Sections
    def section(title, yy):
        draw.text((dx+52, yy), title, fill=TEXT_SEC, font=bold(26))
        return yy+44

    def row_text(label, value, yy):
        card(draw, dx+40, yy, dw-80, 90, radius=16, fill=(*WHITE,6))
        draw.text((dx+80, yy+22), label, fill=TEXT_PRIMARY, font=regular(30))
        draw.text((dx+dw-180, yy+22), value, fill=ACCENT, font=bold(30))
        return yy+106

    def row_toggle(label, on, yy):
        card(draw, dx+40, yy, dw-80, 90, radius=16, fill=(*WHITE,6))
        draw.text((dx+80, yy+22), label, fill=TEXT_PRIMARY, font=regular(30))
        color = SUCCESS if on else TEXT_DIM
        bx = dx+dw-180
        draw.rounded_rectangle([bx, yy+22, bx+100, yy+66], radius=22, fill=color)
        cx = bx+60 if on else bx+14
        draw.ellipse([cx, yy+28, cx+34, yy+62], fill=WHITE)
        return yy+106

    cy = dy + 140
    cy = section("Page Size", cy)
    cy = row_text("Size", "A4", cy)

    cy = section("Quality", cy)
    cy = row_text("Image Quality", "90%", cy)
    cy = row_text("Compression", "Medium", cy)

    cy = section("Enhancement", cy)
    cy = row_toggle("B&W Enhancement", True, cy)
    cy = row_toggle("Auto Brightness", False, cy)

    cy = section("Output", cy)
    cy = row_text("Filename", "document_001", cy)

    # Action buttons
    btn_y = dy + dh - 160
    draw.rounded_rectangle([dx+40, btn_y, dx+dw//2-20, btn_y+100], radius=20, fill=SURFACE_2)
    draw.text((dx+130, btn_y+28), "Cancel", fill=TEXT_SEC, font=bold(34))
    draw.rounded_rectangle([dx+dw//2+20, btn_y, dx+dw-40, btn_y+100], radius=20, fill=PRIMARY)
    draw.text((dx+dw//2+90, btn_y+28), "Create PDF", fill=WHITE, font=bold(34))

    img.save(out_path, "PNG")
    print(f"  ✓ Config Dialog: {out_path}")


# ─── Screen 4: Filter / Enhancement Preview ────────────────────────────────────
def screen_filter(out_path):
    img, draw = new_canvas()
    status_bar(draw)
    content_y = top_bar(draw, "Enhancement Filter", "Choose a filter style", y=80)

    # Before / After mock
    pw = (W - 140) // 2
    ph = 580
    px1, px2 = 52, 52 + pw + 36
    py_img = content_y + 40

    # "Before" card (colour mock)
    for py2 in range(ph):
        t = py2 / ph
        r2 = int(99 + t*(150-99))
        g2 = int(102 + t*(80-102))
        b2 = int(241 + t*(200-241))
        draw.line([(px1, py_img+py2), (px1+pw, py_img+py2)], fill=(r2,g2,b2))
    draw.rectangle([px1, py_img+ph-50, px1+pw, py_img+ph], fill=(0,0,0,100))
    draw.text((px1+pw//2-55, py_img+ph-40), "ORIGINAL", fill=WHITE, font=bold(24))

    # "After" card (greyscale mock)
    for py2 in range(ph):
        t = py2 / ph
        v = int(220 - t*160)
        draw.line([(px2, py_img+py2), (px2+pw, py_img+py2)], fill=(v,v,v))
    draw.rectangle([px2, py_img+ph-50, px2+pw, py_img+ph], fill=(0,0,0,100))
    draw.text((px2+pw//2-60, py_img+ph-40), "B&W CLEAN", fill=WHITE, font=bold(24))

    # Filter chip row
    filters = [("None", False), ("B&W Clean", True), ("Enhance", False), ("Sharpen", False)]
    fx = 52
    fy = content_y + 680
    draw.text((52, fy-44), "Filter Style", fill=TEXT_SEC, font=bold(28))
    for label, sel in filters:
        fw = len(label)*14 + 40
        bg = PRIMARY if sel else SURFACE_2
        outline_c = PRIMARY if sel else DIVIDER
        draw.rounded_rectangle([fx, fy, fx+fw, fy+68], radius=18, fill=bg, outline=outline_c, width=2)
        draw.text((fx+20, fy+16), label, fill=WHITE if sel else TEXT_SEC, font=bold(28))
        fx += fw + 20

    # Intensity slider
    sy = content_y + 830
    draw.text((52, sy), "Intensity", fill=TEXT_SEC, font=bold(28))
    track_y = sy + 60
    draw.rounded_rectangle([52, track_y, W-52, track_y+14], radius=7, fill=SURFACE_2)
    draw.rounded_rectangle([52, track_y, 52 + int((W-104)*0.75), track_y+14], radius=7, fill=PRIMARY)
    thumb_x = 52 + int((W-104)*0.75)
    draw.ellipse([thumb_x-28, track_y-21, thumb_x+28, track_y+35], fill=PRIMARY)

    # Page size & quality below
    iy = track_y + 110
    draw.text((52, iy), "Quality & Output", fill=TEXT_SEC, font=bold(28))
    opts = [("Page Size", "A4"), ("Quality", "90%"), ("Compression", "Medium")]
    oy = iy + 50
    for label, val in opts:
        card(draw, 52, oy, W-104, 86, radius=16, fill=(*WHITE,5))
        draw.text((88, oy+22), label, fill=TEXT_PRIMARY, font=regular(30))
        draw.text((W-200, oy+22), val, fill=ACCENT, font=bold(30))
        oy += 102

    # Bottom apply button
    by = H - 300
    draw.rounded_rectangle([52, by, W-52, by+110], radius=28, fill=PRIMARY)
    draw.text(((W-220)//2, by+28), "Apply & Convert", fill=WHITE, font=bold(36))

    nav_y = bottom_nav(img, draw, active=0)
    img.save(out_path, "PNG")
    print(f"  ✓ Filter Screen: {out_path}")


# ─── Screen 5: History / My Documents ─────────────────────────────────────────
def screen_history(out_path):
    img, draw = new_canvas()
    status_bar(draw)
    content_y = top_bar(draw, "My Documents", "Your PDF history", y=80)

    docs = [
        ("vacation_photos.pdf",   "3.2 MB", "2 pages",  "Sep 17, 2026", (99,102,241)),
        ("receipts_sept.pdf",     "1.1 MB", "5 pages",  "Sep 16, 2026", (6,182,212)),
        ("contract_final.pdf",    "0.8 MB", "3 pages",  "Sep 15, 2026", (220,38,38)),
        ("scan_document_001.pdf", "2.5 MB", "4 pages",  "Sep 14, 2026", (245,158,11)),
        ("photos_backup.pdf",     "4.0 MB", "8 pages",  "Sep 13, 2026", (16,185,129)),
    ]

    dy = content_y + 36
    for name, size, pages, date, color in docs:
        # Card
        card(draw, 40, dy, W-80, 160, radius=22, fill=SURFACE_2)
        # Color stripe left
        draw.rounded_rectangle([40, dy, 52, dy+160], radius=22, fill=color)
        # PDF badge
        draw.rounded_rectangle([72, dy+36, 136, dy+124], radius=14, fill=(*color,30), outline=color, width=2)
        draw.text((80, dy+62), "PDF", fill=color, font=bold(28))
        # Name & meta
        draw.text((158, dy+30), name[:28], fill=TEXT_PRIMARY, font=bold(30))
        draw.text((158, dy+78), f"{size}  •  {pages}  •  {date}", fill=TEXT_SEC, font=regular(26))
        # Share icon
        draw.rounded_rectangle([W-140, dy+40, W-60, dy+120], radius=16, fill=(*WHITE,5))
        draw.text((W-128, dy+52), "⬆", fill=TEXT_SEC, font=bold(36))
        dy += 182

    # Empty state hint at bottom
    draw.text((52, dy+20), "Swipe left on any file to delete", fill=TEXT_DIM, font=regular(26))

    nav_y = bottom_nav(img, draw, active=1)
    img.save(out_path, "PNG")
    print(f"  ✓ History: {out_path}")


# ─── Main ───────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    base = r"f:\Projects\android projects\imagetopdf\docs\assets"
    icon = os.path.join(base, "icon_512.png")
    shots = os.path.join(base, "screenshots")
    os.makedirs(shots, exist_ok=True)

    print("Generating Play Store screenshots (1080×1920)…")
    screen_splash (icon,  os.path.join(shots, "01_splash.png"))
    screen_home   (       os.path.join(shots, "02_home_grid.png"))
    screen_config (       os.path.join(shots, "03_pdf_config.png"))
    screen_filter (       os.path.join(shots, "04_filter.png"))
    screen_history(       os.path.join(shots, "05_history.png"))
    print("\nDone! 5 screenshots saved to:", shots)
