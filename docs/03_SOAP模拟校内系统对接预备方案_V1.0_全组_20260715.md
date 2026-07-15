# BDIS SOAP 模拟校内系统对接预备方案

> 版本：V1.0
> 日期：2026-07-15
> 负责人：全组
> 状态：已实现（本地 mock 演示链路）

## 1. 目标与边界

本阶段用于证明 BDIS 具备通过 SOAP 与校内系统交换数据的能力。由于当前没有可用的真实校内系统，本阶段以本地模拟校内 SOAP 服务替代真实校内服务。

本次只实现一条可演示的入站链路：

```text
BDIS 管理接口创建同步任务
  -> BDIS SOAP 客户端通过 HTTP 调用本地模拟校内 SOAP 服务
  -> 获得 SOAP 响应
  -> 解析、校验并导入一条生长采集记录
  -> 写 SOAP 交换记录、数据同步日志和操作审计日志
```

范围固定如下：

| 项目      | 本次范围                                                                       |
| --------- | ------------------------------------------------------------------------------ |
| 数据方向  | 校内系统到 BDIS 的入站同步，由 BDIS 主动拉取。                                 |
| 资源类型  | 仅 `GROWTH_RECORD`。                                                           |
| SOAP 版本 | SOAP 1.1，HTTP，Document/Literal。                                             |
| 调用方式  | 同步调用；不做定时调度、消息队列和异步回调。                                   |
| 校内系统  | 后端同一进程内发布的本地模拟 SOAP 服务，端点为 `/api/services/campus-growth`。 |
| 认证      | 本地 mock 不启用真实凭据；真实环境的认证方式待校方提供。                       |
| 前端      | 不新增独立管理页面；通过既有 REST 任务接口和 Swagger/Postman 演示。            |

明确不做用户、课程、业绩等其他资源同步，不做双向同步、WS-Security、真实校内地址接入、批量分页、定时补偿或复杂重试策略。

## 2. 现有实现与改造依据

当前后端已有 M19 的导入与留痕能力：

- `POST /api/soap-exchange-jobs` 可创建同步任务，`GET` 接口可查询任务、交换详情并重试。
- 已有 `soap_sync_task`、`soap_exchange_record`、数据同步日志和审计日志。
- `SoapXmlParser` 已采用禁用外部实体的安全 XML 解析配置。
- `GrowthRecordSoapImportHandler` 会按药材、基地、地图点位匹配后调用生长采集 Service 入库，来源标记为 `SOAP`，并使用 `externalNo` 防止重复导入。

当前实现已由 `SoapClient` 通过 HTTP 调用同进程内发布的本地模拟 SOAP 服务；任务、导入、交换记录、数据同步日志和审计日志沿用既有链路。冻结版不支持切换为真实校内地址。

## 3. 最小 SOAP 契约

### 3.1 服务定义

| 项目       | 约定                                     |
| ---------- | ---------------------------------------- |
| 服务名     | `CampusGrowthDataService`                |
| 端口名     | `CampusGrowthDataPort`                   |
| 命名空间   | `https://bdis.local/ws/campus-growth/v1` |
| 操作       | `queryGrowthRecords`                     |
| SOAPAction | `queryGrowthRecords`                     |
| 版本       | `v1`，发布后不修改既有字段语义。         |

`queryGrowthRecords` 返回本地模拟校内系统当前可提供的一条生长采集记录。冻结版收到多于一条 `record` 时会明确标记任务失败并保留原始报文，绝不静默丢弃后续记录；测试时可通过明确的 mock 场景返回业务失败或 SOAP Fault。

### 3.2 请求报文

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:camp="https://bdis.local/ws/campus-growth/v1">
  <soapenv:Header/>
  <soapenv:Body>
    <camp:queryGrowthRecordsRequest>
      <camp:requestId>BDIS-SOAP-DEMO-001</camp:requestId>
      <camp:since>2026-07-15T00:00:00</camp:since>
    </camp:queryGrowthRecordsRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

