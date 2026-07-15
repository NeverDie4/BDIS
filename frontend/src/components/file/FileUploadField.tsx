"use client";

import { App, Button, Image, Upload } from "antd";
import type { UploadProps } from "antd";
import { ImageUp, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import {
  deleteOwnUnboundUpload,
  getFileRequestErrorMessage,
  uploadFile,
  type FileResource,
} from "@/lib/files";
import styles from "./FileUploadField.module.css";

interface FileUploadFieldProps {
  value?: string;
  onChange?: (value?: string) => void;
  accept?: string;
  maxSizeMB?: number;
  bizType?: string;
  bizId?: number;
  fileUsage?: string;
  accessLevel?: "private" | "public";
  buttonText?: string;
  cleanupUnboundOnUnmount?: boolean;
  onUploaded?: (file: FileResource) => void;
}

export function FileUploadField({
  value,
  onChange,
  accept = "image/*",
  maxSizeMB = 10,
  bizType,
  bizId,
  fileUsage,
  accessLevel,
  buttonText = "上传文件",
  cleanupUnboundOnUnmount = true,
  onUploaded,
}: FileUploadFieldProps) {
  const { message } = App.useApp();
  const [previewUrl, setPreviewUrl] = useState(value);
  const localPreviewUrlRef = useRef<string | undefined>(undefined);
  const pendingFileIdRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (!localPreviewUrlRef.current) {
      setPreviewUrl(value);
    }
  }, [value]);

  useEffect(
    () => () => {
      if (localPreviewUrlRef.current) {
        URL.revokeObjectURL(localPreviewUrlRef.current);
      }
      if (cleanupUnboundOnUnmount && pendingFileIdRef.current) {
        void deleteOwnUnboundUpload(pendingFileIdRef.current).catch(() => undefined);
      }
    },
    [],
  );

  function replaceLocalPreview(nextUrl?: string) {
    if (localPreviewUrlRef.current) {
      URL.revokeObjectURL(localPreviewUrlRef.current);
    }
    localPreviewUrlRef.current = nextUrl;
    setPreviewUrl(nextUrl ?? value);
  }

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
    const file = options.file as File;
    const nextPreviewUrl = accept === "image/*" ? URL.createObjectURL(file) : undefined;
    try {
      const uploaded = await uploadFile(file, { bizType, bizId, fileUsage, accessLevel });
      const previousPendingFileId = pendingFileIdRef.current;
      pendingFileIdRef.current = bizId ? undefined : uploaded.id;
      if (cleanupUnboundOnUnmount && previousPendingFileId && previousPendingFileId !== uploaded.id) {
        void deleteOwnUnboundUpload(previousPendingFileId).catch(() => undefined);
      }
      replaceLocalPreview(nextPreviewUrl);
      onChange?.(uploaded.fileUrl);
      onUploaded?.(uploaded);
      options.onSuccess?.(uploaded);
      message.success("文件上传成功");
    } catch (error) {
      if (nextPreviewUrl) {
        URL.revokeObjectURL(nextPreviewUrl);
      }
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
            onClick={() => {
              const pendingFileId = pendingFileIdRef.current;
              pendingFileIdRef.current = undefined;
              if (cleanupUnboundOnUnmount && pendingFileId) {
                void deleteOwnUnboundUpload(pendingFileId).catch(() => undefined);
              }
              replaceLocalPreview(undefined);
              setPreviewUrl(undefined);
              onChange?.(undefined);
            }}
          />
          {accept === "image/*" ? (
            <Image
              className={styles.image}
              src={previewUrl ?? value}
              alt="已上传图片"
              preview={false}
            />
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
