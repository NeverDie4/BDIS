"use client";

import { App, Button, Image, Upload } from "antd";
import type { UploadProps } from "antd";
import { ImageUp, X } from "lucide-react";
import { getFileRequestErrorMessage, uploadFile, type FileResource } from "@/lib/files";
import styles from "./FileUploadField.module.css";

interface FileUploadFieldProps {
  value?: string;
  onChange?: (value?: string) => void;
  accept?: string;
  maxSizeMB?: number;
  bizType?: string;
  fileUsage?: string;
  buttonText?: string;
  onUploaded?: (file: FileResource) => void;
}

export function FileUploadField({
  value,
  onChange,
  accept = "image/*",
  maxSizeMB = 10,
  bizType,
  fileUsage,
  buttonText = "上传文件",
  onUploaded,
}: FileUploadFieldProps) {
  const { message } = App.useApp();

  function beforeUpload(file: File) {
    if (accept === "image/*" && !file.type.startsWith("image/")) {
      message.error("请选择图片文件");
      return Upload.LIST_IGNORE;
    }
    if (file.size > maxSizeMB * 1024 * 1024) {
      message.error(`文件大小不能超过 ${maxSizeMB}MB`);
      return Upload.LIST_IGNORE;
    }
    return true;
  }

  const customRequest: UploadProps["customRequest"] = async (options) => {
    try {
      const file = options.file as File;
      const uploaded = await uploadFile(file, { bizType, fileUsage });
      onChange?.(uploaded.fileUrl);
      onUploaded?.(uploaded);
      options.onSuccess?.(uploaded);
      message.success("文件上传成功");
    } catch (error) {
      options.onError?.(error as Error);
      message.error(getFileRequestErrorMessage(error));
    }
  };

  return (
    <div className={styles.shell}>
      {value && (
        <div className={styles.preview}>
          <Button
            type="text"
            size="small"
            className={styles.remove}
            aria-label="移除已上传文件"
            icon={<X size={13} />}
            onClick={() => onChange?.(undefined)}
          />
          {accept === "image/*" ? (
            <Image className={styles.image} src={value} alt="已上传图片" preview={false} />
          ) : (
            <div className={styles.fileName}>{value}</div>
          )}
        </div>
      )}
      <Upload
        accept={accept}
        maxCount={1}
        showUploadList={false}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
      >
        <Button icon={<ImageUp size={16} />}>{value ? "重新上传" : buttonText}</Button>
      </Upload>
    </div>
  );
}
