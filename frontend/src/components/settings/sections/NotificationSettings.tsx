"use client";

import { App, Form, Input, Skeleton } from "antd";
import { useEffect } from "react";
import { useSettingNamespace } from "@/hooks/settings/useSettingNamespace";
import { getApiErrorMessage } from "@/lib/request";
import type { NotificationSettings as NotificationValues } from "@/types/settings";
import { SettingSwitchRow } from "../SettingSwitchRow";
import { SettingsFormActions } from "../SettingsFormActions";
import { SettingsSection } from "../SettingsSection";
import { useSettingsDirty } from "../SettingsDirtyContext";
import styles from "../settings.module.css";

export function NotificationSettings() {
  const { message } = App.useApp();
  const [form] = Form.useForm<NotificationValues>();
  const setting = useSettingNamespace<NotificationValues>("notification");
  const setDirty = useSettingsDirty("notification");
  useEffect(() => {
    if (setting.data) form.setFieldsValue(setting.data.values);
  }, [form, setting.data]);
  if (setting.isLoading) return <Skeleton active />;
  return (
    <SettingsSection title="通知偏好">
      <Form
        form={form}
        onValuesChange={() => setDirty(true)}
        onFinish={(values) =>
          setting.save.mutate(values, {
            onSuccess: () => {
              setDirty(false);
              message.success("通知偏好已保存");
            },
            onError: (error) => message.error(getApiErrorMessage(error)),
          })
        }
      >
        <SettingSwitchRow label="站内提醒" name="siteEnabled" />
        <SettingSwitchRow label="邮件提醒" name="emailEnabled" />
        <SettingSwitchRow label="任务提醒" name="taskReminderEnabled" />
        <SettingSwitchRow label="审核提醒" name="reviewReminderEnabled" />
        <SettingSwitchRow label="免打扰时段" name="quietHoursEnabled" />
        <div className={styles.formGrid}>
          <Form.Item label="开始时间" name="quietHoursStart">
            <Input placeholder="22:00" />
          </Form.Item>
          <Form.Item label="结束时间" name="quietHoursEnd">
            <Input placeholder="08:00" />
          </Form.Item>
        </div>
        <SettingsFormActions
          resetting={setting.reset.isPending}
          saving={setting.save.isPending}
          onReset={() =>
            setting.reset.mutate(undefined, {
              onSuccess: (data) => {
                form.setFieldsValue(data.values);
                setDirty(false);
              },
            })
          }
        />
      </Form>
    </SettingsSection>
  );
}