| 字段        | 必填 | 规则                                                |
| ----------- | ---- | --------------------------------------------------- |
| `requestId` | 是   | BDIS 生成的请求追踪号，最大 64 字符。               |
| `since`     | 否   | ISO-8601 本地日期时间；本次 mock 可忽略该筛选条件。 |

### 3.3 成功响应与字段映射

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:camp="https://bdis.local/ws/campus-growth/v1">
  <soapenv:Body>
    <camp:queryGrowthRecordsResponse>
      <camp:code>SUCCESS</camp:code>
      <camp:message>ok</camp:message>
      <camp:records>
        <camp:record>
          <camp:externalNo>CAMPUS-DEMO-GROWTH-001</camp:externalNo>
          <camp:herbName>SOAP演示黄连</camp:herbName>
          <camp:baseName>SOAP演示重庆基地</camp:baseName>
          <camp:collectorName>校内系统模拟账号</camp:collectorName>
          <camp:collectedAt>2026-07-15T10:30:00</camp:collectedAt>
          <camp:sourceType>SOAP</camp:sourceType>
        </camp:record>
      </camp:records>
    </camp:queryGrowthRecordsResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

| 校内 SOAP 字段  | BDIS 字段/用途                    | 规则                                                   |
| --------------- | --------------------------------- | ------------------------------------------------------ |
| `externalNo`    | 外部幂等键                        | 必填、最大 100 字符；相同值不得重复创建采集记录。      |
| `herbName`      | 药材名称匹配                      | 必填；必须命中启用且未删除的药材。                     |
| `baseName`      | 基地与地图点位匹配                | 必填；必须命中基地及该基地对应药材的地图点位。         |
| `collectorName` | 采集人名称快照                    | 可选；不映射为本地用户。                               |
| `collectedAt`   | `herb_growth_record.collected_at` | 可选；格式错误时导入失败，缺失时由业务层使用当前时间。 |
| `sourceType`    | `data_source`                     | 固定为 `SOAP`；外部传入其他值不改变本次来源口径。      |

### 3.4 失败约定

- 参数或校内业务失败：返回响应体 `code != SUCCESS`，BDIS 将任务和交换记录标记为 `FAILED` 并保存错误原因。
- 不可解析报文、服务端异常：返回标准 SOAP Fault；BDIS 保存 Fault 摘要，不把堆栈或敏感内容返回给前端。
- BDIS 的药材、基地、地图点位匹配失败：这是本地业务导入失败，不伪造为校内 SOAP 服务成功；记录错误并允许从既有 REST 接口重试。
- 成功任务才会推进 `lastSyncAt`；失败任务重试时沿用上一次成功游标，避免遗漏失败窗口的数据。

## 4. 本地演示数据

演示依赖数据必须以独立 SQL 脚本维护，不放入 Flyway migration，也不自动随应用启动插入。脚本为：

```text
scripts/dev-soap-seed.sql
```

该脚本使用稳定业务编号 `DEV_SOAP_HUANGLIAN`、`DEV_SOAP_CHONGQING_BASE`，并只创建/修复下列本地演示前置数据：

| 数据     | 名称                  | 用途                                             |
| -------- | --------------------- | ------------------------------------------------ |
| 药材     | `SOAP演示黄连`        | 对应 SOAP 响应的 `herbName`。                    |
| 基地     | `SOAP演示重庆基地`    | 对应 SOAP 响应的 `baseName`。                    |
| 地图点位 | `SOAP 演示黄连采集点` | 使现有生长采集导入逻辑能完成基地和药材联合匹配。 |

脚本必须可重复运行，不创建业务采集记录，不写账号、不写密码，也不删除非本脚本创建的数据。执行对象仅限本地开发数据库；执行前先完成 Flyway 迁移。

## 5. 配置与安全约定

本地 mock 使用以下配置名；冻结版端点必须是回环 HTTP 地址，不读取或接入真实校内地址及凭据：

