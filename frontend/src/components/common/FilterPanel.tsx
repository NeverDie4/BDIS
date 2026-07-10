"use client";

import { Button, Card, Space, Typography } from "antd";
import { DownOutlined, ReloadOutlined, SearchOutlined, UpOutlined } from "@ant-design/icons";
import { useState } from "react";
import styles from "./FilterPanel.module.css";

type FilterPanelProps = {
  title?: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
  extra?: React.ReactNode;
  queryText?: string;
  resetText?: string;
  expandable?: boolean;
  defaultExpanded?: boolean;
  loading?: boolean;
  onQuery?: () => void;
  onReset?: () => void;
};

export function FilterPanel({
  title = "筛选条件",
  description,
  children,
  className,
  extra,
  queryText = "查询",
  resetText = "重置",
  expandable = false,
  defaultExpanded = true,
  loading = false,
  onQuery,
  onReset,
}: FilterPanelProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const shouldShowFilters = !expandable || expanded;

  return (
    <Card className={`${styles.panel} ${className ?? ""}`} variant="borderless">
      <div className={styles.header}>
        <div>
          <Typography.Title level={2}>{title}</Typography.Title>
          {description ? <Typography.Paragraph>{description}</Typography.Paragraph> : null}
        </div>
        {extra ? <div className={styles.extra}>{extra}</div> : null}
      </div>

      {shouldShowFilters ? <div className={styles.filters}>{children}</div> : null}

      <Space className={styles.actions} size={10} wrap>
        <Button icon={<SearchOutlined />} loading={loading} type="primary" onClick={onQuery}>
          {queryText}
        </Button>
        <Button icon={<ReloadOutlined />} onClick={onReset}>
          {resetText}
        </Button>
        {expandable ? (
          <Button
            icon={expanded ? <UpOutlined /> : <DownOutlined />}
            type="text"
            onClick={() => setExpanded((current) => !current)}
          >
            {expanded ? "收起筛选" : "展开筛选"}
          </Button>
        ) : null}
      </Space>
    </Card>
  );
}
