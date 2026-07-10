import { Button, Form, Input, Select } from "antd";
import styles from "./herbs.module.css";

const categoryOptions = [
  { label: "根及根茎类", value: "root" },
  { label: "花叶类", value: "flower-leaf" },
];

const statusOptions = [
  { label: "启用", value: "enabled" },
  { label: "停用", value: "disabled" },
];

const partOptions = [
  { label: "根", value: "root" },
  { label: "花", value: "flower" },
  { label: "全草", value: "whole" },
];

export function HerbFilterBar() {
  const [form] = Form.useForm();

  return (
    <Form className={styles.filterForm} form={form} layout="inline" onFinish={() => undefined}>
      <Form.Item className={styles.keywordFilter} name="keyword">
        <Input allowClear placeholder="请输入药材名称、别名或拉丁名" />
      </Form.Item>
      <Form.Item className={styles.selectFilter} name="category">
        <Select allowClear options={categoryOptions} placeholder="所属分类" />
      </Form.Item>
      <Form.Item className={styles.statusFilter} name="status">
        <Select allowClear options={statusOptions} placeholder="状态" />
      </Form.Item>
      <Form.Item className={styles.selectFilter} name="medicinalPart">
        <Select allowClear options={partOptions} placeholder="药用部位" />
      </Form.Item>
      <div className={styles.filterActions}>
        <Button htmlType="submit" type="primary">
          查询
        </Button>
        <Button onClick={() => form.resetFields()}>重置</Button>
      </div>
    </Form>
  );
}
