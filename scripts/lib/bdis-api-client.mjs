export function parseArgs(argv = process.argv.slice(2)) {
  const args = {};
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith("--")) continue;
    const key = token.slice(2);
    const value = argv[index + 1];
    if (value && !value.startsWith("--")) {
      args[key] = value;
      index += 1;
    } else {
      args[key] = true;
    }
  }
  return args;
}

export function createClient(baseUrl) {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");

  async function request(
    path,
    { method = "GET", token, body, raw = false } = {},
  ) {
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined)
      headers["Content-Type"] = "application/json; charset=utf-8";
    const response = await fetch(`${normalizedBaseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (raw) return response;
    const text = await response.text();
    const payload = text ? JSON.parse(text) : null;
    if (
      !response.ok ||
      (payload?.code !== undefined &&
        payload.code !== "SUCCESS" &&
        payload.code !== 200)
    ) {
      throw new Error(
        payload?.message || payload?.msg || `HTTP ${response.status}`,
      );
    }
    return payload?.data ?? payload;
  }

  async function login(username, password) {
    const session = await request("/auth/sessions", {
      method: "POST",
      body: { username, password },
    });
    if (!session?.accessToken)
      throw new Error(`账号 ${username} 登录后未返回 accessToken`);
    return session.accessToken;
  }

  return { login, request };
}

export function requirePositiveId(value, name) {
  const id = Number(value);
  if (!Number.isSafeInteger(id) || id <= 0)
    throw new Error(`${name} 必须是正整数`);
  return id;
}

export function assertStatus(record, expected, action) {
  const actual = record?.auditStatus ?? record?.reviewStatus;
  if (actual !== expected) {
    throw new Error(
      `${action}后状态应为 ${expected}，实际为 ${actual ?? "空"}`,
    );
  }
}

export function step(title) {
  process.stdout.write(`\n== ${title} ==\n`);
}
