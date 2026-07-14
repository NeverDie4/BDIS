import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";

const root = path.resolve(import.meta.dirname, "../..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("reviewer scope repair only targets rows created by the faulty migration", () => {
  const grant = read(
    "backend/src/main/resources/db/migration/V20260714_006__grant_growth_reviewer_data_scope.sql",
  );
  const repair = read(
    "backend/src/main/resources/db/migration/V20260714_008__restore_growth_reviewer_data_scope.sql",
  );
  const currentMarker = "growth-reviewer-scope-migration-20260714-006";
  const faultyMarker = "Reviewers can access growth records pending review";

  assert.match(grant, new RegExp(currentMarker));
  assert.match(repair, new RegExp(`remark.{0,80}${faultyMarker}`, "is"));
  assert.doesNotMatch(
    repair,
    new RegExp(`remark.{0,80}${currentMarker}`, "is"),
  );
  assert.match(repair, /scope_type`\s*=\s*'all'/i);
});

test("anonymous growth trace contract excludes internal review comments and event content", () => {
  const audit = read(
    "backend/src/main/java/com/bdis/modules/growth/vo/GrowthPublicAuditVO.java",
  );
  const event = read(
    "backend/src/main/java/com/bdis/modules/growth/vo/GrowthPublicTraceEventVO.java",
  );
  const archive = read(
    "backend/src/main/java/com/bdis/modules/growth/vo/GrowthPublicTraceArchiveVO.java",
  );
  const service = read(
    "backend/src/main/java/com/bdis/modules/growth/service/impl/GrowthRecordServiceImpl.java",
  );
  const page = read("frontend/src/app/trace/growth/[traceCode]/page.tsx");

  assert.doesNotMatch(audit, /private\s+String\s+comment\s*;/);
  assert.doesNotMatch(event, /private\s+String\s+eventContent\s*;/);
  assert.doesNotMatch(archive, /private\s+String\s+latestAuditComment\s*;/);
  const publicMappingStart = service.indexOf(
    "private GrowthPublicAuditVO toPublicAudit",
  );
  const publicMappingEnd = service.indexOf(
    "private void applyDataScope",
    publicMappingStart,
  );
  const publicMappings = service.slice(publicMappingStart, publicMappingEnd);

  assert.doesNotMatch(publicMappings, /setComment\(/);
  assert.doesNotMatch(publicMappings, /setEventContent\(/);
  assert.match(
    service,
    /private GrowthTraceEventVO toTraceEventVO[\s\S]*?vo\.setEventContent\(event\.getEventContent\(\)\)/,
  );
  assert.doesNotMatch(
    page,
    /latestAuditComment|event\.eventContent|event\.comment/,
  );
});

test("docker deployment requires an externally reachable public Web base URL", () => {
  const compose = read("docker-compose.yml");
  const envExample = read(".env.example");

  assert.match(
    compose,
    /BDIS_PUBLIC_WEB_BASE_URL:\s*\$\{BDIS_PUBLIC_WEB_BASE_URL:\?[^}]+\}/,
  );
  assert.match(
    envExample,
    /^BDIS_PUBLIC_WEB_BASE_URL=https?:\/\/(?!localhost\b).+/m,
  );
});

test("collection task creation requires a scoped collector selection", () => {
  const controller = read(
    "backend/src/main/java/com/bdis/modules/collection/controller/HerbCollectionTaskController.java",
  );
  const request = read(
    "backend/src/main/java/com/bdis/modules/collection/dto/HerbCollectionTaskCreateRequest.java",
  );
  const access = read(
    "backend/src/main/java/com/bdis/modules/collection/support/CollectionAccessService.java",
  );

  assert.match(request, /@NotNull\(message = "请选择采集员"\)\s*private Long collectorId;/);
  assert.match(controller, /@GetMapping\("\/assignable-collectors"\)/);
  assert.match(controller, /@RequirePermission\("growth:record:create"\)/);
  assert.match(controller, /collectionAccessService\.listAssignableCollectors\(\)/);
  assert.match(access, /listAssignableCollectors/);
  assert.match(access, /RoleEntity::getRoleCode, "COLLECTOR"/);
  assert.match(access, /CollectionAccessScope scope = currentScope\(\)/);
  assert.match(access, /public String requireAssignableCollector\(Long collectorId\)/);
  assert.match(access, /UserRoleEntity::getUserId, collectorId/);
  const service = read("backend/src/main/java/com/bdis/modules/collection/service/impl/HerbCollectionTaskServiceImpl.java");
  assert.match(service, /String collectorName =\s*collectionAccessService\.requireAssignableCollector/);
  assert.match(service, /setCollectorName\(collectorName\)/);
});