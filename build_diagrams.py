#!/usr/bin/env python3
"""Draw the 3 Healthcare Appointment System diagrams as PNGs (Pillow, no network)."""
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
W, H = 1400, 900
BG = (255, 255, 255)
INK = (20, 20, 20)
BLUE = (27, 94, 168)
GREEN = (27, 140, 80)
ORANGE = (196, 110, 0)
RED = (180, 40, 40)
GREY = (90, 90, 90)
BASE = "/home/hatch/workspace/job-search/project-15-healthcare-appointment/docs"


def font(size, bold=False):
    return ImageFont.truetype(BOLD if bold else FONT, size)


def wrap(draw, text, fnt, max_w):
    words, lines, cur = text.split(), [], ""
    for word in words:
        trial = f"{cur} {word}".strip()
        if draw.textlength(trial, font=fnt) <= max_w:
            cur = trial
        else:
            lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines


def box(draw, x, y, w, h, title, body="", fill=(235, 242, 252), outline=BLUE):
    draw.rounded_rectangle([x, y, x + w, y + h], radius=14,
                           fill=fill, outline=outline, width=3)
    tf, bf = font(26, True), font(21)
    tlines = wrap(draw, title, tf, w - 30)
    for i, line in enumerate(tlines):
        draw.text((x + 15, y + 12 + i * 30), line, font=tf, fill=INK)
    ty = y + 12 + len(tlines) * 30 + 6
    for i, line in enumerate(wrap(draw, body, bf, w - 30)):
        draw.text((x + 15, ty + i * 26), line, font=bf, fill=(60, 60, 60))


def arrow(draw, x1, y1, x2, y2, label="", color=INK):
    draw.line([x1, y1, x2, y2], fill=color, width=4)
    if x2 > x1:
        draw.polygon([(x2, y2), (x2 - 16, y2 - 9), (x2 - 16, y2 + 9)], fill=color)
    elif x2 < x1:
        draw.polygon([(x2, y2), (x2 + 16, y2 - 9), (x2 + 16, y2 + 9)], fill=color)
    elif y2 > y1:
        draw.polygon([(x2, y2), (x2 - 9, y2 - 16), (x2 + 9, y2 - 16)], fill=color)
    else:
        draw.polygon([(x2, y2), (x2 - 9, y2 + 16), (x2 + 9, y2 + 16)], fill=color)
    if label:
        mx, my = (x1 + x2) // 2, (y1 + y2) // 2
        draw.text((mx + 8, my - 26), label, font=font(20, True), fill=GREY)


def title_block(draw, title, subtitle):
    draw.text((60, 40), title, font=font(40, True), fill=INK)
    draw.text((60, 100), subtitle, font=font(24), fill=GREY)


def save(img, name):
    img.save(f"{BASE}/{name}")
    print("saved", name)


# ---- 1. Architecture ----
img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)
title_block(d, "Architecture", "Healthcare Patient and Appointment Management System — all local, no cloud")
box(d, 60, 170, 520, 170, "Client / Postman", "HTTP Basic auth — demo users: admin, staff, patient1. Requests carry no patient data in URLs.")
box(d, 820, 170, 520, 170, "Security filter chain", "HTTP Basic + role gates. CSRF off (stateless JSON API). Ownership enforced in services.")
arrow(d, 580, 255, 820, 255)
box(d, 60, 420, 380, 200, "Controllers", "Patient, Provider, Appointment, Auth. Validation via @Valid request bodies.")
box(d, 510, 420, 380, 200, "Services", "Scheduling rules, ownership checks, ID-only logging. No names or DOB in logs.")
box(d, 960, 420, 380, 200, "Repositories", "Spring Data JPA: overlap queries for provider and patient bookings.")
arrow(d, 440, 520, 510, 520)
arrow(d, 890, 520, 960, 520)
box(d, 380, 680, 640, 150, "H2 in-memory database (default)", "Patients, providers, appointments, user accounts. PostgreSQL is the documented production option.")
arrow(d, 700, 620, 700, 680)
save(img, "architecture.png")

