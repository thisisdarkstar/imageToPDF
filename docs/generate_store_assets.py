import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_store_icon(output_path):
    size = 512
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    center = size / 2
    plate_radius = 210  # 420px diameter inside 512x512 for perfect safe margin

    # Draw soft shadow for plate
    for i in range(16, 0, -2):
        alpha = int(12 * (1 - i / 16))
        draw.ellipse(
            [center - plate_radius - i + 2, center - plate_radius - i + 8,
             center + plate_radius + i + 2, center + plate_radius + i + 8],
            fill=(15, 23, 42, alpha)
        )

    # Pure white circular plate
    draw.ellipse(
        [center - plate_radius, center - plate_radius,
         center + plate_radius, center + plate_radius],
        fill=(255, 255, 255, 255)
    )
    # Subtle inner border for plate
    draw.ellipse(
        [center - plate_radius, center - plate_radius,
         center + plate_radius, center + plate_radius],
        outline=(241, 245, 249, 255),
        width=3
    )

    # Card 1: Back photo card (Indigo / Cyan gradient) tilted ~ -8 degrees
    card_w, card_h = 160, 210
    card1 = Image.new("RGBA", (card_w, card_h), (0, 0, 0, 0))
    c1_draw = ImageDraw.Draw(card1)

    # Gradient fill for Card 1
    for y in range(card_h):
        t = y / card_h
        r = int(6 + t * (99 - 6))
        g = int(182 - t * (182 - 102))
        b = int(212 + t * (241 - 212))
        c1_draw.line([(0, y), (card_w, y)], fill=(r, g, b, 255))

    # Mask Card 1 with rounded corners
    c1_mask = Image.new("L", (card_w, card_h), 0)
    ImageDraw.Draw(c1_mask).rounded_rectangle([0, 0, card_w, card_h], radius=24, fill=255)
    card1.putalpha(c1_mask)

    # Add photo card landscape sun & mountain icon
    c1_draw.ellipse([card_w - 45, 30, card_w - 20, 55], fill=(255, 255, 255, 220))
    # Mountains
    c1_draw.polygon([(20, card_h - 30), (70, card_h - 85), (110, card_h - 30)], fill=(255, 255, 255, 180))
    c1_draw.polygon([(80, card_h - 30), (120, card_h - 70), (145, card_h - 30)], fill=(255, 255, 255, 140))
    card1.putalpha(c1_mask)

    # Rotate Card 1
    card1_rot = card1.rotate(10, resample=Image.BICUBIC, expand=True)

    # Card 2: Front PDF document card tilted ~ +6 degrees
    card2_w, card2_h = 175, 230
    card2 = Image.new("RGBA", (card2_w, card2_h), (0, 0, 0, 0))
    c2_draw = ImageDraw.Draw(card2)

    # Card 2 White background with light border
    c2_mask = Image.new("L", (card2_w, card2_h), 0)
    ImageDraw.Draw(c2_mask).rounded_rectangle([0, 0, card2_w, card2_h], radius=26, fill=255)
    c2_draw.rounded_rectangle([0, 0, card2_w, card2_h], radius=26, fill=(250, 250, 252, 255))
    c2_draw.rounded_rectangle([0, 0, card2_w, card2_h], radius=26, outline=(226, 232, 240, 255), width=3)

    # Top right folded corner (crimson red)
    fold_size = 45
    c2_draw.polygon(
        [(card2_w - fold_size, 0), (card2_w, fold_size), (card2_w - fold_size, fold_size)],
        fill=(220, 38, 38, 255)
    )
    c2_draw.line([(card2_w - fold_size, 0), (card2_w, fold_size)], fill=(185, 28, 28, 255), width=2)

    # Document text lines
    line_color = (203, 213, 225, 255)
    c2_draw.rounded_rectangle([25, 45, 105, 55], radius=5, fill=(148, 163, 184, 255))
    c2_draw.rounded_rectangle([25, 70, 145, 78], radius=4, fill=line_color)
    c2_draw.rounded_rectangle([25, 90, 135, 98], radius=4, fill=line_color)
    c2_draw.rounded_rectangle([25, 110, 140, 118], radius=4, fill=line_color)

    # Crimson "PDF" badge pill at bottom
    badge_w, badge_h = 125, 46
    badge_x = (card2_w - badge_w) // 2
    badge_y = card2_h - 68
    c2_draw.rounded_rectangle(
        [badge_x, badge_y, badge_x + badge_w, badge_y + badge_h],
        radius=14,
        fill=(220, 38, 38, 255)
    )

    # Load font or draw crisp vector "PDF" text
    try:
        font = ImageFont.truetype("arialbd.ttf", 26)
        c2_draw.text((badge_x + 36, badge_y + 8), "PDF", fill=(255, 255, 255, 255), font=font)
    except:
        c2_draw.text((badge_x + 36, badge_y + 10), "PDF", fill=(255, 255, 255, 255))

    card2.putalpha(c2_mask)

    # Rotate Card 2
    card2_rot = card2.rotate(-7, resample=Image.BICUBIC, expand=True)

    # Paste Card 1 with seamless shadow onto plate
    c1_pos = (int(center - card1_rot.width / 2 - 25), int(center - card1_rot.height / 2 - 10))
    pad = 40
    c1_shadow = Image.new("RGBA", (card1_rot.width + pad * 2, card1_rot.height + pad * 2), (0, 0, 0, 0))
    c1_shadow.paste((15, 23, 42, 60), (pad, pad), mask=card1_rot.split()[3])
    c1_shadow = c1_shadow.filter(ImageFilter.GaussianBlur(12))
    img.paste(c1_shadow, (c1_pos[0] - pad + 4, c1_pos[1] - pad + 8), c1_shadow)
    img.paste(card1_rot, c1_pos, card1_rot)

    # Paste Card 2 with seamless shadow onto plate
    c2_pos = (int(center - card2_rot.width / 2 + 15), int(center - card2_rot.height / 2 + 10))
    c2_shadow = Image.new("RGBA", (card2_rot.width + pad * 2, card2_rot.height + pad * 2), (0, 0, 0, 0))
    c2_shadow.paste((15, 23, 42, 75), (pad, pad), mask=card2_rot.split()[3])
    c2_shadow = c2_shadow.filter(ImageFilter.GaussianBlur(14))
    img.paste(c2_shadow, (c2_pos[0] - pad + 6, c2_pos[1] - pad + 10), c2_shadow)
    img.paste(card2_rot, c2_pos, card2_rot)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, "PNG")
    print(f"Generated 512x512 Store Icon at: {output_path}")

