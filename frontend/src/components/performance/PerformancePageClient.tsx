"use client";

import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileAddOutlined,
  DownloadOutlined,
  PlusOutlined,
  SendOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useInfiniteQuery, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  App,
  Button,
  Col,
  DatePicker,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
} from "antd";
import type { UploadProps } from "antd";
import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { deleteOwnUnboundUpload, uploadFile } from "@/lib/files";
import { resolveServerPagination, toPageRequest } from "@/lib/performance-pagination";
import { fetchProtectedFileBlob } from "@/lib/protected-files";
import { getApiErrorMessage } from "@/lib/request";
import {
  addParticipant,
  addPerformanceMaterial,
  auditPerformance,
  createPerformance,
  createStandard,
  createStandardVersion,
  disableStandard,
  fetchPerformance,
  fetchPerformances,
  fetchPerformanceStatistics,
  fetchPerformanceParticipantUsers,
  fetchStandards,
  publishStandard,
  removeParticipant,
  removePerformanceMaterial,
  submitPerformance,
  updateParticipant,
  updateStandard,
  updatePerformance,
  type PerformanceRecord,
  type PerformanceMaterial,
  type PerformanceParticipant,
  type PerformanceStandard,
  type ApiPage,
  type StandardPayload,
} from "@/lib/performance";
import { useAuthStore } from "@/stores/auth-store";
import styles from "./performance.module.css";

const typeOptions = [
  { label: "科研", value: "RESEARCH" },
  { label: "教学", value: "TEACHING" },
  { label: "培训", value: "TRAINING" },
  { label: "应用", value: "APPLICATION" },
];

const statusLabels: Record<string, string> = {
  draft: "草稿",
  submitted: "待审核",
  approved: "已通过",
  rejected: "待修改",
};

const lifecycleLabels: Record<string, string> = {
  draft: "草稿",
  published: "已发布",
  disabled: "已停用",
};

const auditActionLabels: Record<string, string> = {
  submit: "提交审核",
  approve: "通过认定",
  reject: "要求修改",
};

const materialUsageLabels: Record<string, string> = {
  material: "佐证材料",
};

type PerformanceFormValues = {
  performanceTitle: string;
  performanceType?: string;
  performanceLevel?: string;
  occurredAt?: dayjs.Dayjs;
  standardId?: number;
  sourceType?: string;
  sourceId?: number;
  remark?: string;
};

const sourceTypeOptions = [
  { label: "申报档案", value: "eval_application" },
  { label: "科研课题", value: "research_project" },
  { label: "实验课程", value: "edu_course" },
  { label: "实验记录", value: "edu_experiment_record" },
  { label: "培训计划", value: "edu_training_plan" },
];

type StandardFormValues = Omit<StandardPayload, "effectiveFrom" | "effectiveTo"> & {
  effectiveRange?: [dayjs.Dayjs, dayjs.Dayjs];
};

function toPerformancePayload(values: PerformanceFormValues) {
  return {
    ...values,
    occurredAt: values.occurredAt?.format("YYYY-MM-DDTHH:mm:ss"),
  };
}

function performanceTypeLabel(value?: string) {
  return typeOptions.find((item) => item.value === value)?.label ?? value ?? "-";
}

function participantRoleLabel(value?: string) {
  const labels: Record<string, string> = {
    owner: "负责人",
    participant: "参与人",
  };
  return value ? (labels[value] ?? value) : "参与人";
}

function participantLabel(participant: { userId?: number; username?: string; realName?: string }) {
  const name = participant.realName || participant.username;
  if (!name) return participant.userId ? `用户 #${participant.userId}` : "未知用户";
  return participant.username && participant.username !== name
    ? `${name}（${participant.username}）`
    : name;
}

function formatStandardRuleSnapshot(snapshot?: string) {
  if (!snapshot) return "提交前尚未冻结";
  const legacyLabels: Record<string, string> = {
    scoreRule: "认定规则",
    levelRule: "等级规则",
    materialRequired: "佐证材料要求",
    minMaterialCount: "最少材料数量",
  };
  return snapshot
    .split(/\r?\n/)
    .map((line) => {
      const separator = line.indexOf(":");
      if (separator < 0) return line;
      const key = line.slice(0, separator).trim();
      const value = line.slice(separator + 1).trim();
      const label = legacyLabels[key];
      if (!label) return line;
      if (key === "materialRequired") {
        return `${label}：${value === "1" || value === "true" ? "需要提供材料" : "无需提供材料"}`;
      }
      return `${label}：${key === "minMaterialCount" && value ? `${value} 份` : value}`;
    })
    .join("\n");
}

function getNextPageParam<T>(lastPage: ApiPage<T>, pages: ApiPage<T>[]) {
  const loadedCount = pages.reduce(
    (count, page) => count + (Array.isArray(page?.records) ? page.records.length : 0),
    0,
  );
  const total = typeof lastPage?.total === "number" && lastPage.total > 0 ? lastPage.total : 0;
  return loadedCount < total ? pages.length + 1 : undefined;
}

function statusTag(status: string, className?: string) {
  const color =
    status === "approved"
      ? "green"
      : status === "rejected"
        ? "red"
        : status === "submitted"
          ? "gold"
          : "default";
  const detailStatusClass =
    status === "draft"
      ? styles.statusDraft
      : status === "submitted"
        ? styles.statusSubmitted
        : status === "approved"
          ? styles.statusApproved
          : status === "rejected"
            ? styles.statusRejected
            : undefined;
  return (
    <Tag
      className={[className, className ? detailStatusClass : undefined].filter(Boolean).join(" ")}
      color={color}
    >
      {statusLabels[status] ?? status}
    </Tag>
  );
}

