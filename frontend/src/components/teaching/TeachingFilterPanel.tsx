import { PlusOutlined } from "@ant-design/icons";
import { Button, DatePicker, Form, Input, Select, Tooltip } from "antd";
import styles from "./teaching.module.css";

const statusOptions = [
  { label: "已发布", value: "published" },
  { label: "草稿", value: "draft" },
  { label: "已下架", value: "offline" },
];

const ownerOptions = [
  { label: "周老师", value: "zhou" },
  { label: "李老师", value: "li" },
  { label: "陈老师", value: "chen" },
];

const subjectOptions = [
  { label: "中药鉴定", value: "identification" },
  { label: "资源调查", value: "resource" },
  { label: "质量评价", value: "quality" },
];

const herbOptions = [
  { label: "黄连", value: "coptis" },
  { label: "金银花", value: "honeysuckle" },
  { label: "杜仲", value: "eucommia" },
];

export function TeachingFilterPanel() {
  const [form] = Form.useForm();

  return (
    <Form className={styles.filterPanel} form={form} layout="inline" onFinish={() => undefined}>
      <div className={styles.mainFilters}>
        <Form.Item className={styles.keywordFilter} name="keyword">
          <Input allowClear placeholder="请输入课程名称关键词" />
        </Form.Item>
        <Form.Item className={styles.statusFilter} name="status">
          <Select allowClear options={statusOptions} placeholder="课程状态" />
        </Form.Item>
        <Form.Item className={styles.ownerFilter} name="owner">
          <Select allowClear options={ownerOptions} placeholder="负责人" />
        </Form.Item>
        <Form.Item className={styles.subjectFilter} name="subject">
          <Select allowClear options={subjectOptions} placeholder="学科方向" />
        </Form.Item>
        <Form.Item className={styles.herbFilter} name="herb">
          <Select allowClear options={herbOptions} placeholder="关联药材" />
        </Form.Item>
        <Tooltip title="展开更多筛选条件">
          <Button aria-label="展开更多筛选条件" icon={<PlusOutlined />} onClick={() => undefined} />
        </Tooltip>
      </div>

      <div className={styles.filterFooter}>
        <Form.Item className={styles.dateFilter} name="updatedAt">
          <DatePicker.RangePicker placeholder={["更新时间起", "更新时间止"]} />
        </Form.Item>
        <div className={styles.filterActions}>
          <Button onClick={() => form.resetFields()}>重置</Button>
          <Button htmlType="submit" type="primary">
            查询
          </Button>
        </div>
      </div>
    </Form>
  );
}
