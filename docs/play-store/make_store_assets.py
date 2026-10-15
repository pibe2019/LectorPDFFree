"""Genera el icono 512x512 y el grafico destacado 1024x500 de Play Store con la misma geometria
que res/drawable/ic_launcher_foreground.xml (viewport 108)."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent  # docs/play-store/. Requiere: pip install pillow
OUT.mkdir(parents=True, exist_ok=True)

INDIGO = (0x37, 0x55, 0xC3)
FOLD = (0xC9, 0xD2, 0xF5)
WHITE = (255, 255, 255)
SS = 4  # supersampling para bordes suaves


def draw_glyph(draw, ox, oy, s):
    """Dibuja la hoja (coordenadas del viewport 108) con escala s y origen (ox, oy)."""
    p = lambda x, y: (ox + x * s, oy + y * s)
    draw.polygon([p(36, 28), p(61, 28), p(72, 39), p(72, 80), p(36, 80)], fill=WHITE)
    draw.polygon([p(61, 28), p(61, 39), p(72, 39)], fill=FOLD)
    for y, w in ((50, 24), (58, 24), (66, 16)):
        draw.rectangle([p(42, y), p(42 + w, y + 3.5)], fill=INDIGO)


def icon():
    size = 512 * SS
    img = Image.new("RGB", (size, size), INDIGO)
    # Icono de Play: cuadrado a sangre; Play aplica su propia mascara redondeada.
    s = size / 108 * 1.3  # la hoja ocupa mas: el icono de Play no lleva el margen de seguridad del adaptativo
    draw_glyph(ImageDraw.Draw(img), size / 2 - 54 * s, size / 2 - 54 * s, s)
    img.resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png")


def feature_graphic():
    w, h = 1024 * SS, 500 * SS
    img = Image.new("RGB", (w, h), INDIGO)
    draw = ImageDraw.Draw(img)
    s = h / 108 * 0.95
    draw_glyph(draw, w * 0.09 - 36 * s, (h - 108 * s) / 2, s)
    title = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 96 * SS)
    subtitle = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 46 * SS)
    x = w * 0.40
    draw.text((x, h * 0.30), "MyPDFFree", font=title, fill=WHITE)
    draw.text((x, h * 0.58), "Lector de PDF gratuito", font=subtitle, fill=FOLD)
    img.resize((1024, 500), Image.LANCZOS).save(OUT / "feature-graphic-1024x500.png")


icon()
feature_graphic()
print("ok", list(OUT.iterdir()))
