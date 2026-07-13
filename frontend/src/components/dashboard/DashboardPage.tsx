"use client";

import { getApiErrorMessage } from "@/lib/request";
import { App, Button, Form, Modal } from "antd";
import type { FormInstance } from "antd";
import type { LucideIcon } from "lucide-react";
import { useState } from "react";
import styles from "./DashboardPage.module.css";

export function DashboardPage({
  eyebrow,
  title,
  description,
  actions,
  metrics,
  children,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: React.ReactNode;
  metrics?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.heading}>
          <p className={styles.eyebrow}>{eyebrow}</p>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.description}>{description}</p>
        </div>
        {actions ? <div className={styles.actions}>{actions}</div> : null}
      </header>
      {metrics ? <div className={styles.metrics}>{metrics}</div> : null}
      {children}
    </main>
  );
}

export function DashboardMetric({
  icon: Icon,
  label,
  value,
}: {
  icon: LucideIcon;
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className={styles.metric}>
      <span className={styles.metricLabel}>
        <Icon className={styles.metricIcon} size={16} />
        {label}
      </span>
      <strong className={styles.metricValue}>{value}</strong>
    </div>
  );
}

export function DashboardPanel({
  title,
  description,
  actions,
  flush = false,
  children,
}: {
  title?: string;
  description?: string;
  actions?: React.ReactNode;
  flush?: boolean;
  children: React.ReactNode;
}) {
  return (
    <section className={styles.panel}>
      {title || actions ? (
        <div className={styles.panelHeader}>
          <div className={styles.panelHeading}>
            {title ? <h2 className={styles.panelTitle}>{title}</h2> : null}
            {description ? <p className={styles.panelDescription}>{description}</p> : null}
          </div>
          {actions ? <div className={styles.actions}>{actions}</div> : null}
        </div>
      ) : null}
      <div className={flush ? styles.panelBodyFlush : styles.panelBody}>{children}</div>
    </section>
  );
}

export function DashboardStatus({ enabled }: { enabled: boolean }) {
  return (
    <span className={`${styles.status} ${enabled ? styles.statusEnabled : styles.statusDisabled}`}>
      <span className={styles.statusDot} />
      {enabled ? "启用" : "停用"}
    </span>
  );
}

export function DashboardFormModal<Values extends object>({
  title,
  open,
  form,
  submitText = "保存",
  errorMessage = "保存失败",
  onCancel,
  onFinish,
  children,
}: {
  title: string;
  open: boolean;
  form: FormInstance<Values>;
  submitText?: string;
  errorMessage?: string;
  onCancel: () => void;
  onFinish: (values: Values) => Promise<void>;
  children: React.ReactNode;
}) {
  const { message } = App.useApp();
  const [submitting, setSubmitting] = useState(false);

  async function submit(values: Values) {
    setSubmitting(true);
    try {
      await onFinish(values);
    } catch (error) {
      message.error(getApiErrorMessage(error, errorMessage));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      title={title}
      open={open}
      onCancel={onCancel}
      footer={null}
      forceRender
      maskClosable={!submitting}
      closable={!submitting}
    >
      <Form<Values> form={form} layout="vertical" onFinish={submit}>
        {children}
        <div className={styles.modalActions}>
          <Button onClick={onCancel} disabled={submitting}>
            取消
          </Button>
          <Button type="primary" htmlType="submit" loading={submitting}>
            {submitText}
          </Button>
        </div>
      </Form>
    </Modal>
  );
}
