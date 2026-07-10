"use client";

import { Button, Card, Descriptions, Modal, Select, Typography } from "antd";
import { useMemo, useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import { FilterPanel } from "@/components/common/FilterPanel";
import { InfoCard } from "@/components/common/InfoCard";
import { SearchBar } from "@/components/common/SearchBar";
import { StatusTag } from "@/components/common/StatusTag";
import { ChongqingMapPanel } from "@/components/feature/ChongqingMapPanel";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { herbDistributionPoints } from "@/mocks/herbs";
import type { HerbDistributionPoint } from "@/types/herb";
import styles from "@/styles/mockPages.module.css";

export default function MapPage() {
  const [keyword, setKeyword] = useState("");
  const [district, setDistrict] = useState("all");
  const [selectedPoint, setSelectedPoint] = useState<HerbDistributionPoint | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const districts = Array.from(new Set(herbDistributionPoints.map((point) => point.district)));
  const filteredPoints = useMemo(() => {
    return herbDistributionPoints.filter((point) => {
      const matchedKeyword =
        keyword.length === 0 ||
        point.herbName.includes(keyword) ||
        point.locationName.includes(keyword) ||
        point.baseName?.includes(keyword);
      const matchedDistrict = district === "all" || point.district === district;

      return matchedKeyword && matchedDistrict;
    });
  }, [district, keyword]);

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="CHONGQING DISTRIBUTION"
          title="重庆中药材分布地图"
          subtitle="以静态 SVG 和 mock 点位展示重庆区县、基地、经纬度和药材品种分布，后续可替换为 Leaflet。"
        />

        <FilterPanel
          description="先做前端 mock 筛选，不请求地图接口。"
          title="点位筛选"
          onReset={() => {
            setKeyword("");
            setDistrict("all");
          }}
        >
          <SearchBar placeholder="搜索药材、基地或地点" value={keyword} onChange={setKeyword} />
          <Select
            options={[
              { label: "全部区县", value: "all" },
              ...districts.map((item) => ({ label: item, value: item })),
            ]}
            style={{ minWidth: 160 }}
            value={district}
            onChange={setDistrict}
          />
        </FilterPanel>

        <section className={styles.mapGrid}>
          <ChongqingMapPanel
            markers={filteredPoints.map((point, index) => ({
              id: point.id,
              name: point.locationName,
              district: point.district,
              herbName: point.herbName,
              status: point.status === "pending" ? "pending" : point.status === "draft" ? "warning" : "normal",
              x: 22 + index * 14,
              y: 38 + (index % 3) * 13,
            }))}
            selectedId={selectedPoint?.id}
            onMarkerClick={(marker) => {
              const point = herbDistributionPoints.find((item) => item.id === marker.id) ?? null;
              setSelectedPoint(point);
            }}
          />

          <Card className={styles.panel} variant="borderless">
            <ActionToolbar
              actions={<Button type="primary" onClick={() => setModalOpen(true)}>新增点位</Button>}
              description="点击列表或地图点位查看详情。"
              title="分布点列表"
            />
            <div className={styles.cardList}>
              {filteredPoints.map((point) => (
                <InfoCard
                  extra={<StatusTag status={point.status} />}
                  key={point.id}
                  title={`${point.herbName} / ${point.district}`}
                >
                  <div className={styles.panelBody}>
                    <Typography.Text>{point.locationName}</Typography.Text>
                    <Typography.Text type="secondary">{point.distributionType} / {point.resourceScale}</Typography.Text>
                    <Button type="link" onClick={() => setSelectedPoint(point)}>
                      查看点位详情
                    </Button>
                  </div>
                </InfoCard>
              ))}
            </div>
          </Card>
        </section>
      </div>

      <DetailDrawer
        open={Boolean(selectedPoint)}
        title={selectedPoint?.locationName ?? "点位详情"}
        onClose={() => setSelectedPoint(null)}
      >
        {selectedPoint ? (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="药材">{selectedPoint.herbName}</Descriptions.Item>
            <Descriptions.Item label="区县">{selectedPoint.district}</Descriptions.Item>
            <Descriptions.Item label="基地">{selectedPoint.baseName ?? "未绑定基地"}</Descriptions.Item>
            <Descriptions.Item label="经纬度">{selectedPoint.longitude}, {selectedPoint.latitude}</Descriptions.Item>
            <Descriptions.Item label="海拔">{selectedPoint.altitude ?? "-"} 米</Descriptions.Item>
            <Descriptions.Item label="分布类型">{selectedPoint.distributionType}</Descriptions.Item>
            <Descriptions.Item label="说明">{selectedPoint.description}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </DetailDrawer>

      <Modal footer={null} open={modalOpen} title="新增点位占位" onCancel={() => setModalOpen(false)}>
        <Typography.Paragraph className={styles.mutedText}>
          点位新增表单将在地图模块与后端接口联调时补充。
        </Typography.Paragraph>
      </Modal>
    </SiteLayout>
  );
}
