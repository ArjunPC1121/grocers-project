from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "grocers-er-diagram.png"
W, H = 1600, 900
GREEN = "#1d5a3a"
LINE = "#455a4c"
LOGICAL = "#7b8794"
TEXT = "#28382e"
MUTED = "#607164"
ORANGE = "#c65b19"

font_dir = Path("C:/Windows/Fonts")
regular = ImageFont.truetype(str(font_dir / "arial.ttf"), 16)
small = ImageFont.truetype(str(font_dir / "arial.ttf"), 14)
bold = ImageFont.truetype(str(font_dir / "arialbd.ttf"), 16)
title_font = ImageFont.truetype(str(font_dir / "arialbd.ttf"), 30)
subtitle = ImageFont.truetype(str(font_dir / "arial.ttf"), 16)

img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)


def arrow(points, label=None, label_at=None, dashed=True):
    color = LOGICAL if dashed else LINE
    width = 2
    for a, b in zip(points, points[1:]):
        if dashed:
            dx, dy = b[0] - a[0], b[1] - a[1]
            length = max((dx * dx + dy * dy) ** 0.5, 1)
            step = 13
            for n in range(0, int(length), step):
                start, end = n, min(n + 8, length)
                d.line((a[0] + dx * start / length, a[1] + dy * start / length,
                        a[0] + dx * end / length, a[1] + dy * end / length), fill=color, width=width)
        else:
            d.line((a, b), fill=color, width=width)
    end, prev = points[-1], points[-2]
    dx, dy = end[0] - prev[0], end[1] - prev[1]
    length = max((dx * dx + dy * dy) ** 0.5, 1)
    ux, uy = dx / length, dy / length
    px, py = -uy, ux
    d.polygon([end, (end[0] - 11 * ux + 5 * px, end[1] - 11 * uy + 5 * py),
               (end[0] - 11 * ux - 5 * px, end[1] - 11 * uy - 5 * py)], fill=color)
    if label:
        x, y = label_at
        box = d.textbbox((x, y), label, font=bold)
        d.rounded_rectangle((box[0] - 4, box[1] - 2, box[2] + 4, box[3] + 2), 4, fill="white")
        d.text((x, y), label, font=bold, fill=LINE)


def node(x, y, w, h, name, fields):
    d.rounded_rectangle((x, y, x + w, y + h), radius=12, fill="white", outline="#88a28f", width=2)
    d.rounded_rectangle((x, y, x + w, y + 35), radius=12, fill=GREEN)
    d.rectangle((x, y + 23, x + w, y + 35), fill=GREEN)
    d.text((x + 15, y + 10), name, font=bold, fill="white")
    for i, field in enumerate(fields):
        fy = y + 47 + 22 * i
        if field.startswith("PK") or field.startswith("FK"):
            prefix, rest = field[:2], field[2:].strip()
            d.text((x + 15, fy), prefix, font=bold, fill=ORANGE)
            d.text((x + 38, fy), rest, font=small, fill=TEXT)
        else:
            d.text((x + 15, fy), field, font=small, fill=TEXT)


d.text((80, 48), "Grocers — Core Entity Relationship Diagram", font=title_font, fill="#173b2a")
d.text((80, 82), "Customer shopping, fulfilment, employee approvals, and Kafka-powered admin notifications", font=subtitle, fill=MUTED)

# Relationship lines first, so every entity card stays clean and readable.
arrow([(270, 185), (350, 185)], "submits", (285, 166))
arrow([(570, 185), (650, 185)], "reviews", (585, 166))
arrow([(570, 135), (730, 92), (930, 135)], "Kafka: request created", (690, 83))
arrow([(850, 185), (930, 185)], "reads", (862, 166))
arrow([(570, 230), (810, 275), (1190, 340)], "requests product change", (765, 274))
arrow([(270, 415), (350, 415)], "owns", (285, 396))
arrow([(520, 415), (600, 415)], "contains", (534, 396), dashed=False)
arrow([(770, 385), (940, 280), (1190, 350)], "references", (888, 291))
arrow([(270, 465), (470, 555), (850, 455)], "places", (520, 545))
arrow([(1030, 420), (1110, 545)], "contains", (1040, 477), dashed=False)
arrow([(1210, 545), (1290, 485), (1290, 430)], "references", (1276, 477))
arrow([(270, 470), (420, 600), (600, 700)], "raises ticket", (415, 600))
arrow([(60, 205), (25, 340), (25, 705), (600, 730)], "resolves", (28, 480))
arrow([(270, 470), (310, 830), (960, 840), (1100, 760)], "wallet / funds", (700, 810))
arrow([(1260, 745), (1360, 745)], "top-up", (1274, 725))

node(60, 110, 210, 135, "EMPLOYEE", ["PK  id", "name · email", "status", "mustChangePassword"])
node(350, 110, 220, 135, "PRODUCT REQUEST", ["PK  requestId", "FK  employeeId · productId", "action · quantity", "status"])
node(650, 110, 200, 125, "ADMIN", ["PK  adminId", "email · role", "password"])
node(930, 110, 220, 125, "ADMIN NOTIFICATION", ["PK  id", "FK  requestId · employeeId", "message · read"])
node(60, 350, 210, 145, "USER", ["PK  id", "name · email", "accountNumber", "funds · accountLocked"])
node(350, 365, 170, 105, "CART", ["PK  id", "FK  userId · status"])
node(600, 365, 170, 105, "CART ITEM", ["PK  id", "FK  cartId · productId"])
node(850, 350, 180, 135, "ORDER", ["PK  id", "FK  customerId · cartId", "status · paymentMethod", "totalAmount"])
node(1190, 300, 180, 130, "PRODUCT", ["PK  id", "name · category", "price · quantity", "discount · active"])
node(1110, 545, 200, 105, "ORDER ITEM", ["PK  id", "FK  orderId · productId"])
node(600, 660, 200, 115, "TICKET", ["PK  ticketId", "FK  userId · employeeId", "status · lockedReason"])
node(1100, 690, 160, 105, "FUNDS", ["PK  id", "FK  userId"])
node(1360, 690, 180, 105, "BANK ACCOUNT", ["PK  accountNumber", "balance"])

arrow([(80, 850), (120, 850)], dashed=False)
d.text((135, 843), "Direct JPA relationship", font=small, fill=MUTED)
arrow([(360, 850), (400, 850)], dashed=True)
d.text((415, 843), "Cross-service / ID-based relationship", font=small, fill=MUTED)

img.save(OUTPUT, "PNG", optimize=True)
print(OUTPUT)
