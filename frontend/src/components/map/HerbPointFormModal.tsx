"use client";

import { DatePicker, Form, Input, InputNumber, Modal, Select } from "antd";
import dayjs from "dayjs";
import type { Dayjs } from "dayjs";
import { useEffect } from "react";
import { FileUploadField } from "@/components/file/FileUploadField";
import type { MapPoint, MapPointPayload } from "@/lib/map-points";

const { TextArea } = Input;

interface HerbPointFormValues
  extends Omit<MapPointPayload, "lastCollectedAt" | "longitude" | "latitude"> {
  longitude: number;
  latitude: number;
  lastCollectedAt?: Dayjs;
}

interface HerbPointFormModalProps {
  open: boolean;
  mode: "create" | "edit";
  initialPoint?: Partial<MapPoint>;
  onCancel: () => void;
  onSubmit: (payload: MapPointPayload) => Promise<void>;
}

export function HerbPointFormModal({
  open,
  mode,
  initialPoint,
  onCancel,
  onSubmit,
}: HerbPointFormModalProps) {
  const [form] = Form.useForm<HerbPointFormValues>();

  useEffect(() => {
    if (!open) {
      return;
    }

    form.setFieldsValue({
      speciesId: initialPoint?.speciesId,
      herbName: initialPoint?.herbName,
      aliasName: initialPoint?.aliasName,
      latinName: initialPoint?.latinName,
      medicinalPart: initialPoint?.medicinalPart,
      efficacy: initialPoint?.efficacy,
      growthEnvironment: initialPoint?.growthEnvironment,
      originArea: initialPoint?.originArea,
      growthCycle: initialPoint?.growthCycle,
      herbDescription: initialPoint?.herbDescription,
      baseId: initialPoint?.baseId,
      regionId: initialPoint?.regionId,
      locationName: initialPoint?.locationName,
      longitude: initialPoint?.longitude,
      latitude: initialPoint?.latitude,
      province: initialPoint?.province ?? "重庆市",
      city: initialPoint?.city ?? "重庆市",
      district: initialPoint?.district,
      address: initialPoint?.address,
      altitude: initialPoint?.altitude,
      distributionType: initialPoint?.distributionType ?? "cultivated",
      distributionLevel: initialPoint?.distributionLevel,
      distributionDesc: initialPoint?.distributionDesc,
      coverImageUrl: initialPoint?.coverImageUrl,
      lastCollectedAt: initialPoint?.lastCollectedAt ? dayjs(initialPoint.lastCollectedAt) : undefined,
      sourceType: initialPoint?.sourceType ?? "pc",
      dataSource: initialPoint?.dataSource ?? "map",
      remark: initialPoint?.remark,
    });
  }, [form, initialPoint, open]);

  async function handleOk() {
    const values = await form.validateFields();
    await onSubmit({
      ...values,
      lastCollectedAt: values.lastCollectedAt?.format("YYYY-MM-DDTHH:mm:ss"),
    });
  }

  return (
    <Modal
      title={mode === "create" ? "新增药材地图点位" : "编辑药材地图点位"}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText={mode === "create" ? "新增" : "保存"}
      cancelText="取消"
      destroyOnHidden
      width={720}
    >
      <Form form={form} layout="vertical" requiredMark="optional">
        <Form.Item
          name="herbName"
          label="药材名称"
          rules={[{ required: true, message: "请输入药材名称" }]}
        >
          <Input placeholder="如：黄连、党参、金银花" />
        </Form.Item>

        <div className="map-form-grid">
          <Form.Item name="aliasName" label="别名">
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item name="latinName" label="拉丁学名">
            <Input placeholder="可选" />
          </Form.Item>
        </div>

        <div className="map-form-grid">
          <Form.Item
            name="longitude"
            label="经度"
            rules={[{ required: true, message: "请在地图上选择点位" }]}
          >
            <InputNumber min={-180} max={180} precision={7} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item
            name="latitude"
            label="纬度"
            rules={[{ required: true, message: "请在地图上选择点位" }]}
          >
            <InputNumber min={-90} max={90} precision={7} style={{ width: "100%" }} />
          </Form.Item>
        </div>

        <div className="map-form-grid">
          <Form.Item name="district" label="区县">
            <Input placeholder="如：石柱县、武隆区、南川区" />
          </Form.Item>
          <Form.Item name="locationName" label="地点名称">
            <Input placeholder="如：黄水药材基地" />
          </Form.Item>
        </div>

        <Form.Item name="address" label="详细地址">
          <Input placeholder="地图点位对应的具体地址" />
        </Form.Item>

        <div className="map-form-grid">
          <Form.Item name="distributionType" label="分布类型">
            <Select
              options={[
                { value: "cultivated", label: "人工种植" },
                { value: "wild", label: "野生分布" },
                { value: "specimen", label: "标本点位" },
              ]}
            />
          </Form.Item>
          <Form.Item name="lastCollectedAt" label="最近采集时间">
            <DatePicker showTime style={{ width: "100%" }} />
          </Form.Item>
        </div>

        <Form.Item name="coverImageUrl" label="封面图片">
          <FileUploadField
            accept="image/*"
            maxSizeMB={10}
            fileUsage="cover"
            buttonText="上传本地照片"
          />
        </Form.Item>

        <Form.Item name="efficacy" label="功效主治">
          <TextArea rows={2} placeholder="按实际药材信息填写，可后续由药材档案维护" />
        </Form.Item>

        <Form.Item name="growthEnvironment" label="生长环境">
          <TextArea rows={2} placeholder="如海拔、气候、土壤等环境说明" />
        </Form.Item>

        <Form.Item name="distributionDesc" label="分布说明">
          <TextArea rows={2} placeholder="点位分布、基地情况或采集说明" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
