import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def get_font(name, size):
    font_paths = [
        f"C:/Windows/Fonts/{name}",
        f"C:/Windows/Fonts/{name.lower()}",
    ]
    for p in font_paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except:
                pass
    try:
        return ImageFont.truetype("arialbd.ttf" if "bd" in name or "b." in name else "arial.ttf", size)
    except:
        return ImageFont.load_default()

# -------------------------------------------------------------------------
# Vector Icon Drawing Helpers (guaranteed crisp, no missing glyphs)
# -------------------------------------------------------------------------

def draw_vector_shield(draw, cx, cy, size, fill_color, stroke_color=None):
    """Draws a security shield with a checkmark."""
    s = size / 2
    # Shield shape points
    pts = [
        (cx, cy - s),
        (cx + s * 0.85, cy - s * 0.7),
        (cx + s * 0.85, cy + s * 0.15),
        (cx, cy + s),
        (cx - s * 0.85, cy + s * 0.15),
        (cx - s * 0.85, cy - s * 0.7),
    ]
    draw.polygon(pts, fill=fill_color)
    if stroke_color:
        draw.polygon(pts, outline=stroke_color, width=2)
    # Checkmark inside
    chk_color = (255, 255, 255, 255)
    draw.line([(cx - s * 0.35, cy - s * 0.05), (cx - s * 0.05, cy + s * 0.35)], fill=chk_color, width=3)
    draw.line([(cx - s * 0.05, cy + s * 0.35), (cx + s * 0.45, cy - s * 0.35)], fill=chk_color, width=3)

def draw_vector_sparkle(draw, cx, cy, size, fill_color):
    """Draws a 4-point star / sparkle for Ultra-HD clarity."""
    s = size / 2
    pts = [
        (cx, cy - s),
        (cx + s * 0.25, cy - s * 0.25),
        (cx + s, cy),
        (cx + s * 0.25, cy + s * 0.25),
        (cx, cy + s),
        (cx - s * 0.25, cy + s * 0.25),
        (cx - s, cy),
        (cx - s * 0.25, cy - s * 0.25)
    ]
    draw.polygon(pts, fill=fill_color)

def draw_vector_sliders(draw, cx, cy, size, stroke_color):
    """Draws clean filter / slider adjustment icon."""
    s = size / 2
    y1 = cy - s * 0.5
    y2 = cy + s * 0.5
    # Track 1
    draw.line([(cx - s * 0.8, y1), (cx + s * 0.8, y1)], fill=stroke_color, width=3)
    draw.ellipse([cx - s * 0.3, y1 - 6, cx + s * 0.1, y1 + 6], fill=stroke_color)
    # Track 2
    draw.line([(cx - s * 0.8, y2), (cx + s * 0.8, y2)], fill=stroke_color, width=3)
    draw.ellipse([cx + s * 0.1, y2 - 6, cx + s * 0.5, y2 + 6], fill=stroke_color)

def draw_vector_pdf_doc(draw, cx, cy, size, fill_color, stroke_color):
    """Draws a document icon with folded top-right corner."""
    w = size * 0.75
    h = size
    x0 = cx - w / 2
    y0 = cy - h / 2
    x1 = cx + w / 2
    y1 = cy + h / 2
    fold = size * 0.3
    
    doc_pts = [
        (x0, y0),
        (x1 - fold, y0),
        (x1, y0 + fold),
        (x1, y1),
        (x0, y1)
    ]
    draw.polygon(doc_pts, fill=fill_color, outline=stroke_color, width=2)
    # Fold triangle
    draw.polygon([(x1 - fold, y0), (x1, y0 + fold), (x1 - fold, y0 + fold)], fill=(220, 38, 38, 255))
    # Doc lines
    draw.line([(x0 + 6, y0 + fold + 4), (x1 - 6, y0 + fold + 4)], fill=stroke_color, width=2)
    draw.line([(x0 + 6, y0 + fold + 12), (x1 - 10, y0 + fold + 12)], fill=stroke_color, width=2)
    draw.line([(x0 + 6, y0 + fold + 20), (x1 - 8, y0 + fold + 20)], fill=stroke_color, width=2)