def create_feature_graphic(icon_path, output_path):
    from generate_feature_graphic import generate_feature_graphic
    generate_feature_graphic(output_path)

def create_favicons(icon_path, assets_dir, docs_dir):
    if not os.path.exists(icon_path):
        return
    icon = Image.open(icon_path).convert("RGBA")
    icon.save(os.path.join(assets_dir, "favicon.ico"), format="ICO", sizes=[(16, 16), (32, 32), (48, 48), (64, 64)])
    icon.save(os.path.join(docs_dir, "favicon.ico"), format="ICO", sizes=[(16, 16), (32, 32), (48, 48), (64, 64)])
    icon.resize((32, 32), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, "favicon-32x32.png"))
    icon.resize((16, 16), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, "favicon-16x16.png"))
    icon.resize((180, 180), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, "apple-touch-icon.png"))
    print("Generated favicons (16x16, 32x32, 180x180, ICO)")

if __name__ == "__main__":
    docs_dir = r"f:\Projects\android projects\imagetopdf\docs"
    assets_dir = os.path.join(docs_dir, "assets")
    icon_out = os.path.join(assets_dir, "icon_512.png")
    banner_out = os.path.join(assets_dir, "feature_graphic_1024x500.png")
    create_store_icon(icon_out)
    create_favicons(icon_out, assets_dir, docs_dir)
    create_feature_graphic(icon_out, banner_out)
