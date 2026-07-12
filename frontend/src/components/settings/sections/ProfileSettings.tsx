"use client";

import { App, Avatar, Button, Descriptions, Form, Input, Skeleton, Space, Upload } from "antd";
import type { UploadProps } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ImageUp, Trash2, UserRound } from "lucide-react";
import { useEffect } from "react";
import { uploadFile } from "@/lib/files";
import { apiDelete, apiGet, apiPatch, apiPut, getApiErrorMessage } from "@/lib/request";
import type { ProfileSettings as ProfileSettingsData } from "@/types/settings";
import { SettingsFormActions } from "../SettingsFormActions";
import { SettingsSection } from "../SettingsSection";
import styles from "../settings.module.css";

export function ProfileSettings() {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const queryClient = useQueryClient();
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
      message.success("个人资料已保存");
    },
    onError: (error) => message.error(getApiErrorMessage(error, "保存失败")),
  });
  const clearAvatar = useMutation({
    mutationFn: () => apiDelete<ProfileSettingsData>("/me/profile/avatar"),
    onSuccess: (data) => queryClient.setQueryData(["settings", "profile"], data),
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
      return true;
    },
    customRequest: async ({ file, onSuccess, onError }) => {
      try {
        const uploaded = await uploadFile(file as File);
        const data = await apiPut<ProfileSettingsData>("/me/profile/avatar", {
          fileId: uploaded.id,
        });
        queryClient.setQueryData(["settings", "profile"], data);
        onSuccess?.(data);
        message.success("头像已更新");
      } catch (error) {
        onError?.(error as Error);
        message.error(getApiErrorMessage(error, "头像更新失败"));
      }
    },
  };

  if (profile.isLoading) return <Skeleton active />;
  return (
    <div className={styles.sectionStack}>
      <SettingsSection title="个人资料">
        <div className={styles.profileHeader}>
          <Avatar icon={<UserRound size={28} />} size={68} />
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
        <Form form={form} layout="vertical" onFinish={(values) => save.mutate(values)}>
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
          <Descriptions.Item label="部门">
            {profile.data?.departmentName || "-"}
          </Descriptions.Item>
        </Descriptions>
      </SettingsSection>
    </div>
  );
}