def draw_vector_bolt(draw, cx, cy, size, fill_color):
    """Draws a crisp lightning bolt."""
    s = size / 2
    pts = [
        (cx + s * 0.15, cy - s),
        (cx - s * 0.7, cy + s * 0.05),
        (cx - s * 0.05, cy + s * 0.05),
        (cx - s * 0.2, cy + s),
        (cx + s * 0.7, cy - s * 0.15),
        (cx + s * 0.05, cy - s * 0.15),
    ]
    draw.polygon(pts, fill=fill_color)

# -------------------------------------------------------------------------
# Phone Mockup Renderer
# -------------------------------------------------------------------------

def create_phone_mockup(screenshot_path, target_height=780, corner_radius=38, bezel_side=12, bezel_top=24, bezel_bottom=24):
    sc = Image.open(screenshot_path).convert("RGBA")
    screen_h = target_height - bezel_top - bezel_bottom
    screen_w = int(screen_h * (sc.width / sc.height))
    sc_resized = sc.resize((screen_w, screen_h), Image.Resampling.LANCZOS)
    
    # Rounded corners for screen
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle([0, 0, screen_w, screen_h], radius=corner_radius - 8, fill=255)
    
    screen_canvas = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    screen_canvas.paste(sc_resized, (0, 0), screen_mask)
    
    phone_w = screen_w + bezel_side * 2
    phone_h = target_height
    
    phone = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)
    
    # Outer phone body: dark titanium gradient
    body_mask = Image.new("L", (phone_w, phone_h), 0)
    ImageDraw.Draw(body_mask).rounded_rectangle([0, 0, phone_w, phone_h], radius=corner_radius, fill=255)
    
    for y in range(phone_h):
        t = y / phone_h
        r = int(32 - t * 15)
        g = int(44 - t * 20)
        b = int(62 - t * 26)
        draw.line([(0, y), (phone_w, y)], fill=(r, g, b, 255))
    phone.putalpha(body_mask)
    
    # Outer rim highlight
    draw.rounded_rectangle([1, 1, phone_w - 2, phone_h - 2], radius=corner_radius, outline=(148, 163, 184, 85), width=2)
    draw.rounded_rectangle([3, 3, phone_w - 4, phone_h - 4], radius=corner_radius - 2, outline=(15, 23, 42, 230), width=2)
    
    # Paste screen
    phone.paste(screen_canvas, (bezel_side, bezel_top), screen_canvas)
    draw = ImageDraw.Draw(phone)
    
    # Inner display rim
    draw.rounded_rectangle(
        [bezel_side - 1, bezel_top - 1, bezel_side + screen_w + 1, bezel_top + screen_h + 1],
        radius=corner_radius - 7,
        outline=(15, 23, 42, 200),
        width=2
    )
    
    # Camera punch hole
    cx = phone_w // 2
    cam_y = bezel_top + 14
    cam_r = 7
    draw.ellipse([cx - cam_r, cam_y - cam_r, cx + cam_r, cam_y + cam_r], fill=(8, 12, 20, 255))
    draw.ellipse([cx - cam_r, cam_y - cam_r, cx + cam_r, cam_y + cam_r], outline=(30, 41, 59, 180), width=1)
    draw.ellipse([cx - 2, cam_y - 2, cx + 1, cam_y + 1], fill=(56, 189, 248, 150))
    
    # Diagonal glass gloss
    gloss = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    gloss_draw = ImageDraw.Draw(gloss)
    gloss_polygon = [
        (bezel_side, bezel_top),
        (bezel_side + int(screen_w * 0.75), bezel_top),
        (bezel_side, bezel_top + int(screen_h * 0.55)),
    ]
    gloss_draw.polygon(gloss_polygon, fill=(255, 255, 255, 15))
    phone = Image.alpha_composite(phone, gloss)
    
    return phone

