import { DeleteOutlined, InboxOutlined } from "@ant-design/icons";
import { App, Button, Form, Input, Modal, Upload } from "antd";
import type { UploadProps } from "antd";
import { useEffect, useState } from "react";
import {
  createExperimentRecord,
  listExperimentAttachments,
  unbindExperimentAttachment,
  updateExperimentRecord,
  uploadExperimentAttachment,
  type ExperimentAttachmentApi,
  type ExperimentRecordDetailApi,
} from "@/lib/experiment-records";
import { getApiErrorMessage } from "@/lib/request";
import styles from "./teaching.module.css";

type ExperimentRecordEditorModalProps = {
  open: boolean;
  courseId: number;
  record?: ExperimentRecordDetailApi | null;
  onCancel: () => void;
  onSaved: (record: ExperimentRecordDetailApi) => void;
};

type RecordFormValues = {
  recordNo: string;
  experimentTitle: string;
  experimentProcess?: string;
  experimentResult?: string;
  recordedAt?: string;
  remark?: string;
};

export function ExperimentRecordEditorModal({
  open,
  courseId,
  record,
  onCancel,
  onSaved,
}: ExperimentRecordEditorModalProps) {
  const { message } = App.useApp();
  const [form] = Form.useForm<RecordFormValues>();
  const [saving, setSaving] = useState(false);
  const [attachments, setAttachments] = useState<ExperimentAttachmentApi[]>([]);
  const [loadingAttachments, setLoadingAttachments] = useState(false);

  useEffect(() => {
    if (!open) return;
    form.setFieldsValue({
      recordNo: record?.recordNo ?? `ER-${Date.now()}`,
      experimentTitle: record?.experimentTitle ?? "",
      experimentProcess: record?.experimentProcess ?? "",
      experimentResult: record?.experimentResult ?? "",
      recordedAt: record?.recordedAt?.slice(0, 16) ?? new Date().toISOString().slice(0, 16),
      remark: record?.remark ?? "",
    });
    if (!record) {
      setAttachments([]);
      return;
    }
    setLoadingAttachments(true);
    void listExperimentAttachments(record.id)
      .then(setAttachments)
      .catch((error) => message.error(getApiErrorMessage(error, "附件加载失败")))
      .finally(() => setLoadingAttachments(false));
  }, [form, message, open, record]);

  async function handleSubmit(values: RecordFormValues) {
    setSaving(true);
    try {
      const payload = {
        ...values,
        recordedAt: values.recordedAt ? new Date(values.recordedAt).toISOString() : undefined,
      };
      const saved = record
        ? await updateExperimentRecord(record.id, { ...payload, version: record.version })
        : await createExperimentRecord({ ...payload, courseId });
      message.success(record ? "实验记录已保存" : "实验记录已创建");
      onSaved(saved);
    } catch (error) {
      message.error(getApiErrorMessage(error, "实验记录保存失败"));
    } finally {
      setSaving(false);
    }
  }

  const uploadProps: UploadProps = {
    multiple: true,
    showUploadList: false,
    beforeUpload: async (file) => {
      if (!record) {
        message.info("请先保存实验记录，再上传附件");
        return Upload.LIST_IGNORE;
      }
      try {
        const uploaded = await uploadExperimentAttachment(file, record.id);
        setAttachments((current) => [...current, uploaded]);
        message.success("附件上传成功");
      } catch (error) {
        message.error(getApiErrorMessage(error, "附件上传失败"));
      }
      return Upload.LIST_IGNORE;
    },
  };

  async function removeAttachment(file: ExperimentAttachmentApi) {
    if (!record) return;
    try {
      await unbindExperimentAttachment(record.id, file.id);
      setAttachments((current) => current.filter((item) => item.id !== file.id));
      message.success("附件已移除");
    } catch (error) {
      message.error(getApiErrorMessage(error, "附件移除失败"));
    }
  }

  return (
    <Modal
      centered
      destroyOnClose
      open={open}
      title={record ? "编辑实验记录" : "新增实验记录"}
      width={720}
      okText="保存记录"
      cancelText="取消"
      confirmLoading={saving}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <div className={styles.recordFormGrid}>
          <Form.Item label="记录编号" name="recordNo" rules={[{ required: true, message: "请输入记录编号" }]}>
            <Input disabled={Boolean(record)} placeholder="如 ER-2026-001" />
          </Form.Item>
          <Form.Item label="记录时间" name="recordedAt" rules={[{ required: true, message: "请选择记录时间" }]}>
            <Input type="datetime-local" />
          </Form.Item>
        </div>
        <Form.Item label="实验标题" name="experimentTitle" rules={[{ required: true, message: "请输入实验标题" }]}>
          <Input placeholder="请输入本次实验标题" />
        </Form.Item>
        <Form.Item label="实验过程" name="experimentProcess">
          <Input.TextArea rows={4} showCount maxLength={5000} placeholder="记录实验步骤、现象和操作过程" />
        </Form.Item>
        <Form.Item label="实验结果" name="experimentResult">
          <Input.TextArea rows={4} showCount maxLength={5000} placeholder="记录实验结果、结论和分析" />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={2} maxLength={500} placeholder="可选" />
        </Form.Item>
        <section className={styles.recordAttachmentSection}>
          <div className={styles.recordSectionHeading}>
            <strong>附件</strong>
            <Upload {...uploadProps}>
              <Button icon={<InboxOutlined />} disabled={!record}>上传附件</Button>
            </Upload>
          </div>
          {loadingAttachments ? <span className={styles.mutedText}>正在加载附件…</span> : null}
          {attachments.map((file) => (
            <div className={styles.recordAttachmentRow} key={file.id}>
              <span title={file.originalFilename ?? file.fileName}>{file.originalFilename ?? file.fileName}</span>
              <Button type="link" href={file.fileUrl} target="_blank">查看</Button>
              <Button type="text" danger icon={<DeleteOutlined />} onClick={() => void removeAttachment(file)} aria-label={`移除${file.originalFilename ?? file.fileName}`} />
            </div>
          ))}
          {!loadingAttachments && attachments.length === 0 ? <span className={styles.mutedText}>暂无附件</span> : null}
        </section>
      </Form>
    </Modal>
  );
}
