"use client";

import { LockKeyhole, UserRound } from "lucide-react";
import { Button, Form, Input, Tabs, Typography } from "antd";
import Image from "next/image";
import { useState } from "react";
import styles from "./LoginFormPanel.module.css";

export type LoginFormValues = {
  username: string;
  password: string;
};

export type BootstrapFormValues = LoginFormValues & {
  bootstrapToken: string;
  realName: string;
  phoneNumber?: string;
  email?: string;
};

type LoginFormPanelProps = {
  loginLoading: boolean;
  bootstrapLoading: boolean;
  onSubmit: (values: LoginFormValues) => Promise<void>;
  onBootstrapSubmit: (values: BootstrapFormValues) => Promise<void>;
};

type LoginFormProps = Pick<LoginFormPanelProps, "loginLoading" | "onSubmit">;
type InitializeAdminFormProps = Pick<
  LoginFormPanelProps,
  "bootstrapLoading" | "onBootstrapSubmit"
>;
type AuthMode = "login" | "bootstrap";

const text = {
  account: "\u8d26\u53f7",
  administrator: "\u521d\u59cb\u5316\u7ba1\u7406\u5458",
  bootstrapToken: "\u521d\u59cb\u5316\u4ee4\u724c",
  createAdministrator: "\u521b\u5efa\u7ba1\u7406\u5458",
  email: "\u90ae\u7bb1",
  enterAccount: "\u8bf7\u8f93\u5165\u8d26\u53f7",
  enterBootstrapToken: "\u8bf7\u8f93\u5165\u521d\u59cb\u5316\u4ee4\u724c",
  enterName: "\u8bf7\u8f93\u5165\u59d3\u540d",
  enterPassword: "\u8bf7\u8f93\u5165\u5bc6\u7801",
  forgotPassword: "\u5fd8\u8bb0\u5bc6\u7801",
  identityProvider: "\u6821\u5185\u7edf\u4e00\u8eab\u4efd\u8ba4\u8bc1",
  login: "\u767b\u5f55",
  name: "\u59d3\u540d",
  password: "\u5bc6\u7801",
  passwordMinimum: "\u5bc6\u7801\u81f3\u5c11 8 \u4f4d",
  phone: "\u624b\u673a\u53f7",
  workbench: "\u8fdb\u5165\u672c\u8349\u6807\u672c\u9986\u5de5\u4f5c\u53f0",
  welcome: "\u6b22\u8fce\u767b\u5f55",
};

export function LoginFormPanel({
  bootstrapLoading,
  loginLoading,
  onBootstrapSubmit,
  onSubmit,
}: LoginFormPanelProps) {
  const [activeMode, setActiveMode] = useState<AuthMode>("login");

  return (
    <section aria-labelledby="login-form-title" className={styles.formPanel}>
      <AuthFrame />
      <div className={styles.controlInner}>
        <AuthHeader />
        <Tabs
          activeKey={activeMode}
          className={styles.modeTabs}
          items={[
            { key: "login", label: text.login },
            { key: "bootstrap", label: text.administrator },
          ]}
          onChange={(key) => setActiveMode(key as AuthMode)}
        />
        <div className={styles.formViewport} key={activeMode}>
          {activeMode === "login" ? (
            <LoginForm loginLoading={loginLoading} onSubmit={onSubmit} />
          ) : (
            <InitializeAdminForm
              bootstrapLoading={bootstrapLoading}
              onBootstrapSubmit={onBootstrapSubmit}
            />
          )}
        </div>
      </div>
      <Image
        alt=""
        aria-hidden="true"
        className={styles.seal}
        height={112}
        sizes="68px"
        src="/images/login/herbal-seal.png"
        width={112}
      />
    </section>
  );
}

function AuthHeader() {
  return (
    <header className={styles.heading}>
      <Typography.Text className={styles.kicker}>BDIS ACCESS PORTAL</Typography.Text>
      <Typography.Title id="login-form-title" level={2}>
        {text.welcome}
      </Typography.Title>
      <Typography.Paragraph>{text.workbench}</Typography.Paragraph>
    </header>
  );
}

function LoginForm({ loginLoading, onSubmit }: LoginFormProps) {
  return (
    <Form<LoginFormValues>
      className={styles.loginFormWrapper}
      layout="vertical"
      onFinish={onSubmit}
      requiredMark={false}
    >
      <Form.Item
        label={text.account}
        name="username"
        rules={[{ required: true, message: text.enterAccount }]}
      >
        <Input
          autoComplete="username"
          disabled={loginLoading}
          prefix={<UserRound aria-hidden="true" size={18} />}
        />
      </Form.Item>
      <Form.Item
        label={text.password}
        name="password"
        rules={[{ required: true, message: text.enterPassword }]}
      >
        <Input.Password
          autoComplete="current-password"
          disabled={loginLoading}
          prefix={<LockKeyhole aria-hidden="true" size={18} />}
        />
      </Form.Item>
      <Button block htmlType="submit" loading={loginLoading} type="primary">
        {text.login}
      </Button>
      <div className={styles.loginOptions}>
        <Button disabled size="small" type="text">
          {text.forgotPassword}
        </Button>
      </div>
      <div className={styles.divider} />
      <Button block disabled type="default">
        {text.identityProvider}
      </Button>
    </Form>
  );
}

function InitializeAdminForm({
  bootstrapLoading,
  onBootstrapSubmit,
}: InitializeAdminFormProps) {
  return (
    <Form<BootstrapFormValues>
      className={styles.initializeGrid}
      layout="vertical"
      onFinish={onBootstrapSubmit}
      requiredMark={false}
    >
      <Form.Item
        className={styles.gridFull}
        label={text.bootstrapToken}
        name="bootstrapToken"
        rules={[{ required: true, message: text.enterBootstrapToken }]}
      >
        <Input.Password disabled={bootstrapLoading} />
      </Form.Item>
      <Form.Item
        label={text.account}
        name="username"
        rules={[{ required: true, message: text.enterAccount }]}
      >
        <Input disabled={bootstrapLoading} />
      </Form.Item>
      <Form.Item
        label={text.password}
        name="password"
        rules={[
          { required: true, message: text.enterPassword },
          { min: 8, message: text.passwordMinimum },
        ]}
      >
        <Input.Password disabled={bootstrapLoading} />
      </Form.Item>
      <Form.Item
        label={text.name}
        name="realName"
        rules={[{ required: true, message: text.enterName }]}
      >
        <Input disabled={bootstrapLoading} />
      </Form.Item>
      <Form.Item label={text.phone} name="phoneNumber">
        <Input disabled={bootstrapLoading} />
      </Form.Item>
      <Form.Item className={styles.gridFull} label={text.email} name="email">
        <Input disabled={bootstrapLoading} />
      </Form.Item>
      <Form.Item className={`${styles.gridFull} ${styles.gridButton}`}>
        <Button block htmlType="submit" loading={bootstrapLoading} type="primary">
          {text.createAdministrator}
        </Button>
      </Form.Item>
    </Form>
  );
}

function AuthFrame() {
  return (
    <div aria-hidden="true" className={styles.frame}>
      <span className={`${styles.frameCorner} ${styles.frameCornerTopLeft}`} />
      <span className={`${styles.frameCorner} ${styles.frameCornerTopRight}`} />
      <span className={`${styles.frameCorner} ${styles.frameCornerBottomLeft}`} />
      <span className={`${styles.frameCorner} ${styles.frameCornerBottomRight}`} />
    </div>
  );
}
