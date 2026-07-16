import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

async function readSource(relativePath) {
  return readFile(new URL(relativePath, root), "utf8");
}

test("course editor submits structured prerequisites and tags", async () => {
  const source = await readSource("components/teaching/CourseEditorModal.tsx");

  assert.match(source, /name="prerequisiteCourseIds"/);
  assert.match(source, /mode="multiple"/);
  assert.match(source, /name="tags"/);
  assert.match(source, /mode="tags"/);
  assert.match(source, /prerequisiteCourseIds:\s*values\.prerequisiteCourseIds/);
});

test("course detail separates files videos and editable relations", async () => {
  const source = await readSource("components/teaching/CourseDetailPanel.tsx");

  assert.match(source, /const renderedDetailItems = detailItems\.map/);
  assert.match(source, /course\.detail\.resources\.length === 0/);
  assert.match(source, /course\.detail\.videos\.map/);
  assert.match(source, /getCourseRelationOptions/);
  assert.match(source, /updateCourseRelations/);
  assert.match(source, /item\.key === "relations"[\s\S]*?编辑关联/);
  assert.doesNotMatch(source, /<section className=\{styles\.courseSummary\}[\s\S]*?编辑关联[\s\S]*?<Tabs/);
});

test("course data layer calls relation APIs and splits video resources", async () => {
  const source = await readSource("lib/courses.ts");

  assert.match(source, /\/courses\/\$\{courseId\}\/relation-options/);
  assert.match(source, /\/courses\/\$\{courseId\}\/relations/);
  assert.match(source, /resource\.resourceType !== "video"/);
  assert.match(source, /prerequisiteCourseIds/);
});
