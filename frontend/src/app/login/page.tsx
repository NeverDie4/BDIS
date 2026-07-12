"use client";

import { apiPost, getApiErrorMessage } from "@/lib/request";
import { resolvePostLoginPath } from "@/config/routes";
import { useAuthStore } from "@/stores/auth-store";
import type { CurrentUser, LoginResult } from "@/types/api";
import { LockKeyhole, UserRound } from "lucide-react";
import { App, Button, Form, Input, Tabs, Typography } from "antd";
import { useRouter } from "next/navigation";

type LoginForm = {
  username: string;
  password: string;
};

type BootstrapForm = LoginForm & {
  bootstrapToken: string;
  realName: string;
  phoneNumber?: string;
  email?: string;
};

export default function LoginPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const setAuth = useAuthStore((state) => state.setAuth);

  async function login(values: LoginForm) {
    try {
      const result = await apiPost<LoginResult>("/auth/sessions", values);
      setAuth(result.accessToken, result.user);
      message.success("登录成功");
      if (result.mustChangePassword || result.user.mustChangePassword) {
        router.replace("/settings?tab=security");
        return;
      }
      const returnUrl = resolvePostLoginPath(
        result.user,
        typeof window === "undefined"
          ? undefined
          : new URLSearchParams(window.location.search).get("returnUrl"),
        result.preferredLandingPath,
      );
      router.replace(returnUrl);
    } catch (error) {
      message.error(getApiErrorMessage(error, "登录失败"));
    }
  }

  async function bootstrap(values: BootstrapForm) {
    try {
      await apiPost<CurrentUser>("/auth/bootstrap-admin", values);
      message.success("管理员已初始化，请使用该账号登录");
    } catch (error) {
      message.error(getApiErrorMessage(error, "管理员初始化失败"));
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="login-title">
          <Typography.Text className="brand-eyebrow">BDIS</Typography.Text>
          <Typography.Title level={2}>生物医药数字信息系统</Typography.Title>
        </div>
        <Tabs
          items={[
            {
              key: "login",
              label: "登录",
              children: (
                <Form<LoginForm> layout="vertical" onFinish={login}>
                  <Form.Item
                    name="username"
                    label="账号"
                    rules={[{ required: true, message: "请输入账号" }]}
                  >
                    <Input prefix={<UserRound size={16} />} autoComplete="username" />
                  </Form.Item>
                  <Form.Item
                    name="password"
                    label="密码"
                    rules={[{ required: true, message: "请输入密码" }]}
                  >
                    <Input.Password
                      prefix={<LockKeyhole size={16} />}
                      autoComplete="current-password"
                    />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block>
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: "bootstrap",
              label: "初始化管理员",
              children: (
                <Form<BootstrapForm> layout="vertical" onFinish={bootstrap}>
                  <Form.Item
                    name="bootstrapToken"
                    label="初始化令牌"
                    rules={[{ required: true, message: "请输入初始化令牌" }]}
                  >
                    <Input.Password />
                  </Form.Item>
                  <Form.Item
                    name="username"
                    label="账号"
                    rules={[{ required: true, message: "请输入账号" }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item
                    name="password"
                    label="密码"
                    rules={[
                      { required: true, message: "请输入密码" },
                      { min: 8, message: "密码至少 8 位" },
                    ]}
                  >
                    <Input.Password />
                  </Form.Item>
                  <Form.Item
                    name="realName"
                    label="姓名"
                    rules={[{ required: true, message: "请输入姓名" }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item name="phoneNumber" label="手机号">
                    <Input />
                  </Form.Item>
                  <Form.Item name="email" label="邮箱">
                    <Input />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block>
                    创建管理员
                  </Button>
                </Form>
              ),
            },
          ]}
        />
      </section>
    </main>
  );
}
