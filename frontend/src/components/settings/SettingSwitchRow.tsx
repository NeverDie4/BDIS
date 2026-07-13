import { Form, Switch } from "antd";
import styles from "./settings.module.css";

export function SettingSwitchRow({
  name,
  label,
  description,
  disabled,
}: {
  name: string;
  label: string;
  description?: string;
  disabled?: boolean;
}) {
  return (
    <div className={styles.controlRow}>
      <div>
        <strong>{label}</strong>
        {description ? <span>{description}</span> : null}
      </div>
      <Form.Item name={name} valuePropName="checked" noStyle>
        <Switch disabled={disabled} />
      </Form.Item>
    </div>
  );
}
