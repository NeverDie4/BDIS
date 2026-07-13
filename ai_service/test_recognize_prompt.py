from pathlib import Path
import unittest


class RecognizePromptTest(unittest.TestCase):

  def test_prompt_requires_non_empty_reason_and_suggestion(self):
    source = Path(__file__).with_name("recognize_app.py").read_text(encoding="utf-8")

    self.assertIn("reason 和 suggestion 必须返回且不能为空", source)
    self.assertIn("reason 必须说明图片中的具体判断依据", source)
    self.assertIn("suggestion 必须给出下一步复核建议", source)

  def test_terminal_logs_include_timing_and_result_details(self):
    source = Path(__file__).with_name("recognize_app.py").read_text(encoding="utf-8")

    for label in (
        "[识别开始]",
        "[识别完成]",
        "[识别失败]",
        "大模型耗时",
        "总耗时",
        "识别结果",
        "置信度",
        "理由",
        "建议",
    ):
      self.assertIn(label, source)
    self.assertIn('sys.stdout.reconfigure(encoding="utf-8")', source)
    self.assertIn('sys.stderr.reconfigure(encoding="utf-8")', source)


if __name__ == "__main__":
  unittest.main()
