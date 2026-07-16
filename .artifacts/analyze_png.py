from pathlib import Path

from PIL import Image


for path in [
    Path(".artifacts/render-word-v2/pages/page-05.png"),
    Path(".artifacts/render-word-v2/pages/page-06.png"),
    Path(".artifacts/render-word-v2/pages/page-07.png"),
]:
    image = Image.open(path).convert("RGB")
    pixels = list(image.getdata())
    total = len(pixels)
    dark = sum(1 for red, green, blue in pixels if red < 20 and green < 20 and blue < 20)
    white = sum(1 for red, green, blue in pixels if red > 245 and green > 245 and blue > 245)
    samples = [
        image.getpixel((0, 0)),
        image.getpixel((10, 10)),
        image.getpixel((image.width // 2, image.height // 2)),
    ]
    print(
        path.name,
        image.size,
        f"dark={dark / total:.3%}",
        f"white={white / total:.3%}",
        f"samples={samples}",
    )
