import {
  assertStatus,
  createClient,
  parseArgs,
  requirePositiveId,
  step,
} from "./lib/bdis-api-client.mjs";

const args = parseArgs();
const baseUrl = args["base-url"] || process.env.BDIS_API_BASE_URL || "http://localhost:8080/api";
const batchIdValue = args["batch-id"] || process.env.BDIS_TEST_BATCH_ID;
if (!batchIdValue) {
  throw new Error(
    "请通过 --batch-id 或 BDIS_TEST_BATCH_ID 提供当前采集员拥有、状态可写且尚未创建生长记录的批次 ID",
  );
}

const batchId = requirePositiveId(batchIdValue, "batchId");
const client = createClient(baseUrl);
const credentials = {
  collector: {
    username: args["collector-username"] || process.env.BDIS_COLLECTOR_USERNAME || "collector",
    password: args["collector-password"] || process.env.BDIS_COLLECTOR_PASSWORD || "password",
  },
  reviewer: {
    username: args["reviewer-username"] || process.env.BDIS_REVIEWER_USERNAME || "reviewer",
    password: args["reviewer-password"] || process.env.BDIS_REVIEWER_PASSWORD || "password",
  },
  admin: {
    username: args["admin-username"] || process.env.BDIS_ADMIN_USERNAME || "admin",
    password: args["admin-password"] || process.env.BDIS_ADMIN_PASSWORD || "password",
  },
};

step("登录采集员、审核员和管理员");
const collectorToken = await client.login(credentials.collector.username, credentials.collector.password);
const reviewerToken = await client.login(credentials.reviewer.username, credentials.reviewer.password);
const adminToken = await client.login(credentials.admin.username, credentials.admin.password);

const createBody = {
  growthStage: "seedling",
  plantHeight: 18.6,
  soilType: "loam",
  soilPh: 6.4,
  temperature: 22.5,
  humidity: 66,
  soilMoisture: 38,
  light: 12500,
  stemDiameter: 4.8,
  leafColor: "绿色",
  floweringStatus: "未开花",
  growthEvaluation: "长势正常",
  sampleWeight: 12.3,
  dataSource: "integration-test",
  remark: "跨平台审核链路联调记录",
};

step("按所属批次创建生长记录");
const created = await client.request(`/herb/batch/${batchId}/growth-record`, {
  method: "POST",
  token: collectorToken,
  body: createBody,
});
const recordId = requirePositiveId(created?.id, "recordId");
assertStatus(created, "draft", "创建");

step("提交审核并读取历史");
assertStatus(
  await client.request(`/growth-records/${recordId}/submit`, {
    method: "PUT",
    token: collectorToken,
  }),
  "submitted",
  "提交",
);
await client.request(`/growth-records/${recordId}/audit-history`, { token: collectorToken });
await client.request(`/growth-records/${recordId}/trace-events`, { token: collectorToken });

step("审核驳回、按批次修改并重新提交");
assertStatus(
  await client.request(`/growth-records/${recordId}/reject`, {
    method: "PUT",
    token: reviewerToken,
    body: { comment: "联调测试：请补充采集说明" },
  }),
  "rejected",
  "驳回",
);
assertStatus(
  await client.request(`/herb/batch/${batchId}/growth-record/${recordId}`, {
    method: "PUT",
    token: collectorToken,
    body: { ...createBody, remark: "已补充说明并重新提交" },
  }),
  "rejected",
  "修改",
);
assertStatus(
  await client.request(`/growth-records/${recordId}/submit`, {
    method: "PUT",
    token: collectorToken,
  }),
  "submitted",
  "重新提交",
);

step("审核通过并由管理员归档");
assertStatus(
  await client.request(`/growth-records/${recordId}/approve`, {
    method: "PUT",
    token: reviewerToken,
    body: { comment: "联调测试：数据完整，审核通过" },
  }),
  "approved",
  "审核通过",
);
assertStatus(
  await client.request(`/growth-records/${recordId}/archive`, {
    method: "PUT",
    token: adminToken,
    body: { comment: "联调测试归档" },
  }),
  "archived",
  "归档",
);

const history = await client.request(`/growth-records/${recordId}/audit-history`, {
  token: adminToken,
});
const events = await client.request(`/growth-records/${recordId}/trace-events`, {
  token: adminToken,
});

step("确认归档记录不可再次修改");
let archivedUpdateRejected = false;
try {
  await client.request(`/herb/batch/${batchId}/growth-record/${recordId}`, {
    method: "PUT",
    token: collectorToken,
    body: createBody,
  });
} catch {
  archivedUpdateRejected = true;
}
if (!archivedUpdateRejected) throw new Error("归档记录仍可修改");

process.stdout.write(
  `\n审核链路验证通过：recordId=${recordId}，审核历史=${history?.length ?? 0}，溯源事件=${events?.length ?? 0}\n`,
);
