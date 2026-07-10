"use client";

import { Card, Tree, Typography } from "antd";
import type { DataNode, TreeProps } from "antd/es/tree";
import styles from "./PermissionTree.module.css";

type PermissionTreeProps = {
  treeData?: DataNode[];
  checkedKeys?: TreeProps["checkedKeys"];
  defaultCheckedKeys?: TreeProps["defaultCheckedKeys"];
  expandedKeys?: TreeProps["expandedKeys"];
  defaultExpandedKeys?: TreeProps["defaultExpandedKeys"];
  checkable?: boolean;
  title?: string;
  description?: string;
  onCheck?: TreeProps["onCheck"];
  className?: string;
};

const DEFAULT_PERMISSION_TREE: DataNode[] = [
  {
    title: "菜单权限",
    key: "menu",
    children: [
      { title: "中药材资源", key: "menu:herbs" },
      { title: "分布地图", key: "menu:map" },
      { title: "生长数据", key: "menu:growth" },
    ],
  },
  {
    title: "按钮权限",
    key: "button",
    children: [
      { title: "新增药材", key: "button:herb:create" },
      { title: "导出资料", key: "button:file:export" },
      { title: "提交审核", key: "button:growth:submit" },
    ],
  },
  {
    title: "接口权限",
    key: "api",
    children: [
      { title: "药材查询接口", key: "api:herb:list" },
      { title: "采集记录接口", key: "api:growth:list" },
      { title: "课程资源接口", key: "api:course:list" },
    ],
  },
];

export function PermissionTree({
  treeData = DEFAULT_PERMISSION_TREE,
  checkedKeys,
  defaultCheckedKeys,
  expandedKeys,
  defaultExpandedKeys = ["menu", "button", "api"],
  checkable = true,
  title = "角色权限范围",
  description = "按菜单、按钮、接口三类展示权限，后续接入登录用户与角色配置。",
  onCheck,
  className,
}: PermissionTreeProps) {
  return (
    <Card className={`${styles.card} ${className ?? ""}`} variant="borderless">
      <Typography.Title level={2}>{title}</Typography.Title>
      <Typography.Paragraph>{description}</Typography.Paragraph>
      <Tree
        checkable={checkable}
        checkedKeys={checkedKeys}
        defaultCheckedKeys={defaultCheckedKeys}
        defaultExpandedKeys={defaultExpandedKeys}
        expandedKeys={expandedKeys}
        treeData={treeData}
        onCheck={onCheck}
      />
    </Card>
  );
}
