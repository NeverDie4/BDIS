"use client";

import { Alert, App, Button, Descriptions, Form, Input, Skeleton, Space, Upload } from "antd";
import type { UploadProps } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ImageUp, Trash2 } from "lucide-react";
import { useEffect } from "react";
import { UserAvatar } from "@/components/common/UserAvatar";
import { apiDelete, apiGet, apiPatch, getApiErrorMessage, request } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import type { ProfileSettings as ProfileSettingsData } from "@/types/settings";
import { SettingsFormActions } from "../SettingsFormActions";
import { SettingsSection } from "../SettingsSection";
import { useSettingsDirty } from "../SettingsDirtyContext";
import styles from "../settings.module.css";

export function ProfileSettings() {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const queryClient = useQueryClient();
  const setDirty = useSettingsDirty("profile");
  const authUser = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const profile = useQuery({
    queryKey: ["settings", "profile"],
    queryFn: () => apiGet<ProfileSettingsData>("/me/profile"),
  });
  useEffect(() => {
    if (profile.data) form.setFieldsValue(profile.data);
  }, [form, profile.data]);
  const save = useMutation({
    mutationFn: (values: { realName?: string; phoneNumber?: string; email?: string }) =>
      apiPatch<ProfileSettingsData>("/me/profile", values),
    onSuccess: (data) => {
      queryClient.setQueryData(["settings", "profile"], data);
      if (authUser) setUser({ ...authUser, realName: data.realName });
      setDirty(false);
      message.success("个人资料已保存");
    },
    onError: (error) => message.error(getApiErrorMessage(error, "保存失败")),
  });
  const clearAvatar = useMutation({
    mutationFn: () => apiDelete<ProfileSettingsData>("/me/profile/avatar"),
    onSuccess: (data) => {
      queryClient.setQueryData(["settings", "profile"], data);
      queryClient.removeQueries({ queryKey: ["user-avatar"] });
      if (authUser) setUser({ ...authUser, avatarUrl: undefined });
      message.success("头像已清除");
    },
    onError: (error) => message.error(getApiErrorMessage(error, "头像清除失败")),
  });
  const uploadProps: UploadProps = {
    accept: "image/*",
    maxCount: 1,
    showUploadList: false,
    beforeUpload: (file) => {
      if (!file.type.startsWith("image/")) {
        message.error("请选择图片文件");
        return Upload.LIST_IGNORE;
      }
      if (file.size > 5 * 1024 * 1024) {
        message.error("头像文件不能超过5MB");
        return Upload.LIST_IGNORE;
      }
      return true;
    },
    customRequest: async ({ file, onSuccess, onError }) => {
      try {
        const formData = new FormData();
        formData.append("file", file as File);
        const response = await request.put<{ data: ProfileSettingsData }>(
          "/me/profile/avatar",
          formData,
        );
        const data = response.data.data;
        queryClient.setQueryData(["settings", "profile"], data);
        queryClient.removeQueries({ queryKey: ["user-avatar"] });
        if (authUser) setUser({ ...authUser, avatarUrl: data.avatarUrl });
        onSuccess?.(data);
        message.success("头像已更新");
      } catch (error) {
        onError?.(error as Error);
        message.error(getApiErrorMessage(error, "头像更新失败"));
      }
    },
  };

  if (profile.isLoading) return <Skeleton active />;
  if (profile.isError) {
    return (
      <Alert
        showIcon
        type="error"
        message="个人资料加载失败"
        description={getApiErrorMessage(profile.error)}
        action={<Button onClick={() => void profile.refetch()}>重新加载</Button>}
      />
    );
  }
  return (
    <div className={styles.sectionStack}>
      <SettingsSection title="个人资料">
        <div className={styles.profileHeader}>
          <UserAvatar avatarUrl={profile.data?.avatarUrl} iconSize={28} size={68} />
          <Space wrap>
            <Upload {...uploadProps}>
              <Button icon={<ImageUp size={16} />}>更换头像</Button>
            </Upload>
            {profile.data?.avatarUrl ? (
              <Button
                danger
                icon={<Trash2 size={16} />}
                loading={clearAvatar.isPending}
                onClick={() => clearAvatar.mutate()}
              >
                清除头像
              </Button>
            ) : null}
          </Space>
        </div>
        <Form
          form={form}
          layout="vertical"
          onValuesChange={() => setDirty(true)}
          onFinish={(values) => save.mutate(values)}
        >
          <div className={styles.formGrid}>
            <Form.Item label="姓名" name="realName" rules={[{ max: 50 }]}>
              <Input />
            </Form.Item>
            <Form.Item label="手机号" name="phoneNumber">
              <Input />
            </Form.Item>
            <Form.Item label="邮箱" name="email" rules={[{ type: "email" }]}>
              <Input />
            </Form.Item>
          </div>
          <SettingsFormActions saving={save.isPending} />
        </Form>
      </SettingsSection>
      <SettingsSection title="账号归属">
        <Descriptions column={{ xs: 1, md: 2 }} size="small">
          <Descriptions.Item label="账号">{profile.data?.username || "-"}</Descriptions.Item>
          <Descriptions.Item label="角色">
            {profile.data?.roleCodes.join("、") || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="机构">
            {profile.data?.organizationName || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="部门">{profile.data?.departmentName || "-"}</Descriptions.Item>
        </Descriptions>
      </SettingsSection>
    </div>
  );
}
