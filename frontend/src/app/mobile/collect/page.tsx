"use client";

import { Button, Card, Form, Input, Select, Typography, Upload } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import styles from "./page.module.css";

export default function MobileCollectPage() {
  return (
    <main className={styles.mobilePage}>
      <section className={styles.mobileHero}>
        <p className={styles.eyebrow}>MOBILE COLLECTION</p>
        <Typography.Title level={1}>移动采集 H5</Typography.Title>
        <Typography.Paragraph>
          面向手机端现场采集，预留药材选择、定位、图片上传和生长指标录入结构。
        </Typography.Paragraph>
      </section>

      <Card className={styles.formCard} bordered={false}>
        <Form layout="vertical">
          <Form.Item label="药材名称">
            <Select
              placeholder="请选择药材"
              options={[
                { value: "coptis", label: "黄连" },
                { value: "codonopsis", label: "党参" },
              ]}
            />
          </Form.Item>
          <Form.Item label="采集地点">
            <Input placeholder="自动定位或手动填写地点" />
          </Form.Item>
          <Form.Item label="生长阶段">
            <Select
              placeholder="请选择生长阶段"
              options={[
                { value: "seedling", label: "幼苗期" },
                { value: "flowering", label: "花期" },
                { value: "harvest", label: "采收期" },
              ]}
            />
          </Form.Item>
          <Form.Item label="现场图片">
            <Upload>
              <Button icon={<UploadOutlined />}>上传图片</Button>
            </Upload>
          </Form.Item>
          <Form.Item label="采集备注">
            <Input.TextArea placeholder="记录天气、土壤、样本状态等现场情况" rows={4} />
          </Form.Item>
          <div className={styles.mobileActions}>
            <Button block size="large">
              暂存
            </Button>
            <Button block size="large" type="primary">
              提交
            </Button>
          </div>
        </Form>
      </Card>
    </main>
  );
}
