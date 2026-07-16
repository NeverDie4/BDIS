from __future__ import annotations

import sys
from pathlib import Path

import fitz


def main() -> None:
    src = Path(sys.argv[1])
    out = Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    doc = fitz.open(src)
    matrix = fitz.Matrix(150 / 72, 150 / 72)
    for index, page in enumerate(doc, start=1):
        pix = page.get_pixmap(matrix=matrix, alpha=False)
        pix.save(out / f"page-{index}.png")
    print(f"pages={len(doc)}")


if __name__ == "__main__":
    main()
