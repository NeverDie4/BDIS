"use client";

import { useAuthStore } from "@/stores/auth-store";
import { Card, Col, Row, Statistic, Typography } from "antd";

export default function DashboardPage() {
  const user = useAuthStore((state) => state.user);
  const menus = useAuthStore((state) => state.menus);

  return (
    <section className="page-section">
      <Typography.Title level={3}>后台首页</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="当前用户" value={user?.realName || user?.username || "-"} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="角色数量" value={user?.roleCodes?.length || 0} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="可访问菜单" value={menus.length} />
          </Card>
        </Col>
      </Row>
    </section>
  );
}
