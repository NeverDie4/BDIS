"use client";

import { Alert, App, Button, Form, Skeleton } from "antd";
import { useEffect } from "react";
import { getApiErrorMessage } from "@/lib/request";
import { useSettingNamespace } from "@/hooks/settings/useSettingNamespace";
import type {
  AppearanceSettings as AppearanceValues,
  CommonSettings as CommonValues,
} from "@/types/settings";
import { SettingSelectRow } from "../SettingSelectRow";
import { SettingSwitchRow } from "../SettingSwitchRow";
import { SettingsFormActions } from "../SettingsFormActions";
import { SettingsSection } from "../SettingsSection";
import { useSettingsDirty } from "../SettingsDirtyContext";

export function AppearanceSettings() {
  const { message } = App.useApp();
  const [form] = Form.useForm<AppearanceValues>();
  const [commonForm] = Form.useForm<CommonValues>();
  const setting = useSettingNamespace<AppearanceValues>("appearance");
  const common = useSettingNamespace<CommonValues>("common");
  const setCommonDirty = useSettingsDirty("appearance-common");
  const setAppearanceDirty = useSettingsDirty("appearance-ui");
  useEffect(() => {
    if (!setting.isLoading && !common.isLoading && setting.data) {
      form.setFieldsValue(setting.data.values);
    }
  }, [common.isLoading, form, setting.data, setting.isLoading]);
  useEffect(() => {
    if (!setting.isLoading && !common.isLoading && common.data) {
      commonForm.setFieldsValue(common.data.values);
    }
  }, [common.data, common.isLoading, commonForm, setting.isLoading]);
  if (setting.isLoading || common.isLoading) return <Skeleton active />;
  if (setting.isError || common.isError) {
    const error = setting.error || common.error;
    return (
      <Alert
        showIcon
        type="error"
        message="界面偏好加载失败"
        description={getApiErrorMessage(error)}
        action={
          <Button
            onClick={() => {
              void setting.refetch();
              void common.refetch();
            }}
          >
            重新加载
          </Button>
        }
      />
    );
  }
  return (
    <div style={{ display: "grid", gap: 18 }}>
      <SettingsSection title="常规偏好">
        <Form
          form={commonForm}
          onValuesChange={() => setCommonDirty(true)}
          onFinish={(values) =>
            common.save.mutate(values, {
              onSuccess: () => {
                setCommonDirty(false);
                message.success("常规偏好已保存");
              },
              onError: (error) => message.error(getApiErrorMessage(error)),
            })
          }
        >
          <SettingSelectRow
            label="登录后进入"
            name="defaultLandingPath"
            options={[
              { label: "门户首页", value: "/" },
              { label: "个人主页", value: "/profile" },
              { label: "个人设置", value: "/settings" },
              { label: "权限后台", value: "/dashboard" },
            ]}
          />
          <SettingSelectRow
            label="显示时区"
            name="timezone"
            options={[{ label: "Asia/Shanghai", value: "Asia/Shanghai" }]}
          />
          <SettingSelectRow
            label="界面语言"
            name="locale"
            options={[{ label: "简体中文", value: "zh-CN" }]}
          />
          <SettingsFormActions
            resetting={common.reset.isPending}
            saving={common.save.isPending}
            onReset={() =>
              common.reset.mutate(undefined, {
                onSuccess: (data) => {
                  commonForm.setFieldsValue(data.values);
                  setCommonDirty(false);
                },
                onError: (error) => message.error(getApiErrorMessage(error, "恢复默认值失败")),
              })
            }
          />
        </Form>
      </SettingsSection>
      <SettingsSection title="界面偏好">
        <Form
          form={form}
          onValuesChange={() => setAppearanceDirty(true)}
          onFinish={(values) =>
            setting.save.mutate(values, {
              onSuccess: () => {
                setAppearanceDirty(false);
                message.success("界面偏好已保存");
              },
              onError: (error) => message.error(getApiErrorMessage(error)),
            })
          }
        >
          <SettingSelectRow
            label="内容密度"
            name="contentDensity"
            options={[
              { label: "舒适", value: "comfortable" },
              { label: "紧凑", value: "compact" },
            ]}
          />
          <SettingSelectRow
            label="后台侧栏"
            name="sidebarMode"
            options={[
              { label: "自动", value: "auto" },
              { label: "展开", value: "expanded" },
              { label: "收起", value: "collapsed" },
            ]}
          />
          <SettingSwitchRow label="减少动态效果" name="reduceMotion" />
          <SettingsFormActions
            resetting={setting.reset.isPending}
            saving={setting.save.isPending}
            onReset={() =>
              setting.reset.mutate(undefined, {
                onSuccess: (data) => {
                  form.setFieldsValue(data.values);
                  setAppearanceDirty(false);
                },
                onError: (error) => message.error(getApiErrorMessage(error, "恢复默认值失败")),
              })
            }
          />
        </Form>
      </SettingsSection>
    </div>
  );
}
