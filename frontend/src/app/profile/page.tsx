"use client";

import { Avatar, Button, Card, Descriptions, Modal, Tabs, Typography } from "antd";
import type { TableProps } from "antd";
import { UserOutlined } from "@ant-design/icons";
import { useMemo, useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DataTable } from "@/components/common/DataTable";
import { InfoCard } from "@/components/common/InfoCard";
import { MetricCard } from "@/components/common/MetricCard";
import { SearchBar } from "@/components/common/SearchBar";
import { StatusTag } from "@/components/common/StatusTag";
import { PermissionTree } from "@/components/feature/PermissionTree";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { dataScopes, fileAccessLogs, loginLogs, operationLogs, permissionTree, roles, users } from "@/mocks/users";
import type { FileAccessLog, LoginLog, OperationLog, Permission, Role, SystemUser } from "@/types/user";
import styles from "@/styles/mockPages.module.css";

function countPermissions(nodes: Permission[]): number {
  return nodes.reduce((total, node) => total + 1 + countPermissions(node.children ?? []), 0);
}

export default function ProfilePage() {
  const [keyword, setKeyword] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const currentUser = users[1];

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      return (
        keyword.length === 0 ||
        user.realName.includes(keyword) ||
        user.username.includes(keyword) ||
        user.department.includes(keyword)
      );
    });
  }, [keyword]);

  const userColumns: TableProps<SystemUser>["columns"] = [
    { title: "用户名", dataIndex: "username", key: "username" },
    { title: "姓名", dataIndex: "realName", key: "realName" },
    { title: "部门", dataIndex: "department", key: "department" },
    { title: "角色", key: "roles", render: (_, record) => record.roles.join("、") },
    { title: "状态", key: "status", render: (_, record) => <StatusTag status={record.status} /> },
  ];

  const roleColumns: TableProps<Role>["columns"] = [
    { title: "角色编码", dataIndex: "roleCode", key: "roleCode" },
    { title: "角色名称", dataIndex: "roleName", key: "roleName" },
    { title: "数据范围", dataIndex: "dataScope", key: "dataScope" },
    { title: "状态", key: "status", render: (_, record) => <StatusTag status={record.status} /> },
  ];

  const operationColumns: TableProps<OperationLog>["columns"] = [
    { title: "操作人", dataIndex: "operator", key: "operator" },
    { title: "模块", dataIndex: "moduleName", key: "moduleName" },
    { title: "动作", dataIndex: "action", key: "action" },
    { title: "对象", dataIndex: "targetName", key: "targetName" },
    { title: "结果", dataIndex: "result", key: "result" },
  ];

  const loginColumns: TableProps<LoginLog>["columns"] = [
    { title: "账号", dataIndex: "username", key: "username" },
    { title: "姓名", dataIndex: "realName", key: "realName" },
    { title: "登录时间", dataIndex: "loginTime", key: "loginTime" },
    { title: "设备", dataIndex: "device", key: "device" },
    { title: "结果", dataIndex: "result", key: "result" },
  ];

  const fileColumns: TableProps<FileAccessLog>["columns"] = [
    { title: "用户", dataIndex: "username", key: "username" },
    { title: "文件", dataIndex: "fileName", key: "fileName" },
    { title: "模块", dataIndex: "businessModule", key: "businessModule" },
    { title: "操作", dataIndex: "accessType", key: "accessType" },
    { title: "时间", dataIndex: "accessTime", key: "accessTime" },
  ];

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="PERSONAL DESK"
          title="个人主页"
          subtitle="展示当前用户、角色权限、系统配置和日志审计入口，第一版使用 mock 数据支撑演示。"
        />

        <section className={styles.contentGrid}>
          <Card className={styles.panel} variant="borderless">
            <div className={styles.panelBody}>
              <Avatar icon={<UserOutlined />} size={72} />
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="姓名">{currentUser.realName}</Descriptions.Item>
                <Descriptions.Item label="账号">{currentUser.username}</Descriptions.Item>
                <Descriptions.Item label="部门">{currentUser.department}</Descriptions.Item>
                <Descriptions.Item label="角色">{currentUser.roles.join("、")}</Descriptions.Item>
                <Descriptions.Item label="数据范围">{currentUser.dataScope}</Descriptions.Item>
              </Descriptions>
            </div>
          </Card>

          <div className={styles.metricGrid}>
            <MetricCard description="系统用户 mock 列表。" title="用户数" value={users.length} />
            <MetricCard description="角色与数据范围配置。" title="角色数" value={roles.length} />
            <MetricCard description="菜单、按钮、接口权限。" title="权限节点" value={countPermissions(permissionTree)} />
            <MetricCard description="操作、登录、文件访问。" title="日志数" value={operationLogs.length + loginLogs.length + fileAccessLogs.length} />
          </div>
        </section>

        <ActionToolbar
          actions={
            <div className={styles.toolbarActions}>
              <Button onClick={() => setModalOpen(true)}>系统配置</Button>
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增用户
              </Button>
            </div>
          }
          description="用户、角色、权限和日志均为前端 mock 展示。"
          title="系统管理概览"
        />

        <InfoCard title="用户管理">
          <div className={styles.sectionStack}>
            <SearchBar placeholder="搜索姓名、账号或部门" value={keyword} onChange={setKeyword} />
            <DataTable<SystemUser> columns={userColumns} dataSource={filteredUsers} pagination={false} rowKey="id" />
          </div>
        </InfoCard>

        <section className={styles.contentGrid}>
          <InfoCard title="角色权限管理">
            <DataTable<Role> columns={roleColumns} dataSource={roles} pagination={false} rowKey="id" />
          </InfoCard>
          <PermissionTree />
        </section>

        <div className={styles.twoGrid}>
          <InfoCard title="数据范围">
            <ul className={styles.compactList}>
              {dataScopes.map((scope) => (
                <li key={scope.value}>{scope.label}：{scope.description}</li>
              ))}
            </ul>
          </InfoCard>
          <InfoCard title="系统配置占位">
            <ul className={styles.compactList}>
              <li>登录 Token 与用户信息后续接入 Zustand。</li>
              <li>菜单权限后续由登录接口返回并动态裁剪。</li>
              <li>文件访问与操作审计后续接入统一日志接口。</li>
            </ul>
          </InfoCard>
        </div>

        <Tabs
          items={[
            {
              key: "operation",
              label: "操作日志",
              children: <DataTable<OperationLog> columns={operationColumns} dataSource={operationLogs} pagination={false} rowKey="id" />,
            },
            {
              key: "login",
              label: "登录日志",
              children: <DataTable<LoginLog> columns={loginColumns} dataSource={loginLogs} pagination={false} rowKey="id" />,
            },
            {
              key: "file",
              label: "文件访问日志",
              children: <DataTable<FileAccessLog> columns={fileColumns} dataSource={fileAccessLogs} pagination={false} rowKey="id" />,
            },
          ]}
        />
      </div>

      <Modal footer={null} open={modalOpen} title="系统管理操作占位" onCancel={() => setModalOpen(false)}>
        <Typography.Paragraph className={styles.mutedText}>
          用户新增、系统配置和权限保存将在登录权限与后端接口接入后实现。
        </Typography.Paragraph>
      </Modal>
    </SiteLayout>
  );
}