| 配置                           | 本地演示值                              | 说明                           |
| ------------------------------ | --------------------------------------- | ------------------------------ |
| `BDIS_SOAP_MODE`               | 默认 `disabled`；演示时显式设为 `mock`  | 仅 `mock` 会发布本地模拟端点。 |
| `BDIS_SOAP_CAMPUS_ENDPOINT`    | 本地 `/api/services/campus-growth` 地址 | SOAP 服务端点。                |
| `BDIS_SOAP_CONNECT_TIMEOUT_MS` | `3000`                                  | 连接超时。                     |
| `BDIS_SOAP_READ_TIMEOUT_MS`    | `5000`                                  | 响应超时。                     |

请求、响应和错误摘要可写入现有交换记录以便演示追溯；真实环境接入前必须另行确认账号认证、脱敏字段、日志保留期和访问权限。不得提交真实校内地址、账号、密码、证书或 Token。

本地 mock 服务仅允许回环地址访问，避免无认证的演示端点暴露到局域网；BDIS 自身通过 `localhost` 调用该端点。

## 6. 实施顺序与验收

本次已按以下顺序完成实现：

1. 基于本契约创建 WSDL/XSD 和本地模拟 SOAP 服务端；WSDL 为生成/联调唯一依据。
2. 将现有 `SoapClient` 改为通过 HTTP 调用该端点，保留现有任务、导入和日志 Service。
3. 让 `mock` 模式显式选择本地模拟端点；默认 `disabled`，非 mock 模式明确拒绝，不能静默回退为假数据。
4. 接入 `scripts/dev-soap-seed.sql` 中的前置数据，覆盖成功、SOAP Fault、导入匹配失败、重复 `externalNo`、多记录拒绝和失败游标保持等测试。
5. 同步更新 REST 接口文档、WSDL 地址、演示步骤和已知限制。

### 6.1 一键本地演示

先显式开启本地 mock，再启动后端、本地 MySQL 和 Redis；Mock 默认关闭，Docker Compose 也不会默认发布该演示端点：

```bash
export BDIS_SOAP_MODE=mock
pnpm dev:api

# 在另一终端提供具备 soap:exchange:view、soap:exchange:execute 权限的本地访问令牌
export BDIS_DEMO_TOKEN='本地访问令牌'
pnpm demo:soap
```

也可设置 `BDIS_DEMO_USERNAME`、`BDIS_DEMO_PASSWORD` 由脚本登录。脚本会校验 API 与 SOAP 端点均为本机 HTTP 地址，并要求显式 `BDIS_SOAP_MODE=mock`；仅允许对名称以 `_dev` 结尾的本地数据库执行 [scripts/dev-soap-seed.sql](../scripts/dev-soap-seed.sql)。默认种子步骤需要本机可用的 MySQL/MariaDB 客户端；若已手工执行演示数据脚本，可使用 `pnpm demo:soap -- --skip-seed` 跳过数据准备。它不会启动服务、创建账号、连接真实校内地址或共享数据库。

冻结前的验收标准：

1. 可访问本地模拟服务的 WSDL：`/api/services/campus-growth?wsdl`。
2. 创建 `GROWTH_RECORD` 同步任务后，交换记录中能看到真实 SOAP 请求和响应。
3. 成功结果能生成一条来源为 `SOAP` 的生长采集记录。
4. SOAP Fault 与本地导入失败均能留下失败原因；失败任务可重试。
5. 相同 `externalNo` 重复执行不新增第二条采集记录。
6. 多记录响应不会静默遗漏；任务失败后重试不推进或跳过上次成功游标。

## 7. 冻结版限制与后续对接条件

本方案只证明协议链路和业务导入能力，不代表已与真实校内系统完成联调。SOAP 导入记录不伪造关联到采集任务或采集批次；它可在地图点位详情的生长记录中以 `SOAP` 来源查看。真实接入前必须由校方提供 WSDL、环境地址、接口账号/认证方式、字段字典、错误码、调用频率和联调窗口；届时新增适配器实现，不修改本次已冻结的业务导入契约。
