import { growthRecords } from "./growth";
import { herbDistributionPoints, recommendedHerbs } from "./herbs";
import { researchProjects } from "./teaching";

export const homeHerbRecommendations = recommendedHerbs.map((herb) => ({
  id: herb.id,
  name: herb.herbName,
  image: herb.imageUrl,
  subtitle: herb.categoryName,
  tags: herb.tags,
}));

export const mapOverviewStats = [
  {
    title: "重庆分布点",
    value: herbDistributionPoints.length,
    unit: "处",
    description: "覆盖南川、巫溪、石柱、彭水、武隆等区县。",
  },
  {
    title: "教学科研基地",
    value: 3,
    unit: "个",
    description: "支撑标本观察、生长采集和课程资料归档。",
  },
  {
    title: "已归档点位",
    value: herbDistributionPoints.filter((point) => point.status === "archived").length,
    unit: "处",
    description: "历史调查点位已进入归档展示。",
  },
];

export const latestResearchNews = researchProjects.map((project) => ({
  id: project.id,
  title: project.projectName,
  leader: project.leader,
  status: project.status,
  summary: project.summary,
}));

export const growthObservationSummary = [
  {
    title: "今日采集记录",
    value: growthRecords.length,
    description: "移动采集与 PC 录入汇总展示。",
  },
  {
    title: "待完善记录",
    value: growthRecords.filter((record) => record.reviewStatus === "pending" || record.reviewStatus === "draft")
      .length,
    description: "需要补充图片、定位或审核信息。",
  },
  {
    title: "已通过记录",
    value: growthRecords.filter((record) => record.reviewStatus === "approved").length,
    description: "可用于课程案例和资源归档。",
  },
];

export const featuredHerbs = [
  {
    id: "renshen",
    name: "人参",
    category: "补益类",
    descriptionLines: ["大补元气", "复脉固脱"],
    origin: "吉林 · 长白山",
    level: "精品",
    imageSrc: "/images/herbs/showcase/renshen.png",
  },
  {
    id: "huangqi",
    name: "黄芪",
    category: "根茎类",
    descriptionLines: ["补气升阳", "固表止汗"],
    origin: "甘肃 · 岷县",
    level: "道地",
    imageSrc: "/images/herbs/showcase/huangqi.png",
  },
  {
    id: "gouqi",
    name: "枸杞",
    category: "果实类",
    descriptionLines: ["滋补肝肾", "益精明目"],
    origin: "宁夏 · 中宁",
    level: "优选",
    imageSrc: "/images/herbs/showcase/gouqi.png",
  },
  {
    id: "danshen",
    name: "丹参",
    category: "活血类",
    descriptionLines: ["活血祛瘀", "通经止痛"],
    origin: "山东 · 临沂",
    level: "优选",
    imageSrc: "/images/herbs/showcase/danshen.png",
  },
];

export const mapOverview = {
  totalSpecies: 128,
  totalPoints: 46,
  totalBases: 12,
};

export const researchNews = [
  {
    id: "news-001",
    type: "研究论文",
    title: "基于代谢组学的黄芪品质评价研究",
    source: "中药学院",
    date: "2024-05-12",
    cover: "",
  },
  {
    id: "news-002",
    type: "科研项目",
    title: "道地药材生态适应性与可持续利用研究",
    source: "国家自然科学基金",
    date: "2024-04-28",
    cover: "",
  },
  {
    id: "news-003",
    type: "教学课程",
    title: "中药鉴定学实验指导（教学标本实训）",
    source: "课程资源",
    date: "2024-04-15",
    cover: "",
  },
];

export const growthObservation = {
  baseName: "甘肃 · 岷县 野外观测站",
  updatedAt: "2024-05-18 10:00",
  temperature: 18.6,
  humidity: 62,
  light: 12500,
  rainfall: 0,
  trend: [
    { date: "5/12", value: 19 },
    { date: "5/13", value: 14 },
    { date: "5/14", value: 18 },
    { date: "5/15", value: 21 },
    { date: "5/16", value: 20 },
    { date: "5/17", value: 27 },
    { date: "5/18", value: 22 },
  ],
};