def create_glass_chip(title, subtitle, icon_type="shield", accent_color=(6, 182, 212)):
    w, h = 350, 94
    chip = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(chip)
    
    # Frosted glass body with smooth border
    draw.rounded_rectangle([0, 0, w, h], radius=22, fill=(15, 23, 42, 225), outline=(255, 255, 255, 55), width=2)
    draw.rounded_rectangle([2, 2, w - 2, h - 2], radius=20, outline=(accent_color[0], accent_color[1], accent_color[2], 80), width=1)
    
    # Icon box
    draw.rounded_rectangle([16, 16, 76, 76], radius=16, fill=(accent_color[0], accent_color[1], accent_color[2], 40), outline=accent_color, width=2)
    
    icx = 46
    icy = 46
    if icon_type == "shield":
        draw_vector_shield(draw, icx, icy, 34, accent_color, (255, 255, 255, 200))
    elif icon_type == "sparkle":
        draw_vector_sparkle(draw, icx, icy, 36, accent_color)
    elif icon_type == "bolt":
        draw_vector_bolt(draw, icx, icy, 34, accent_color)
    elif icon_type == "doc":
        draw_vector_pdf_doc(draw, icx, icy, 32, (255, 255, 255, 230), (203, 213, 225, 255))
    
    title_font = get_font("segoeuib.ttf", 24)
    sub_font = get_font("segoeui.ttf", 18)
    
    draw.text((94, 21), title, fill=(255, 255, 255, 255), font=title_font)
    draw.text((94, 52), subtitle, fill=(148, 163, 184, 255), font=sub_font)
    
    return chip

def add_drop_shadow(image, blur_radius=32, offset=(12, 28), shadow_color=(0, 0, 0, 170)):
    pad = blur_radius * 2 + 40
    w = image.width + pad * 2
    h = image.height + pad * 2
    
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    alpha = image.split()[3]
    shadow_layer = Image.new("RGBA", (image.width, image.height), shadow_color)
    shadow_layer.putalpha(alpha)
    
    shadow_x = pad + offset[0]
    shadow_y = pad + offset[1]
    shadow.paste(shadow_layer, (shadow_x, shadow_y), shadow_layer)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur_radius))
    shadow.paste(image, (pad, pad), image)
    
    return shadow, pad

