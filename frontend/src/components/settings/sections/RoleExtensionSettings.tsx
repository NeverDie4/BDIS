import { Empty } from "antd";
import { SettingsSection } from "../SettingsSection";

export function TeachingSettings() {
  return <RoleExtension title="教学设置" />;
}

export function LearningSettings() {
  return <RoleExtension title="学习设置" />;
}

export function CollectionSettings() {
  return <RoleExtension title="采集设置" />;
}

export function ReviewSettings() {
  return <RoleExtension title="审核设置" />;
}

function RoleExtension({ title }: { title: string }) {
  return (
    <SettingsSection title={title}>
      <Empty description="暂无可配置项" image={Empty.PRESENTED_IMAGE_SIMPLE} />
    </SettingsSection>
  );
}
