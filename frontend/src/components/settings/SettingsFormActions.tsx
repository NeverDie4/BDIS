import { Button, Space } from "antd";
import { RotateCcw, Save } from "lucide-react";

export function SettingsFormActions({
  saving,
  resetting,
  onReset,
}: {
  saving?: boolean;
  resetting?: boolean;
  onReset?: () => void;
}) {
  return (
    <Space>
      {onReset ? (
        <Button icon={<RotateCcw size={16} />} loading={resetting} onClick={onReset}>
          恢复默认
        </Button>
      ) : null}
      <Button htmlType="submit" icon={<Save size={16} />} loading={saving} type="primary">
        保存
      </Button>
    </Space>
  );
}
