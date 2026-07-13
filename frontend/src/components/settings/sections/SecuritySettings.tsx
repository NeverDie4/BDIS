"use client";

import { Alert, App, Button, Collapse, Form, Input, List, Popconfirm, Skeleton, Tag } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { LogOut, ShieldCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { apiDelete, apiGet, apiPut, getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { UserSession } from "@/types/settings";
import { SettingsFormActions } from "../SettingsFormActions";
import { SettingsSection } from "../SettingsSection";
import { useSettingsDirty } from "../SettingsDirtyContext";
import styles from "../settings.module.css";

export function SecuritySettings() {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const router = useRouter();
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const queryClient = useQueryClient();
  const setDirty = useSettingsDirty("security-password");
  const sessions = useQuery({
    queryKey: ["settings", "sessions"],
    queryFn: () => apiGet<UserSession[]>("/me/sessions"),
  });
  const changePassword = useMutation({
    mutationFn: (values: { currentPassword: string; newPassword: string }) =>
      apiPut<void>("/me/password", { ...values, revokeOtherSessions: true }),
    onSuccess: () => {
      setDirty(false);
      message.success("密码已修改，请重新登录");
      clearAuth();
      router.push("/login");
    },
    onError: (error) => message.error(getApiErrorMessage(error, "密码修改失败")),
  });
  const revoke = useMutation({
    mutationFn: (sessionId: string) => apiDelete<void>(`/me/sessions/${sessionId}`),
    onSuccess: (_, sessionId) => {
      const current = sessions.data?.find((item) => item.sessionId === sessionId)?.current;
      if (current) {
        clearAuth();
        router.push("/login");
      } else {
        void queryClient.invalidateQueries({ queryKey: ["settings", "sessions"] });
      }
    },
    onError: (error) => message.error(getApiErrorMessage(error, "设备退出失败")),
  });
  const revokeOthers = useMutation({
    mutationFn: () => apiDelete<void>("/me/sessions/others"),
    onSuccess: () => {
      message.success("其他设备已退出");
      void queryClient.invalidateQueries({ queryKey: ["settings", "sessions"] });
    },
    onError: (error) => message.error(getApiErrorMessage(error, "其他设备退出失败")),
  });
  const now = Date.now();
  const activeSessions =
    sessions.data?.filter(
      (session) => session.status === "active" && new Date(session.expiresAt).getTime() > now,
    ) ?? [];
  const historySessions =
    sessions.data?.filter(
      (session) => session.status !== "active" || new Date(session.expiresAt).getTime() <= now,
    ) ?? [];

  function sessionList(data: UserSession[], allowRevoke: boolean) {
    return (
      <List
        dataSource={data}
        locale={{ emptyText: "暂无会话" }}
        renderItem={(session) => (
          <List.Item
            actions={
              allowRevoke
                ? [
                    <Popconfirm
                      key="revoke"
                      title="确认退出该设备？"
                      onConfirm={() => revoke.mutate(session.sessionId)}
                    >
                      <Button
                        danger
                        icon={<LogOut size={15} />}
                        loading={revoke.isPending}
                        size="small"
                        type="text"
                      >
                        退出
                      </Button>
                    </Popconfirm>,
                  ]
                : undefined
            }
          >
            <List.Item.Meta
              avatar={<ShieldCheck size={20} />}
              title={
                <span>
                  {session.deviceName || "未知设备"}{" "}
                  {session.current ? <Tag color="green">当前设备</Tag> : null}
                  {!allowRevoke ? <SessionStatusTag status={session.status} /> : null}
                </span>
              }
              description={`${session.ipAddress || "未知 IP"} · 最近活动 ${new Date(
                session.lastActiveAt,
              ).toLocaleString()}`}
            />
          </List.Item>
        )}
      />
    );
  }

  return (
    <div className={styles.sectionStack}>
      <SettingsSection title="修改密码">
        <Form
          form={form}
          className={styles.passwordForm}
          layout="vertical"
          onValuesChange={() => setDirty(true)}
          onFinish={(values) => changePassword.mutate(values)}
        >
          <Form.Item label="当前密码" name="currentPassword" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            label="新密码"
            name="newPassword"
            rules={[{ required: true }, { min: 8, max: 72 }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            dependencies={["newPassword"]}
            label="确认新密码"
            name="confirmPassword"
            rules={[
              { required: true },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  return !value || getFieldValue("newPassword") === value
                    ? Promise.resolve()
                    : Promise.reject(new Error("两次输入的密码不一致"));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <SettingsFormActions saving={changePassword.isPending} />
        </Form>
      </SettingsSection>
      <SettingsSection title="登录设备">
        {sessions.isLoading ? (
          <Skeleton active />
        ) : sessions.isError ? (
          <Alert
            showIcon
            type="error"
            message="登录设备加载失败"
            description={getApiErrorMessage(sessions.error)}
            action={<Button onClick={() => void sessions.refetch()}>重新加载</Button>}
          />
        ) : (
          <>
            {sessionList(activeSessions, true)}
            <Button
              danger
              disabled={!activeSessions.some((session) => !session.current)}
              loading={revokeOthers.isPending}
              onClick={() => revokeOthers.mutate()}
            >
              退出其他设备
            </Button>
            {historySessions.length ? (
              <Collapse
                ghost
                items={[
                  {
                    key: "history",
                    label: `最近登录历史（${historySessions.length}）`,
                    children: sessionList(historySessions, false),
                  },
                ]}
              />
            ) : null}
          </>
        )}
      </SettingsSection>
    </div>
  );
}

function SessionStatusTag({ status }: { status: UserSession["status"] }) {
  if (status === "revoked") return <Tag>已退出</Tag>;
  if (status === "expired") return <Tag>已过期</Tag>;
  return <Tag>已失效</Tag>;
}
