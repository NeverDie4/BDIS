import { Empty } from "antd";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import type { EvaluationApplication } from "./types";
import { ApplicationDetailContent } from "./ApplicationDetailContent";

type ApplicationDetailDrawerProps = {
  application: EvaluationApplication | null;
  open: boolean;
  onClose: () => void;
};

export function ApplicationDetailDrawer({ application, open, onClose }: ApplicationDetailDrawerProps) {
  if (!application) return <DetailDrawer open={false} title="申报详情" onClose={onClose}><Empty description="暂无申报记录" /></DetailDrawer>;
  return <DetailDrawer open={open} title={application.title} width={500} onClose={onClose}><ApplicationDetailContent application={application} /></DetailDrawer>;
}