export function PerformancePageClient() {
  const { message, modal } = App.useApp();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const currentUser = useAuthStore((state) => state.user);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const canCreate = hasPermission("performance:record:create");
  const canUpdate = hasPermission("performance:record:update");
  const canSubmit = hasPermission("performance:record:submit");
  const canAudit = hasPermission("performance:record:audit");
  const canManageStandards = hasPermission("performance:standard:manage");
  const [activeTab, setActiveTab] = useState("records");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string>();
  const [recordPagination, setRecordPagination] = useState({ current: 1, pageSize: 10 });
  const [standardPagination, setStandardPagination] = useState({ current: 1, pageSize: 10 });
  const [standardKeyword, setStandardKeyword] = useState("");
  const [standardType, setStandardType] = useState<string>();
  const [standardLifecycle, setStandardLifecycle] = useState<string>();
  const [standardSearch, setStandardSearch] = useState("");
  const [participantSearch, setParticipantSearch] = useState("");
  const [recordModalOpen, setRecordModalOpen] = useState(false);
  const [recordDrawerEditing, setRecordDrawerEditing] = useState(false);
  const [standardModalOpen, setStandardModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<PerformanceRecord>();
  const [versionSource, setVersionSource] = useState<PerformanceStandard>();
  const [editingStandard, setEditingStandard] = useState<PerformanceStandard>();
  const [selectedStandard, setSelectedStandard] = useState<PerformanceStandard>();
  const [selectedId, setSelectedId] = useState<number>();
  const [reviewingRecord, setReviewingRecord] = useState<PerformanceRecord>();
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [editingParticipant, setEditingParticipant] = useState<{
    id: number;
    userId: number;
    participantRole: string;
    sortOrder?: number;
  }>();
  const [recordForm] = Form.useForm<PerformanceFormValues>();
  const [standardForm] = Form.useForm<StandardFormValues>();
  const materialRequired = Form.useWatch("materialRequired", standardForm);
  const [reviewForm] = Form.useForm<{ comment?: string }>();
  const [participantForm] = Form.useForm<{
    userId: number;
    participantRole: string;
    sortOrder?: number;
  }>();
  const currentUserId = currentUser?.userId ?? null;

  const records = useQuery({
    queryKey: ["performance", "records", currentUserId, keyword, status, recordPagination],
    queryFn: () =>
      fetchPerformances({
        ...toPageRequest(recordPagination),
        keyword: keyword || undefined,
        identifyStatus: status,
      }),
  });
  const standards = useQuery({
    queryKey: [
      "performance",
      "standards",
      currentUserId,
      standardKeyword,
      standardType,
      standardLifecycle,
      standardPagination,
    ],
    queryFn: () =>
      fetchStandards({
        ...toPageRequest(standardPagination),
        keyword: standardKeyword || undefined,
        performanceType: standardType,
        lifecycleStatus: standardLifecycle,
      }),
  });
  const publishedStandardOptions = useInfiniteQuery({
    queryKey: ["performance", "published-standard-options", currentUserId, standardSearch],
    initialPageParam: 1,
    queryFn: ({ pageParam }) =>
      fetchStandards({
        pageNum: pageParam,
        pageSize: 20,
        keyword: standardSearch || undefined,
        lifecycleStatus: "published",
      }),
    getNextPageParam,
  });
  const participantCandidates = useInfiniteQuery({
    queryKey: ["performance", "participant-users", currentUserId, selectedId, participantSearch],
    initialPageParam: 1,
    queryFn: ({ pageParam }) =>
      fetchPerformanceParticipantUsers(selectedId as number, {
        pageNum: pageParam,
        pageSize: 20,
        keyword: participantSearch || undefined,
      }),
    getNextPageParam,
    enabled: canUpdate && Boolean(selectedId),
  });
  const statistics = useQuery({
    queryKey: ["performance", "statistics", currentUserId],
    queryFn: () => fetchPerformanceStatistics({}),
  });
  const detail = useQuery({
    queryKey: ["performance", "detail", currentUserId, selectedId],
    queryFn: () => fetchPerformance(selectedId as number),
    enabled: Boolean(selectedId),
  });

  const publishedStandards = useMemo(
    () => publishedStandardOptions.data?.pages.flatMap((page) => page.records) ?? [],
    [publishedStandardOptions.data],
  );
  const participantUsers = useMemo(
    () =>
      participantCandidates.data?.pages.flatMap((page) =>
        Array.isArray(page?.records) ? page.records : [],
      ) ?? [],
    [participantCandidates.data],
  );
  const participantOptions = useMemo(() => {
    const options = participantUsers.map((user) => ({
      value: user.id,
      label: participantLabel(user),
    }));
    const editingUser = editingParticipant
      ? detail.data?.participants.find((participant) => participant.id === editingParticipant.id)
      : undefined;
    if (editingUser && !options.some((option) => option.value === editingUser.userId)) {
      options.unshift({ value: editingUser.userId, label: participantLabel(editingUser) });
    }
    return options;
  }, [detail.data?.participants, editingParticipant, participantUsers]);
  const participantDetails = useMemo(
    () =>
      detail.data?.participants.map((participant) => {
        if (participant.realName || participant.username) return participant;
        const candidate = participantUsers.find((user) => user.id === participant.userId);
        const isCurrentUser = participant.userId === currentUser?.userId;
        return {
          ...participant,
          username: candidate?.username ?? (isCurrentUser ? currentUser.username : undefined),
          realName: candidate?.realName ?? (isCurrentUser ? currentUser.realName : undefined),
        };
      }) ?? [],
    [currentUser, detail.data?.participants, participantUsers],
  );

  useEffect(() => {
    const requestedId = Number(searchParams.get("performanceId"));
    if (Number.isSafeInteger(requestedId) && requestedId > 0) {
      setSelectedId(requestedId);
    }
  }, [searchParams]);

  useEffect(() => {
    if (!recordModalOpen || editingRecord) return;
    recordForm.resetFields();
  }, [editingRecord, recordForm, recordModalOpen]);

  useEffect(() => {
    if (!recordDrawerEditing || !editingRecord) return;
    recordForm.setFieldsValue({
      ...editingRecord,
      occurredAt: editingRecord.occurredAt ? dayjs(editingRecord.occurredAt) : undefined,
    });
  }, [editingRecord, recordDrawerEditing, recordForm]);

  useEffect(() => {
    if (!standardModalOpen) return;
    const source = editingStandard ?? versionSource;
    standardForm.resetFields();
    standardForm.setFieldsValue(
      source
        ? {
            standardName: source.standardName,
            performanceType: source.performanceType,
            standardDesc: source.standardDesc,
            scoreRule: source.scoreRule,
            levelRule: source.levelRule,
            materialRequired: source.materialRequired === 1,
            minMaterialCount: source.minMaterialCount,
            sortOrder: source.sortOrder,
            remark: source.remark,
            effectiveRange:
              source.effectiveFrom && source.effectiveTo
                ? [dayjs(source.effectiveFrom), dayjs(source.effectiveTo)]
                : undefined,
          }
        : { materialRequired: true, minMaterialCount: 1 },
    );
  }, [editingStandard, standardForm, standardModalOpen, versionSource]);

  useEffect(() => {
    if (reviewingRecord) reviewForm.resetFields();
  }, [reviewForm, reviewingRecord]);

  useEffect(() => {
    setEditingParticipant(undefined);
    setParticipantSearch("");
  }, [selectedId]);

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["performance", "records"] }),
      queryClient.invalidateQueries({ queryKey: ["performance", "statistics"] }),
      queryClient.invalidateQueries({
        queryKey: ["performance", "detail", currentUserId, selectedId],
      }),
    ]);
  };

  const openCreateRecord = () => {
    setEditingRecord(undefined);
    setRecordDrawerEditing(false);
    setRecordModalOpen(true);
  };

  const prepareRecordEdit = (record: PerformanceRecord) => {
    setEditingRecord(record);
  };

  const openEditRecord = (record: PerformanceRecord) => {
    prepareRecordEdit(record);
    setSelectedId(record.id);
    setRecordDrawerEditing(false);
  };

  const saveRecord = async () => {
    try {
      const values = await recordForm.validateFields();
      if (editingRecord) {
        await updatePerformance(editingRecord.id, toPerformancePayload(values));
        message.success("业绩已保存");
      } else {
        const created = await createPerformance(toPerformancePayload(values));
        message.success("业绩草稿已创建，请补充材料后提交");
        setSelectedId(created.id);
      }
      setRecordModalOpen(false);
      setRecordDrawerEditing(false);
      await refresh();
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "保存业绩失败"));
    }
  };

  const submit = (record: PerformanceRecord) => {
    modal.confirm({
      title: "提交业绩审核",
      content: "提交后将冻结当前标准快照和材料清单，等待审核。",
      okText: "提交",
      onOk: async () => {
        try {
          await submitPerformance(record.id);
          message.success("业绩已提交审核");
          await refresh();
        } catch (error) {
          message.error(getApiErrorMessage(error, "提交失败"));
        }
      },
    });
  };

  const openReview = (record: PerformanceRecord) => {
    setReviewingRecord(record);
  };

  const review = async (decision: "approved" | "rejected") => {
    if (!reviewingRecord) return;
    setReviewSubmitting(true);
    try {
      const values =
        decision === "rejected"
          ? await reviewForm.validateFields(["comment"])
          : reviewForm.getFieldsValue();
      await auditPerformance(reviewingRecord.id, decision, values.comment);
      message.success(
        decision === "approved" ? "业绩已通过认定" : "业绩已标记为待修改，请修改后重新提交",
      );
      setReviewingRecord(undefined);
      await refresh();
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "审核失败"));
    } finally {
      setReviewSubmitting(false);
    }
  };

  const saveParticipant = async () => {
    if (!selectedId) return;
    try {
      const values = await participantForm.validateFields();
      if (editingParticipant) {
        await updateParticipant(selectedId, editingParticipant.id, values);
      } else {
        await addParticipant(selectedId, values);
      }
      participantForm.resetFields();
      setEditingParticipant(undefined);
      message.success(editingParticipant ? "参与人已更新" : "参与人已添加");
      await queryClient.invalidateQueries({
        queryKey: ["performance", "detail", currentUserId, selectedId],
      });
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "添加参与人失败"));
    }
  };

  const openParticipantEdit = (participant: {
    id: number;
    userId: number;
    participantRole: string;
    sortOrder?: number;
  }) => {
    setEditingParticipant(participant);
    participantForm.setFieldsValue({
      userId: participant.userId,
      participantRole: participant.participantRole,
      sortOrder: participant.sortOrder,
    });
  };

  const materialUploadProps: UploadProps = {
    showUploadList: false,
    customRequest: async (options) => {
      if (!selectedId) return;
      let uploadedFileId: number | undefined;
      try {
        const file = options.file as File;
        const uploaded = await uploadFile(file, { accessLevel: "private" });
        uploadedFileId = uploaded.id;
        await addPerformanceMaterial(selectedId, uploaded.id);
        options.onSuccess?.(uploaded);
        message.success("佐证材料已关联");
        await queryClient.invalidateQueries({
          queryKey: ["performance", "detail", currentUserId, selectedId],
        });
      } catch (error) {
        if (uploadedFileId) {
          void deleteOwnUnboundUpload(uploadedFileId).catch(() => undefined);
        }
        options.onError?.(error as Error);
        message.error(getApiErrorMessage(error, "上传材料失败"));
      }
    },
  };

  const openProtectedFile = async (fileId: number, download = false) => {
    try {
      const blob = await fetchProtectedFileBlob(
        `/files/${fileId}/content?disposition=${download ? "attachment" : "inline"}`,
      );
      const objectUrl = URL.createObjectURL(blob);
      if (download) {
        const anchor = document.createElement("a");
        anchor.href = objectUrl;
        anchor.download = `业绩材料-${fileId}`;
        anchor.click();
      } else {
        window.open(objectUrl, "_blank", "noopener,noreferrer");
      }
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    } catch (error) {
      message.error(getApiErrorMessage(error, "读取材料失败"));
    }
  };

  const removeMaterial = async (relationId: number) => {
    if (!selectedId) return;
    try {
      await removePerformanceMaterial(selectedId, relationId);
      message.success("材料关联已删除");
      await queryClient.invalidateQueries({
        queryKey: ["performance", "detail", currentUserId, selectedId],
      });
    } catch (error) {
      message.error(getApiErrorMessage(error, "删除材料失败"));
    }
  };

  const removeParticipantFromRecord = async (participantId: number) => {
    if (!selectedId) return;
    try {
      await removeParticipant(selectedId, participantId);
      if (editingParticipant?.id === participantId) {
        participantForm.resetFields();
        setEditingParticipant(undefined);
      }
      message.success("参与人已移除");
      await queryClient.invalidateQueries({
        queryKey: ["performance", "detail", currentUserId, selectedId],
      });
    } catch (error) {
      message.error(getApiErrorMessage(error, "移除参与人失败"));
    }
  };

  const openStandardModal = (source?: PerformanceStandard, edit = false) => {
    setVersionSource(edit ? undefined : source);
    setEditingStandard(edit ? source : undefined);
    setStandardModalOpen(true);
  };

  const saveStandard = async () => {
    try {
      const values = await standardForm.validateFields();
      const { effectiveRange, ...rest } = values;
      const payload: StandardPayload = {
        ...rest,
        effectiveFrom: effectiveRange?.[0]?.format("YYYY-MM-DDTHH:mm:ss"),
        effectiveTo: effectiveRange?.[1]?.format("YYYY-MM-DDTHH:mm:ss"),
      };
      if (editingStandard) {
        await updateStandard(editingStandard.id, payload);
        message.success("标准草稿已更新");
      } else if (versionSource) {
        await createStandardVersion(versionSource.id, payload);
        message.success("新版本标准已创建为草稿");
      } else {
        await createStandard(payload);
        message.success("标准草稿已创建");
      }
      setStandardModalOpen(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["performance", "standards"] }),
        queryClient.invalidateQueries({ queryKey: ["performance", "published-standard-options"] }),
      ]);
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "保存标准失败"));
    }
  };

  const changeStandardLifecycle = async (
    standard: PerformanceStandard,
    action: "publish" | "disable",
  ) => {
    try {
      if (action === "publish") {
        await publishStandard(standard.id);
        message.success("标准已发布");
      } else {
        await disableStandard(standard.id);
        message.success("标准已停用");
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["performance", "standards"] }),
        queryClient.invalidateQueries({ queryKey: ["performance", "published-standard-options"] }),
      ]);
    } catch (error) {
      message.error(
        getApiErrorMessage(error, action === "publish" ? "发布标准失败" : "停用标准失败"),
      );
    }
  };

  const recordColumns = [
    { title: "业绩编号", dataIndex: "performanceNo", width: 185 },
    { title: "业绩名称", dataIndex: "performanceTitle", ellipsis: true },
    {
      title: "类别",
      dataIndex: "performanceType",
      width: 110,
      render: (value: string) => (
        <span className={styles.typeCell}>{performanceTypeLabel(value)}</span>
      ),
    },
    { title: "等级", dataIndex: "performanceLevel", width: 100 },
    {
      title: "发生时间",
      dataIndex: "occurredAt",
      width: 170,
      render: (value?: string) => (value ? dayjs(value).format("YYYY-MM-DD") : "-"),
    },
    {
      title: "状态",
      dataIndex: "identifyStatus",
      width: 100,
      render: (value: string) => statusTag(value),
    },
    {
      title: "操作",
      key: "actions",
      width: 190,
      render: (_: unknown, record: PerformanceRecord) => {
        const editable = record.identifyStatus === "draft" || record.identifyStatus === "rejected";
        return (
          <Space size={4} wrap>
            {canUpdate && editable ? (
              <Button type="link" icon={<EditOutlined />} onClick={() => openEditRecord(record)}>
                编辑
              </Button>
            ) : (
              <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedId(record.id)}>
                查看
              </Button>
            )}
            {canSubmit && editable && record.userId === currentUser?.userId ? (
              <Button type="link" icon={<SendOutlined />} onClick={() => submit(record)}>
                提交
              </Button>
            ) : null}
            {canAudit &&
            record.identifyStatus === "submitted" &&
            record.userId !== currentUser?.userId ? (
              <Button type="link" onClick={() => openReview(record)}>
                审核
              </Button>
            ) : null}
          </Space>
        );
      },
    },
  ];

  const standardColumns = [
    { title: "标准编号", dataIndex: "standardNo", width: 160 },
    {
      title: "版本",
      dataIndex: "standardVersion",
      width: 70,
      render: (value: number) => `v${value}`,
    },
    {
      title: "名称",
      dataIndex: "standardName",
      ellipsis: true,
      render: (value: string, record: PerformanceStandard) => (
        <Button
          className={styles.standardNameButton}
          type="link"
          onClick={() => setSelectedStandard(record)}
        >
          {value}
        </Button>
      ),
    },
    {
      title: "类别",
      dataIndex: "performanceType",
      width: 110,
      render: (value: string) => (
        <span className={styles.typeCell}>{performanceTypeLabel(value)}</span>
      ),
    },
    {
      title: "材料",
      width: 110,
      render: (_: unknown, record: PerformanceStandard) =>
        record.materialRequired ? `至少 ${record.minMaterialCount} 份` : "非必填",
    },
    {
      title: "状态",
      dataIndex: "lifecycleStatus",
      width: 100,
      render: (value: string) => <Tag>{lifecycleLabels[value] ?? value}</Tag>,
    },
    {
      title: "操作",
      width: 290,
      render: (_: unknown, record: PerformanceStandard) => (
        <Space size={4} wrap>
          <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedStandard(record)}>
            查看详情
          </Button>
          {canManageStandards ? (
            <>
              {record.lifecycleStatus === "draft" ? (
                <>
                  <Button
                    type="link"
                    icon={<EditOutlined />}
                    onClick={() => openStandardModal(record, true)}
                  >
                    编辑
                  </Button>
                  <Button
                    type="link"
                    onClick={() => void changeStandardLifecycle(record, "publish")}
                  >
                    发布
                  </Button>
                </>
              ) : null}
              {record.lifecycleStatus === "published" ? (
                <Button
                  danger
                  type="link"
                  onClick={() => void changeStandardLifecycle(record, "disable")}
                >
                  停用
                </Button>
              ) : null}
              {record.lifecycleStatus !== "draft" ? (
                <Button type="link" onClick={() => openStandardModal(record)}>
                  新版本
                </Button>
              ) : null}
            </>
          ) : null}
        </Space>
      ),
    },
  ];

  const materialColumns = [
    { title: "文件 ID", dataIndex: "fileId" },
    {
      title: "用途",
      dataIndex: "fileUsage",
      render: (value?: string) => (value ? (materialUsageLabels[value] ?? value) : "佐证材料"),
    },
    { title: "排序", dataIndex: "sortOrder" },
    {
      title: "操作",
      render: (_: unknown, material: PerformanceMaterial) => (
        <Space size={0}>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => void openProtectedFile(material.fileId)}
          >
            预览
          </Button>
          <Button
            type="link"
            icon={<DownloadOutlined />}
            onClick={() => void openProtectedFile(material.fileId, true)}
          >
            下载
          </Button>
          {canUpdate &&
          detail.data &&
          ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
            <Popconfirm title="删除该材料关联？" onConfirm={() => void removeMaterial(material.id)}>
              <Button danger type="link" icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <ModuleHeroBanner
        className={styles.moduleHero}
        eyebrow="PERFORMANCE RECOGNITION"
        sealText="认定"
        title="业绩认定"
        description="管理工作业绩、证明材料、标准版本和审核认定记录。"
      />
      <section className={styles.statsSection} aria-label="业绩认定概览">
        <div className={styles.sectionHeader}>
          <h2>认定进度概览</h2>
        </div>
        <div className={styles.statsGrid}>
          <div className={styles.statItem}>
            <span>全部业绩</span>
            <strong>{statistics.data?.totalCount ?? 0}</strong>
          </div>
          <div className={styles.statItem}>
            <span>草稿</span>
            <strong>{statistics.data?.draftCount ?? 0}</strong>
          </div>
          <div className={styles.statItem}>
            <span>待审核</span>
            <strong>{statistics.data?.submittedCount ?? 0}</strong>
          </div>
          <div className={styles.statItem}>
            <span>已通过</span>
            <strong>{statistics.data?.approvedCount ?? 0}</strong>
          </div>
          <div className={styles.statItem}>
            <span>待修改</span>
            <strong>{statistics.data?.rejectedCount ?? 0}</strong>
          </div>
        </div>
      </section>
      <section className={styles.workspace} aria-label="业绩认定工作区">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: "records",
              label: "业绩工作台",
              children: (
                <div className={styles.tabContent}>
                  <div className={styles.recordsToolbar}>
                    <Space className={styles.filters} wrap>
                      <Input
                        allowClear
                        placeholder="搜索业绩名称"
                        value={keyword}
                        onChange={(event) => {
                          setKeyword(event.target.value);
                          setRecordPagination((current) => ({ ...current, current: 1 }));
                        }}
                      />
                      <Select
                        allowClear
                        placeholder="全部状态"
                        options={Object.entries(statusLabels).map(([value, label]) => ({
                          value,
                          label,
                        }))}
                        value={status}
                        onChange={(value) => {
                          setStatus(value);
                          setRecordPagination((current) => ({ ...current, current: 1 }));
                        }}
                      />
                    </Space>
                    {canCreate ? (
                      <Button icon={<PlusOutlined />} type="primary" onClick={openCreateRecord}>
                        新增业绩
                      </Button>
                    ) : null}
                  </div>
                  {records.isError ? (
                    <Alert
                      showIcon
                      type="error"
                      message={getApiErrorMessage(records.error, "业绩列表加载失败")}
                    />
                  ) : null}
                  <Table
                    className={styles.dataTable}
                    rowKey="id"
                    columns={recordColumns}
                    dataSource={records.data?.records ?? []}
                    loading={records.isLoading}
                    pagination={{
                      current: recordPagination.current,
                      pageSize: recordPagination.pageSize,
                      total: records.data?.total ?? 0,
                      showSizeChanger: true,
                      onChange: (current, pageSize) =>
                        setRecordPagination((previous) =>
                          resolveServerPagination(
                            previous.current,
                            previous.pageSize,
                            current,
                            pageSize,
                          ),
                        ),
                    }}
                    scroll={{ x: 980 }}
                  />
                </div>
              ),
            },
            {
              key: "standards",
              label: "认定标准",
              children: (
                <div className={styles.tabContent}>
                  <div className={styles.standardToolbar}>
                    <Space className={styles.filters} wrap>
                      <Input
                        allowClear
                        placeholder="搜索标准名称"
                        value={standardKeyword}
                        onChange={(event) => {
                          setStandardKeyword(event.target.value);
                          setStandardPagination((current) => ({ ...current, current: 1 }));
                        }}
                      />
                      <Select
                        allowClear
                        placeholder="全部类别"
                        options={typeOptions}
                        value={standardType}
                        onChange={(value) => {
                          setStandardType(value);
                          setStandardPagination((current) => ({ ...current, current: 1 }));
                        }}
                      />
                      <Select
                        allowClear
                        placeholder="全部状态"
                        options={Object.entries(lifecycleLabels).map(([value, label]) => ({
                          value,
                          label,
                        }))}
                        value={standardLifecycle}
                        onChange={(value) => {
                          setStandardLifecycle(value);
                          setStandardPagination((current) => ({ ...current, current: 1 }));
                        }}
                      />
                    </Space>
                    {canManageStandards ? (
                      <Button
                        icon={<FileAddOutlined />}
                        type="primary"
                        onClick={() => openStandardModal()}
                      >
                        新增标准
                      </Button>
                    ) : null}
                  </div>
                  {standards.isError ? (
                    <Alert
                      showIcon
                      type="error"
                      message={getApiErrorMessage(standards.error, "认定标准加载失败")}
                    />
                  ) : null}
                  <Table
                    className={styles.dataTable}
                    rowKey="id"
                    columns={standardColumns}
                    dataSource={standards.data?.records ?? []}
                    loading={standards.isLoading}
                    pagination={{
                      current: standardPagination.current,
                      pageSize: standardPagination.pageSize,
                      total: standards.data?.total ?? 0,
                      showSizeChanger: true,
                      onChange: (current, pageSize) =>
                        setStandardPagination((previous) =>
                          resolveServerPagination(
                            previous.current,
                            previous.pageSize,
                            current,
                            pageSize,
                          ),
                        ),
                    }}
                    scroll={{ x: 920 }}
                  />
                </div>
              ),
            },
          ]}
        />
      </section>

      <Modal
        title={editingRecord ? "编辑业绩" : "新增业绩"}
        open={recordModalOpen}
        okText="保存草稿"
        onCancel={() => setRecordModalOpen(false)}
        onOk={() => void saveRecord()}
      >
        <Form form={recordForm} layout="vertical">
          <Form.Item
            label="业绩名称"
            name="performanceTitle"
            rules={[{ required: true, message: "请输入业绩名称" }]}
          >
            <Input maxLength={200} />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="业绩类别" name="performanceType">
                <Select options={typeOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="业绩等级" name="performanceLevel">
                <Input placeholder="例如：省部级" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="发生时间" name="occurredAt">
                <DatePicker className={styles.fullWidth} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="认定标准" name="standardId">
                <Select
                  allowClear
                  filterOption={false}
                  loading={publishedStandardOptions.isLoading}
                  options={publishedStandards.map((standard) => ({
                    value: standard.id,
                    label: `${standard.standardName} v${standard.standardVersion}`,
                  }))}
                  showSearch
                  onSearch={setStandardSearch}
                  onPopupScroll={(event) => {
                    const target = event.currentTarget;
                    if (
                      target.scrollHeight - target.scrollTop - target.clientHeight < 24 &&
                      publishedStandardOptions.hasNextPage &&
                      !publishedStandardOptions.isFetchingNextPage
                    ) {
                      void publishedStandardOptions.fetchNextPage();
                    }
                  }}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="来源类型" name="sourceType">
                <Select allowClear options={sourceTypeOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="来源 ID" name="sourceId">
                <InputNumber className={styles.fullWidth} min={1} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="说明" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          editingStandard
            ? `编辑 ${editingStandard.standardNo} 草稿`
            : versionSource
              ? `创建 ${versionSource.standardNo} 新版本`
              : "新增认定标准"
        }
        open={standardModalOpen}
        okText="保存草稿"
        onCancel={() => setStandardModalOpen(false)}
        onOk={() => void saveStandard()}
      >
        <Form form={standardForm} layout="vertical">
          <Form.Item
            label="标准名称"
            name="standardName"
            rules={[{ required: true, message: "请输入标准名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            label="业绩类别"
            name="performanceType"
            rules={[{ required: true, message: "请选择类别" }]}
          >
            <Select options={typeOptions} />
          </Form.Item>
          <Form.Item label="适用时间" name="effectiveRange">
            <DatePicker.RangePicker className={styles.fullWidth} showTime />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="要求材料" name="materialRequired">
                <Select
                  options={[
                    { value: true, label: "必须提供" },
                    { value: false, label: "无需材料" },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="最少材料数" name="minMaterialCount">
                <InputNumber
                  className={styles.fullWidth}
                  disabled={materialRequired === false}
                  min={materialRequired === false ? 0 : 1}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="认定规则" name="scoreRule">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="等级规则" name="levelRule">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="说明" name="standardDesc">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`审核：${reviewingRecord?.performanceTitle ?? ""}`}
        open={Boolean(reviewingRecord)}
        closable={!reviewSubmitting}
        maskClosable={!reviewSubmitting}
        onCancel={() => {
          if (!reviewSubmitting) setReviewingRecord(undefined);
        }}
        footer={
          <Space>
            <Button disabled={reviewSubmitting} onClick={() => setReviewingRecord(undefined)}>
              取消
            </Button>
            <Button danger loading={reviewSubmitting} onClick={() => void review("rejected")}>
              要求修改
            </Button>
            <Button
              type="primary"
              loading={reviewSubmitting}
              onClick={() => void review("approved")}
            >
              通过认定
            </Button>
          </Space>
        }
      >
        <Form form={reviewForm} layout="vertical">
          <Form.Item
            label="审核意见"
            name="comment"
            rules={[{ required: true, message: "要求修改时必须填写审核意见" }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        className={styles.detailDrawer}
        title={
          <div className={styles.detailDrawerTitle}>
            <strong>标准详情</strong>
          </div>
        }
        open={Boolean(selectedStandard)}
        onClose={() => setSelectedStandard(undefined)}
        width={640}
      >
        {selectedStandard ? (
          <div className={styles.detailContent}>
            <section className={styles.detailSummary}>
              <div>
                <span className={styles.detailRecordNo}>
                  {selectedStandard.standardNo} · v{selectedStandard.standardVersion}
                </span>
                <h2>{selectedStandard.standardName}</h2>
              </div>
              <div className={styles.detailStatus}>
                <span>当前状态</span>
                <Tag
                  className={`${styles.standardStatusTag} ${
                    selectedStandard.lifecycleStatus === "published"
                      ? styles.standardStatusPublished
                      : selectedStandard.lifecycleStatus === "disabled"
                        ? styles.standardStatusDisabled
                        : styles.standardStatusDraft
                  }`}
                >
                  {lifecycleLabels[selectedStandard.lifecycleStatus] ??
                    selectedStandard.lifecycleStatus}
                </Tag>
              </div>
            </section>

            <section className={styles.detailSection}>
              <div className={styles.detailSectionHeader}>
                <div>
                  <h3>标准信息</h3>
                </div>
              </div>
              <dl className={styles.detailFacts}>
                <div>
                  <dt>业绩类别</dt>
                  <dd>{performanceTypeLabel(selectedStandard.performanceType)}</dd>
                </div>
                <div>
                  <dt>材料要求</dt>
                  <dd>
                    {selectedStandard.materialRequired
                      ? `至少 ${selectedStandard.minMaterialCount} 份材料`
                      : "无需材料"}
                  </dd>
                </div>
                <div className={styles.detailStandard}>
                  <dt>适用时间</dt>
                  <dd>
                    {selectedStandard.effectiveFrom
                      ? dayjs(selectedStandard.effectiveFrom).format("YYYY-MM-DD HH:mm")
                      : "不限开始时间"}
                    {" 至 "}
                    {selectedStandard.effectiveTo
                      ? dayjs(selectedStandard.effectiveTo).format("YYYY-MM-DD HH:mm")
                      : "不限结束时间"}
                  </dd>
                </div>
              </dl>
            </section>

            <section className={styles.detailSection}>
              <div className={styles.detailSectionHeader}>
                <div>
                  <h3>认定规则</h3>
                </div>
              </div>
              <dl className={styles.detailFacts}>
                <div className={styles.detailRule}>
                  <dt>认定规则</dt>
                  <dd>{selectedStandard.scoreRule || "暂未配置"}</dd>
                </div>
                <div className={styles.detailRule}>
                  <dt>等级规则</dt>
                  <dd>{selectedStandard.levelRule || "暂未配置"}</dd>
                </div>
                <div className={styles.detailRule}>
                  <dt>说明</dt>
                  <dd>{selectedStandard.standardDesc || "暂无说明"}</dd>
                </div>
              </dl>
            </section>
          </div>
        ) : null}
      </Drawer>

      <Drawer
        className={styles.detailDrawer}
        title={
          <div className={styles.detailDrawerTitle}>
            <strong>{recordDrawerEditing ? "编辑业绩" : "业绩详情"}</strong>
          </div>
        }
        open={Boolean(selectedId)}
        extra={
          recordDrawerEditing ? (
            <Space size={8}>
              <Button
                onClick={() => {
                  setRecordDrawerEditing(false);
                  setEditingRecord(undefined);
                  recordForm.resetFields();
                }}
              >
                取消
              </Button>
              <Button type="primary" onClick={() => void saveRecord()}>
                保存
              </Button>
            </Space>
          ) : canUpdate &&
            detail.data &&
            ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
            <Button
              icon={<EditOutlined />}
              onClick={() => {
                prepareRecordEdit(detail.data.performance);
                setRecordDrawerEditing(true);
              }}
            >
              编辑基本信息
            </Button>
          ) : undefined
        }
        onClose={() => {
          setSelectedId(undefined);
          setRecordDrawerEditing(false);
          setEditingRecord(undefined);
          setEditingParticipant(undefined);
          setParticipantSearch("");
        }}
        width={720}
      >
        {detail.isError ? (
          <Alert
            showIcon
            type="error"
            message={getApiErrorMessage(detail.error, "业绩详情加载失败")}
          />
        ) : recordDrawerEditing && editingRecord ? (
          <Form className={styles.drawerRecordForm} form={recordForm} layout="vertical">
            <Form.Item
              label="业绩名称"
              name="performanceTitle"
              rules={[{ required: true, message: "请输入业绩名称" }]}
            >
              <Input maxLength={200} />
            </Form.Item>
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item label="业绩类别" name="performanceType">
                  <Select options={typeOptions} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="业绩等级" name="performanceLevel">
                  <Input placeholder="例如：省部级" />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item label="发生时间" name="occurredAt">
                  <DatePicker className={styles.fullWidth} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="认定标准" name="standardId">
                  <Select
                    allowClear
                    filterOption={false}
                    loading={publishedStandardOptions.isLoading}
                    options={publishedStandards.map((standard) => ({
                      value: standard.id,
                      label: `${standard.standardName} v${standard.standardVersion}`,
                    }))}
                    showSearch
                    onSearch={setStandardSearch}
                    onPopupScroll={(event) => {
                      const target = event.currentTarget;
                      if (
                        target.scrollHeight - target.scrollTop - target.clientHeight < 24 &&
                        publishedStandardOptions.hasNextPage &&
                        !publishedStandardOptions.isFetchingNextPage
                      ) {
                        void publishedStandardOptions.fetchNextPage();
                      }
                    }}
                  />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item label="来源类型" name="sourceType">
                  <Select allowClear options={sourceTypeOptions} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="来源 ID" name="sourceId">
                  <InputNumber className={styles.fullWidth} min={1} />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item label="说明" name="remark">
              <Input.TextArea rows={5} maxLength={500} />
            </Form.Item>
          </Form>
        ) : detail.data ? (
          <div className={styles.detailContent}>
            <section className={styles.detailSummary}>
              <div>
                <span className={styles.detailRecordNo}>
                  {detail.data.performance.performanceNo}
                </span>
                <h2>{detail.data.performance.performanceTitle}</h2>
              </div>
              <div className={styles.detailStatus}>
                <span>当前状态</span>
                {statusTag(detail.data.performance.identifyStatus, styles.detailStatusTag)}
              </div>
            </section>

            <section className={styles.detailSection}>
              <div className={styles.detailSectionHeader}>
                <div>
                  <h3>认定信息</h3>
                </div>
              </div>
              <dl className={styles.detailFacts}>
                <div>
                  <dt>发生时间</dt>
                  <dd>
                    {detail.data.performance.occurredAt
                      ? dayjs(detail.data.performance.occurredAt).format("YYYY-MM-DD HH:mm")
                      : "未填写"}
                  </dd>
                </div>
                <div>
                  <dt>业绩来源</dt>
                  <dd>
                    {detail.data.performance.sourceNameSnapshot
                      ? `${
                          sourceTypeOptions.find(
                            (option) => option.value === detail.data?.performance.sourceType,
                          )?.label ?? detail.data.performance.sourceType
                        }：${detail.data.performance.sourceNameSnapshot}`
                      : "手工填报"}
                  </dd>
                </div>
                <div className={styles.detailStandard}>
                  <dt>认定标准</dt>
                  <dd>
                    {detail.data.performance.standardNameSnapshot
                      ? `${detail.data.performance.standardNameSnapshot} v${detail.data.performance.standardVersionSnapshot}`
                      : "提交前尚未冻结"}
                  </dd>
                </div>
                <div className={styles.detailRule}>
                  <dt>认定规则快照</dt>
                  <dd>
                    {detail.data.performance.standardRuleSnapshot ? (
                      <Typography.Paragraph className={styles.snapshotRule}>
                        {formatStandardRuleSnapshot(detail.data.performance.standardRuleSnapshot)}
                      </Typography.Paragraph>
                    ) : (
                      "提交前尚未冻结"
                    )}
                  </dd>
                </div>
              </dl>
            </section>

            <section className={styles.detailSection}>
              <div className={styles.detailSectionHeader}>
                <div>
                  <h3>佐证材料</h3>
                </div>
                {canUpdate &&
                ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
                  <Upload {...materialUploadProps}>
                    <Button icon={<UploadOutlined />}>上传并关联材料</Button>
                  </Upload>
                ) : null}
              </div>
              <Table
                className={styles.detailTable}
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={detail.data.materials}
                columns={materialColumns}
              />
            </section>

            <section className={styles.detailSection}>
              <div className={styles.detailSectionHeader}>
                <div>
                  <h3>参与人</h3>
                </div>
              </div>
              {canUpdate &&
              ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
                <Form
                  key={selectedId}
                  clearOnDestroy
                  className={styles.participantForm}
                  form={participantForm}
                  layout="inline"
                  onFinish={() => void saveParticipant()}
                >
                  <Form.Item name="userId" rules={[{ required: true, message: "请选择参与人" }]}>
                    <Select
                      className={styles.participantSelect}
                      filterOption={false}
                      loading={
                        participantCandidates.isLoading || participantCandidates.isFetchingNextPage
                      }
                      options={participantOptions}
                      placeholder="输入姓名或账号搜索"
                      showSearch
                      onSearch={(value) => setParticipantSearch(value.trim())}
                      onOpenChange={(open) => {
                        if (!open) setParticipantSearch("");
                      }}
                      onPopupScroll={(event) => {
                        const target = event.currentTarget;
                        if (
                          target.scrollHeight - target.scrollTop - target.clientHeight < 24 &&
                          participantCandidates.hasNextPage &&
                          !participantCandidates.isFetchingNextPage
                        ) {
                          void participantCandidates.fetchNextPage();
                        }
                      }}
                    />
                  </Form.Item>
                  <Form.Item
                    name="participantRole"
                    rules={[{ required: true, message: "请输入参与角色" }]}
                  >
                    <Input placeholder="参与角色" />
                  </Form.Item>
                  <Form.Item name="sortOrder">
                    <InputNumber min={0} placeholder="排序" />
                  </Form.Item>
                  <Button
                    htmlType="submit"
                    icon={editingParticipant ? <EditOutlined /> : <PlusOutlined />}
                  >
                    {editingParticipant ? "保存" : "添加"}
                  </Button>
                  {editingParticipant ? (
                    <Button
                      onClick={() => {
                        participantForm.resetFields();
                        setEditingParticipant(undefined);
                      }}
                    >
                      取消
                    </Button>
                  ) : null}
                </Form>
              ) : null}
              <Table<PerformanceParticipant>
                className={styles.detailTable}
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={participantDetails}
                columns={[
                  {
                    title: "参与人",
                    render: (_: unknown, participant) => participantLabel(participant),
                  },
                  {
                    title: "角色",
                    dataIndex: "participantRole",
                    render: (value: string) => participantRoleLabel(value),
                  },
                  {
                    title: "操作",
                    render: (_: unknown, participant: PerformanceParticipant) =>
                      canUpdate &&
                      participant.isPrimary !== 1 &&
                      ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
                        <Space size={0}>
                          <Button type="link" onClick={() => openParticipantEdit(participant)}>
                            编辑
                          </Button>
                          <Popconfirm
                            title="移除参与人？"
                            onConfirm={() => void removeParticipantFromRecord(participant.id)}
                          >
                            <Button danger type="link">
                              移除
                            </Button>
                          </Popconfirm>
                        </Space>
                      ) : null,
                  },
                ]}
              />
            </section>

            <section className={styles.detailSection}>
              <div className={styles.detailSectionHeader}>
                <div>
                  <h3>审核历史</h3>
                </div>
              </div>
              <Table
                className={styles.detailTable}
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={detail.data.auditRecords}
                columns={[
                  {
                    title: "动作",
                    dataIndex: "identifyAction",
                    render: (value: string) => auditActionLabels[value] ?? value,
                  },
                  {
                    title: "结果",
                    dataIndex: "identifyResult",
                    render: (value: string) => statusTag(value),
                  },
                  { title: "意见", dataIndex: "identifyComment" },
                  {
                    title: "时间",
                    dataIndex: "identifiedAt",
                    render: (value?: string) =>
                      value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-",
                  },
                ]}
              />
            </section>
          </div>
        ) : null}
      </Drawer>
    </div>
  );
}
