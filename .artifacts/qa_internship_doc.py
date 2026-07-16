from __future__ import annotations

import re
import zipfile
from pathlib import Path

from docx import Document
from PIL import Image
from pypdf import PdfReader


ROOT = Path(__file__).resolve().parent.parent
DOCX = ROOT / "8209230308-刘麒源-BDIS实训材料填写稿.docx"
PDF = ROOT / ".artifacts" / "render-word-v3" / "8209230308-刘麒源-BDIS实训材料填写稿.pdf"
PAGES = ROOT / ".artifacts" / "render-word-v2" / "pages"


with zipfile.ZipFile(DOCX) as archive:
    bad_member = archive.testzip()

document = Document(DOCX)
paragraphs = [paragraph.text.strip() for paragraph in document.paragraphs if paragraph.text.strip()]
full_text = "\n".join(paragraphs)

date_pattern = re.compile(r"^2026年7月(\d{1,2})日（星期[一二三四五六日]）$")
diary_days = [int(match.group(1)) for text in paragraphs if (match := date_pattern.match(text))]

summary_start = paragraphs.index("一、实习鉴定表：个人总结与自我鉴定")
diary_start = paragraphs.index("二、学生实训日志：2026年7月8日至7月16日")
experience_start = paragraphs.index("三、实训体会表：实训体会")
summary_text = "".join(paragraphs[summary_start + 1 : diary_start])
experience_text = "".join(paragraphs[experience_start + 1 :])

reader = PdfReader(PDF)
pdf_text = "\n".join((page.extract_text() or "") for page in reader.pages)

image_results = []
for path in sorted(PAGES.glob("page-*.png")):
    image = Image.open(path).convert("RGB")
    total = image.width * image.height
    nearly_black = sum(
        1
        for red, green, blue in image.getdata()
        if red < 40 and green < 40 and blue < 40
    )
    image_results.append((path.name, nearly_black / total))

expected_days = list(range(8, 17))
missing_pdf_days = [day for day in expected_days if f"7 月 {day} 日" not in pdf_text and f"7月{day}日" not in pdf_text]

print(f"docx_zip_bad_member={bad_member}")
print(f"paragraph_count={len(paragraphs)}")
print(f"diary_days={diary_days}")
print(f"diary_days_ok={diary_days == expected_days}")
print(f"name_count={full_text.count('刘麒源')}")
print(f"student_id_count={full_text.count('8209230308')}")
print(f"summary_chars={len(summary_text)}")
print(f"experience_chars={len(experience_text)}")
print(f"pdf_pages={len(reader.pages)}")
print(f"pdf_missing_days={missing_pdf_days}")
print(f"rendered_png_pages={len(image_results)}")
print(f"max_nearly_black_ratio={max(ratio for _, ratio in image_results):.3%}")
print(f"headings_ok={all(title in full_text for title in ['一、实习鉴定表：个人总结与自我鉴定', '二、学生实训日志：2026年7月8日至7月16日', '三、实训体会表：实训体会'])}")
