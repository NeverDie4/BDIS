import { Form, Select } from "antd";
import type { SelectProps } from "antd";
import styles from "./settings.module.css";

export function SettingSelectRow({
  name,
  label,
  description,
  options,
}: {
  name: string;
  label: string;
  description?: string;
  options: SelectProps["options"];
}) {
  return (
    <div className={styles.controlRow}>
      <div>
        <strong>{label}</strong>
        {description ? <span>{description}</span> : null}
      </div>
      <Form.Item name={name} noStyle>
        <Select className={styles.selectControl} options={options} />
      </Form.Item>
    </div>
  );
}