def generate_feature_graphic(output_path):
    W, H = 2048, 1000
    canvas = Image.new("RGBA", (W, H), (10, 15, 29, 255))
    draw = ImageDraw.Draw(canvas)
    
    # 1. Background Gradient: Deep Studio Slate / Obsidian
    for y in range(H):
        t = y / H
        r = int(9 + t * (16 - 9))
        g = int(14 + t * (24 - 14))
        b = int(26 + t * (44 - 26))
        draw.line([(0, y), (W, y)], fill=(r, g, b, 255))
    
    # 2. Ambient Lighting (Radial Glows)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    
    # Intense modern cyan glow behind front phone
    gdraw.ellipse([1300, 80, 2050, 880], fill=(6, 182, 212, 55))
    # Indigo aura behind back phone
    gdraw.ellipse([1000, 120, 1850, 950], fill=(99, 102, 241, 65))
    # Soft violet aura behind top left icon
    gdraw.ellipse([-50, -50, 650, 650], fill=(139, 92, 246, 30))
    # Soft warm accent glow at very bottom right
    gdraw.ellipse([1550, 750, 2050, 1150], fill=(56, 189, 248, 35))
    
    glow = glow.filter(ImageFilter.GaussianBlur(120))
    canvas = Image.alpha_composite(canvas, glow)
    draw = ImageDraw.Draw(canvas)
    
    # Subtle modern dot matrix or grid accent
    grid_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    grid_draw = ImageDraw.Draw(grid_layer)
    for gx in range(40, W, 64):
        for gy in range(40, H, 64):
            grid_draw.ellipse([gx - 1, gy - 1, gx + 1, gy + 1], fill=(255, 255, 255, 12))
    canvas = Image.alpha_composite(canvas, grid_layer)
    draw = ImageDraw.Draw(canvas)
    
    # 3. Prepare Phone Mockups
    sc_dir = "docs/assets/screenshots"
    sc_selected = os.path.join(sc_dir, "selected_screen.png")
    sc_pdf = os.path.join(sc_dir, "pdf_save_options.png")
    
    # Back Phone: PDF Settings (height 770, rotated +6 degrees)
    phone_back = create_phone_mockup(sc_pdf, target_height=770, corner_radius=38, bezel_side=12, bezel_top=23, bezel_bottom=23)
    phone_back_rot = phone_back.rotate(5, resample=Image.BICUBIC, expand=True)
    phone_back_shadow, pad_b = add_drop_shadow(phone_back_rot, blur_radius=38, offset=(10, 26), shadow_color=(0, 0, 0, 180))
    
    # Front Phone: Selected Screen (height 820, rotated -2 degrees)
    phone_front = create_phone_mockup(sc_selected, target_height=820, corner_radius=40, bezel_side=13, bezel_top=24, bezel_bottom=24)
    phone_front_rot = phone_front.rotate(-2, resample=Image.BICUBIC, expand=True)
    phone_front_shadow, pad_f = add_drop_shadow(phone_front_rot, blur_radius=44, offset=(14, 34), shadow_color=(0, 0, 0, 215))
    
    # Paste Back Phone
    pos_back = (1170 - pad_b, 105 - pad_b)
    canvas.paste(phone_back_shadow, pos_back, phone_back_shadow)
    
    # Paste Front Phone
    pos_front = (1475 - pad_f, 95 - pad_f)
    canvas.paste(phone_front_shadow, pos_front, phone_front_shadow)
    
    # 4. Floating Feature Chips (placed to frame phones nicely without obscuring action buttons)
    # Chip 1: Left of Back Phone (Lossless Clarity)
    chip1 = create_glass_chip("Ultra-HD Clarity", "Full 100% sensor detail", icon_type="sparkle", accent_color=(6, 182, 212))
    chip1_shadow, pad_c1 = add_drop_shadow(chip1, blur_radius=22, offset=(6, 14), shadow_color=(0, 0, 0, 160))
    canvas.paste(chip1_shadow, (1030 - pad_c1, 560 - pad_c1), chip1_shadow)
    
    # Chip 2: Floating near top-left of front phone (100% Offline)
    chip2 = create_glass_chip("100% Offline", "Zero cloud • Private & secure", icon_type="shield", accent_color=(16, 185, 129))
    chip2_shadow, pad_c2 = add_drop_shadow(chip2, blur_radius=22, offset=(6, 14), shadow_color=(0, 0, 0, 160))
    canvas.paste(chip2_shadow, (1340 - pad_c2, 40 - pad_c2), chip2_shadow)
    
    # 5. Left Column: Branding, Title & Feature Highlights
    draw = ImageDraw.Draw(canvas)
    
    # App Icon with ambient plate & drop shadow
    icon_path = "docs/assets/icon_512.png"
    if os.path.exists(icon_path):
        icon_img = Image.open(icon_path).convert("RGBA").resize((136, 136), Image.Resampling.LANCZOS)
        icon_shadow, pad_i = add_drop_shadow(icon_img, blur_radius=24, offset=(4, 12), shadow_color=(0, 0, 0, 190))
        canvas.paste(icon_shadow, (90 - pad_i, 90 - pad_i), icon_shadow)
    
    # Beside Icon: App Name & Official Badge
    # Top badge pill
    pill_font = get_font("segoeuib.ttf", 20)
    pill_text = "FREE & OPEN SOURCE"
    ptw = int(draw.textlength(pill_text, font=pill_font))
    draw.rounded_rectangle([254, 94, 254 + ptw + 34, 134], radius=10, fill=(6, 182, 212, 35), outline=(6, 182, 212, 190), width=2)
    draw.text((271, 101), pill_text, fill=(56, 189, 248, 255), font=pill_font)
    
    # Main App Name: "ImageToPdf Free"
    app_title_font = get_font("segoeuib.ttf", 52)
    draw.text((254, 142), "ImageToPdf Free", fill=(255, 255, 255, 255), font=app_title_font)
    
    # Studio Quality sub-tag
    tag_font = get_font("segoeuib.ttf", 19)
    draw.text((256, 204), "STUDIO QUALITY  •  PRIVATE BY DESIGN", fill=(148, 163, 184, 255), font=tag_font)
    
    # Hero Headline: "Turn Photos into Clean, Sharp PDFs"
    headline_font = get_font("segoeuib.ttf", 54)
    # Subtle soft shadow
    draw.text((92, 282), "Turn Photos into Clean, Sharp PDFs", fill=(0, 0, 0, 180), font=headline_font)
    draw.text((90, 280), "Turn Photos into Clean, Sharp PDFs", fill=(255, 255, 255, 255), font=headline_font)
    
    # Subtitle / description
    desc_font = get_font("segoeui.ttf", 26)
    desc_text = (
        "High-fidelity on-device document scanner & multi-page converter.\n"
        "Crisp B&W enhancement, page reordering, margins & stamps."
    )
    draw.text((90, 360), desc_text, fill=(203, 213, 225, 230), font=desc_font, spacing=10)
    
    # Feature Bullet Points with crisp custom vector icons
    features = [
        ("shield", "100% On-Device Privacy", "Zero cloud uploads, zero tracking, works fully offline", (16, 185, 129)),
        ("sparkle", "Ultra-HD Lossless Clarity", "Full camera sensor resolution, no blurry compression", (6, 182, 212)),
        ("sliders", "B&W Document Enhancement", "Clean contrast filter, grayscale & page reordering", (245, 158, 11)),
        ("doc", "Flexible PDF Customization", "A4 / US / Fit, custom margins, stamps & page numbers", (129, 140, 248))
    ]
    
    feat_y = 485
    f_title_font = get_font("segoeuib.ttf", 26)
    f_sub_font = get_font("segoeui.ttf", 21)
    
    for icon_type, f_title, f_desc, icon_col in features:
        # Icon box
        ibox = [90, feat_y, 142, feat_y + 52]
        draw.rounded_rectangle(ibox, radius=12, fill=(icon_col[0], icon_col[1], icon_col[2], 35), outline=(icon_col[0], icon_col[1], icon_col[2], 120), width=1)
        
        icx = (ibox[0] + ibox[2]) // 2
        icy = (ibox[1] + ibox[3]) // 2
        
        if icon_type == "shield":
            draw_vector_shield(draw, icx, icy, 26, icon_col, (255, 255, 255, 180))
        elif icon_type == "sparkle":
            draw_vector_sparkle(draw, icx, icy, 28, icon_col)
        elif icon_type == "sliders":
            draw_vector_sliders(draw, icx, icy, 26, icon_col)
        elif icon_type == "doc":
            draw_vector_pdf_doc(draw, icx, icy, 26, (255, 255, 255, 220), (203, 213, 225, 255))
        
        # Text
        draw.text((160, feat_y + 2), f_title, fill=(255, 255, 255, 255), font=f_title_font)
        draw.text((160, feat_y + 30), f_desc, fill=(148, 163, 184, 255), font=f_sub_font)
        
        feat_y += 74
    
    # Bottom Trust Badges (Horizontal Row)
    badges = [
        ("NO ADS", (16, 185, 129)),
        ("NO ACCOUNTS", (6, 182, 212)),
        ("NO SUBSCRIPTIONS", (129, 140, 248)),
        ("100% FREE", (245, 158, 11))
    ]
    
    bx = 90
    by = 840
    b_font = get_font("segoeuib.ttf", 20)
    for blabel, bcolor in badges:
        bw = int(draw.textlength(blabel, font=b_font)) + 38
        draw.rounded_rectangle([bx, by, bx + bw, by + 44], radius=11, fill=(bcolor[0], bcolor[1], bcolor[2], 30), outline=bcolor, width=2)
        draw.text((bx + 19, by + 10), blabel, fill=bcolor, font=b_font)
        bx += bw + 18
    
    # 6. Final Downsampling: Resize from 2048x1000 down to standard 1024x500
    final_img = canvas.resize((1024, 500), Image.Resampling.LANCZOS)
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    final_img.save(output_path, "PNG", optimize=True)
    print(f"Successfully generated 1024x500 Feature Graphic at: {output_path}")

if __name__ == "__main__":
    out_file = r"f:\Projects\android projects\imagetopdf\docs\assets\feature_graphic_1024x500.png"
    generate_feature_graphic(out_file)