# ---- 2. Appointment lifecycle ----
img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)
title_block(d, "Appointment lifecycle", "Every rejection path, and the two terminal states")
box(d, 60, 170, 500, 150, "Booking request", "patientId, providerId, startTime, reason. End time is derived from provider duration.")
box(d, 840, 170, 500, 150, "BOOKED", "Appointment saved. Provider and patient overlap queries both ran clean.", fill=(225, 245, 233), outline=GREEN)
arrow(d, 560, 245, 840, 245)
d.text((600, 208), "all rules pass", font=font(20, True), fill=GREY)
box(d, 60, 400, 380, 190, "Rejected: 400", "Slot in the past. Non-working day. Outside working hours. Unknown patient or provider.", fill=(253, 232, 232), outline=RED)
box(d, 510, 400, 380, 190, "Rejected: 409", "Provider already booked in this slot. Patient already booked in this slot.", fill=(253, 232, 232), outline=RED)
box(d, 960, 400, 380, 190, "Rejected: 403", "Patient booking for someone else. Patient viewing someone else's record.", fill=(253, 232, 232), outline=RED)
arrow(d, 250, 320, 250, 400, "", color=RED)
arrow(d, 310, 320, 700, 400, "", color=RED)
arrow(d, 370, 320, 1150, 400, "", color=RED)
box(d, 60, 650, 500, 150, "CANCELLED", "Only from BOOKED. Staff, admin, or the patient themselves.", fill=(255, 243, 224), outline=ORANGE)
box(d, 840, 650, 500, 150, "COMPLETED", "Only from BOOKED. Staff or admin only.", fill=(225, 245, 233), outline=GREEN)


def elbow(draw, points, color=INK):
    """Polyline with an arrowhead on the last segment (routes around boxes)."""
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        draw.line([x1, y1, x2, y2], fill=color, width=4)
    (x1, y1), (x2, y2) = points[-2], points[-1]
    if x2 > x1:
        draw.polygon([(x2, y2), (x2 - 16, y2 - 9), (x2 - 16, y2 + 9)], fill=color)
    elif x2 < x1:
        draw.polygon([(x2, y2), (x2 + 16, y2 - 9), (x2 + 16, y2 + 9)], fill=color)


# BOOKED -> CANCELLED via the left margin, BOOKED -> COMPLETED via the right.
elbow(d, [(840, 245), (30, 245), (30, 725), (60, 725)])
elbow(d, [(1340, 245), (1370, 245), (1370, 725), (1340, 725)])
save(img, "appointment-lifecycle.png")

# ---- 3. Auth / role flow ----
img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)
title_block(d, "Auth and role flow", "How a request earns its way to the data — and where it is stopped")
box(d, 60, 170, 1280, 120, "1. HTTP Basic credentials", "Username + password. Passwords stored only as BCrypt hashes; never returned by any endpoint.")
box(d, 60, 340, 600, 170, "2. UserDetailsService", "Loads the local UserAccount by username. Unknown user or bad password -> 401 Unauthorized.")
box(d, 740, 340, 600, 170, "3. Role gate (URL rules)", "Write endpoints need STAFF/ADMIN. Completing needs STAFF/ADMIN. Wrong role -> 403 Forbidden.")
arrow(d, 700, 425, 740, 425)
box(d, 60, 580, 600, 170, "4. Ownership check (services)", "PATIENT callers must match their linked patient id — for reads, bookings, cancels, searches. Mismatch -> 403.")
box(d, 740, 580, 600, 170, "5. Handler + minimized response", "List endpoints return summaries only (no DOB, phone, email, address). Logs carry IDs only.")
arrow(d, 660, 425, 660, 580)
arrow(d, 660, 665, 740, 665)
save(img, "auth-role-flow.png")
print("done")
