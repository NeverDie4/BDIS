import { Button, Form, Input, Select } from "antd";
import { useEffect } from "react";
import styles from "./herbs.module.css";

export type HerbFilterValues = {
  keyword?: string;
  category?: string;
  medicinalPart?: string;
};

type HerbFilterBarProps = {
  categoryOptions?: Array<{ label: string; value: string }>;
  initialKeyword?: string;
  onSearch?: (values: HerbFilterValues) => void;
};

const fallbackCategoryOptions = [
  { label: "根及根茎类", value: "root" },
  { label: "花叶类", value: "flower-leaf" },
  { label: "全草类", value: "whole" },
  { label: "皮类", value: "bark" },
];

const partOptions = [
  { label: "根", value: "根" },
  { label: "根茎", value: "根茎" },
  { label: "花", value: "花" },
  { label: "花蕾", value: "花蕾" },
  { label: "全草", value: "全草" },
  { label: "树皮", value: "树皮" },
];

export function HerbFilterBar({ categoryOptions = fallbackCategoryOptions, initialKeyword, onSearch = () => undefined }: HerbFilterBarProps) {
  const [form] = Form.useForm<HerbFilterValues>();

  useEffect(() => {
    form.setFieldValue("keyword", initialKeyword);
  }, [form, initialKeyword]);

  function handleReset() {
    form.resetFields();
    onSearch({});
  }

  return (
    <Form
      className={styles.filterForm}
      form={form}
      layout="inline"
      onFinish={(values) => onSearch(values)}
    >
      <Form.Item className={styles.keywordFilter} name="keyword">
        <Input allowClear placeholder="请输入药材名称、别名或编号" />
      </Form.Item>
      <Form.Item className={styles.selectFilter} name="category">
        <Select allowClear options={categoryOptions} placeholder="所属分类" />
      </Form.Item>
      <Form.Item className={styles.selectFilter} name="medicinalPart">
        <Select allowClear options={partOptions} placeholder="药用部位" />
      </Form.Item>
      <div className={styles.filterActions}>
        <Button htmlType="submit" type="primary">
          查询
        </Button>
        <Button onClick={handleReset}>重置</Button>
      </div>
    </Form>
  );
}
